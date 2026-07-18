package com.ghmanager.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghmanager.app.data.local.ActionLogEntity
import com.ghmanager.app.data.local.RepoHistoryEntity
import com.ghmanager.app.data.remote.ApiResult
import com.ghmanager.app.data.remote.GithubError
import com.ghmanager.app.data.remote.model.CreateRepoRequest
import com.ghmanager.app.data.remote.model.GithubRepo
import com.ghmanager.app.data.remote.model.RenameRepoRequest
import com.ghmanager.app.data.repository.GithubRepository
import com.ghmanager.app.data.repository.HistoryRepository
import com.ghmanager.app.data.repository.TokenRepository
import com.ghmanager.app.security.SaveLocationStore
import com.ghmanager.app.security.ThemeStore
import com.ghmanager.app.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiMessage(
    val text: String,
    val isError: Boolean,
    val actionLabel: String? = null,
    val action: UiAction? = null
)

enum class UiAction {
    MAKE_PUBLIC_AND_PUBLISH
}

class MainViewModel(
    private val tokenRepo: TokenRepository,
    private val githubRepo: GithubRepository,
    private val historyRepo: HistoryRepository,
    private val saveLocationStore: SaveLocationStore,
    private val themeStore: ThemeStore
) : ViewModel() {

    val tokens = tokenRepo.tokens
    val activeTokenId = tokenRepo.activeTokenId

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _defaultSaveUri = MutableStateFlow<String?>(null)
    val defaultSaveUri: StateFlow<String?> = _defaultSaveUri.asStateFlow()

    private val _needsSaveLocation = MutableStateFlow(false)
    val needsSaveLocation: StateFlow<Boolean> = _needsSaveLocation.asStateFlow()

    private var pendingCloneRepo: GithubRepo? = null
    private var pendingPublishRepo: GithubRepo? = null

    private val _pendingCloneWithUri = MutableStateFlow<Pair<String, GithubRepo>?>(null)
    val pendingCloneWithUri: StateFlow<Pair<String, GithubRepo>?> = _pendingCloneWithUri.asStateFlow()

    fun consumePendingCloneWithUri() {
        _pendingCloneWithUri.value = null
    }

    private val _existingRepos = MutableStateFlow<List<GithubRepo>>(emptyList())
    val existingRepos: StateFlow<List<GithubRepo>> = _existingRepos.asStateFlow()

    private val _historyRepos = MutableStateFlow<List<RepoHistoryEntity>>(emptyList())
    val historyRepos: StateFlow<List<RepoHistoryEntity>> = _historyRepos.asStateFlow()

    private val _actionLogs = MutableStateFlow<List<ActionLogEntity>>(emptyList())
    val actionLogs: StateFlow<List<ActionLogEntity>> = _actionLogs.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message.asStateFlow()

    private val _showSwitchWarning = MutableStateFlow(false)
    val showSwitchWarning: StateFlow<Boolean> = _showSwitchWarning.asStateFlow()

    private var pendingTokenId: String? = null

    init {
        viewModelScope.launch {
            _themeMode.value = themeStore.getMode()
            _defaultSaveUri.value = saveLocationStore.getDefaultUri()
            tokenRepo.refresh()
            applyActiveToken()
            tokenRepo.activeTokenId.collect {
                // reactive: when active token changes, reload
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeStore.setMode(mode)
            _themeMode.value = mode
        }
    }

    fun onSaveLocationResolved(uri: String) {
        viewModelScope.launch {
            saveLocationStore.setDefaultUri(uri)
            _defaultSaveUri.value = uri
            _needsSaveLocation.value = false
            pendingCloneRepo?.let { repo ->
                val toClone = repo
                pendingCloneRepo = null
                requestClone(toClone)
            }
        }
    }

    fun cancelSaveLocation() {
        _needsSaveLocation.value = false
        pendingCloneRepo = null
    }

    fun activeUsername(): String = tokenRepo.getActiveTokenEntity()?.username ?: ""

    fun applyActiveToken() {
        viewModelScope.launch {
            val id = tokenRepo.activeTokenId.value
            val token = if (id != null) tokenRepo.getPlainToken(id) else null
            githubRepo.setToken(token)
            reloadAll()
        }
    }

    fun init() {
        viewModelScope.launch {
            tokenRepo.refresh()
            applyActiveToken()
        }
    }

    private fun loadHistory(tokenId: String?) {
        viewModelScope.launch {
            _historyRepos.value = if (tokenId != null) historyRepo.getHistoryForToken(tokenId) else emptyList()
            _actionLogs.value = if (tokenId != null) historyRepo.getLogsForToken(tokenId) else emptyList()
        }
    }

    fun reloadAll() {
        val tokenId = tokenRepo.activeTokenId.value
        loadHistory(tokenId)
        if (tokenId == null) {
            _existingRepos.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            when (val res = githubRepo.getRepos()) {
                is ApiResult.Success -> _existingRepos.value = res.data
                is ApiResult.Error -> showError(res.error)
            }
            _isBusy.value = false
        }
    }

    // ---- Token management ----
    fun requestSwitchToken(tokenId: String) {
        if (tokenId == tokenRepo.activeTokenId.value) return
        if (_isBusy.value) {
            pendingTokenId = tokenId
            _showSwitchWarning.value = true
            return
        }
        performSwitch(tokenId)
    }

    fun confirmSwitchDespiteBusy() {
        _showSwitchWarning.value = false
        pendingTokenId?.let { performSwitch(it) }
        pendingTokenId = null
    }

    fun cancelSwitch() {
        _showSwitchWarning.value = false
        pendingTokenId = null
    }

    private fun performSwitch(tokenId: String) {
        viewModelScope.launch {
            tokenRepo.setActiveToken(tokenId)
            applyActiveToken()
            showMessage("Switched to token '${tokenRepo.getActiveTokenEntity()?.name ?: tokenId}'", false)
        }
    }

    suspend fun addToken(name: String, token: String): Boolean {
        _isBusy.value = true
        try {
            // Validate the token by fetching the authenticated user.
            githubRepo.setToken(token)
            val userRes = githubRepo.getCurrentUser()
            return when (userRes) {
                is ApiResult.Success -> {
                    val username = userRes.data.login
                    tokenRepo.addToken(name, token, username)
                    showMessage("Token '$name' added for user $username", false)
                    true
                }
                is ApiResult.Error -> {
                    showError(userRes.error)
                    false
                }
            }
        } finally {
            // Restore the previously active token synchronously, then refresh UI.
            val activeId = tokenRepo.activeTokenId.value
            val activeToken = if (activeId != null) tokenRepo.getPlainToken(activeId) else null
            githubRepo.setToken(activeToken)
            reloadAll()
            _isBusy.value = false
        }
    }

    fun removeToken(tokenId: String) {
        viewModelScope.launch {
            val entity = tokenRepo.tokens.value.firstOrNull { it.id == tokenId }
            if (entity != null) {
                tokenRepo.removeToken(entity)
                applyActiveToken()
                showMessage("Token removed", false)
            }
        }
    }

    // ---- Create repo ----
    fun createRepo(name: String, description: String?, isPrivate: Boolean, autoInit: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            when (val res = githubRepo.createRepo(CreateRepoRequest(name, description, isPrivate, autoInit))) {
                is ApiResult.Success -> {
                    val repo = res.data
                    val entity = RepoHistoryEntity(
                        fullName = repo.fullName,
                        name = repo.name,
                        owner = repo.owner?.login ?: "",
                        description = repo.description,
                        isPrivate = repo.isPrivate,
                        cloneUrl = repo.cloneUrl,
                        defaultBranch = repo.defaultBranch,
                        hasPages = repo.hasPages,
                        tokenId = tokenRepo.activeTokenId.value ?: ""
                    )
                    historyRepo.recordCreatedRepo(entity)
                    showMessage("Repository '${repo.fullName}' created", false)
                    reloadReposQuietly()
                }
                is ApiResult.Error -> showError(res.error)
            }
            _isBusy.value = false
        }
    }

    // ---- Repo actions ----
    fun deleteRepo(repo: GithubRepo, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val owner = repo.owner?.login ?: repo.fullName.substringBefore("/")
            when (val res = githubRepo.deleteRepo(owner, repo.name)) {
                is ApiResult.Success -> {
                    historyRepo.removeFromHistory(repo.fullName)
                    historyRepo.logAction(ActionLogEntity(repo.fullName, "DELETE", tokenId = tokenRepo.activeTokenId.value ?: "", success = true))
                    showMessage("Repository '${repo.fullName}' deleted", false)
                    // Refresh the list in the background. A refresh failure here is
                    // non-fatal (the delete already succeeded), so never clobber the
                    // success message with an error banner — just re-fetch optimistically.
                    reloadReposQuietly()
                }
                is ApiResult.Error -> showError(res.error)
            }
            _isBusy.value = false
            onDone()
        }
    }

    /**
     * Re-fetches the repo list without surfacing errors. Used after a successful
     * mutation (delete/visibility/publish) where a transient list-refresh failure
     * must NOT overwrite the success confirmation already shown to the user.
     */
    private fun reloadReposQuietly() {
        viewModelScope.launch {
            when (val res = githubRepo.getRepos()) {
                is ApiResult.Success -> _existingRepos.value = res.data
                is ApiResult.Error -> { /* keep current list; ignore refresh error */ }
            }
        }
    }

    fun renameRepo(repo: GithubRepo, newName: String, newDesc: String?, newPrivate: Boolean?) {
        viewModelScope.launch {
            _isBusy.value = true
            val owner = repo.owner?.login ?: repo.fullName.substringBefore("/")
            when (val res = githubRepo.updateRepo(owner, repo.name, RenameRepoRequest(name = newName, description = newDesc, `private` = newPrivate))) {
                is ApiResult.Success -> {
                    historyRepo.logAction(ActionLogEntity(repo.fullName, "RENAME->$newName", tokenId = tokenRepo.activeTokenId.value ?: "", success = true))
                    showMessage("Repository renamed to '${res.data.fullName}'", false)
                    reloadReposQuietly()
                }
                is ApiResult.Error -> showError(res.error)
            }
            _isBusy.value = false
        }
    }

    fun changeVisibility(repo: GithubRepo, makePrivate: Boolean) {
        viewModelScope.launch {
            _isBusy.value = true
            val owner = repo.owner?.login ?: repo.fullName.substringBefore("/")
            when (val res = githubRepo.updateRepo(owner, repo.name, RenameRepoRequest(name = repo.name, description = repo.description, `private` = makePrivate))) {
                is ApiResult.Success -> {
                    historyRepo.logAction(ActionLogEntity(repo.fullName, "VISIBILITY->${if (makePrivate) "private" else "public"}", tokenId = tokenRepo.activeTokenId.value ?: "", success = true))
                    showMessage("Visibility updated", false)
                    reloadReposQuietly()
                }
                is ApiResult.Error -> showError(res.error)
            }
            _isBusy.value = false
        }
    }

    fun forkRepo(repo: GithubRepo, org: String? = null) {
        viewModelScope.launch {
            _isBusy.value = true
            val owner = repo.owner?.login ?: repo.fullName.substringBefore("/")
            when (val res = githubRepo.forkRepo(owner, repo.name, org)) {
                is ApiResult.Success -> {
                    historyRepo.logAction(ActionLogEntity(repo.fullName, "FORK", tokenId = tokenRepo.activeTokenId.value ?: "", success = true))
                    showMessage("Forked to ${res.data.fullName}", false)
                    reloadReposQuietly()
                }
                is ApiResult.Error -> showError(res.error)
            }
            _isBusy.value = false
        }
    }

    fun transferRepo(repo: GithubRepo, newOwner: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val owner = repo.owner?.login ?: repo.fullName.substringBefore("/")
            when (val res = githubRepo.transferRepo(owner, repo.name, newOwner)) {
                is ApiResult.Success -> {
                    historyRepo.logAction(ActionLogEntity(repo.fullName, "TRANSFER->$newOwner", tokenId = tokenRepo.activeTokenId.value ?: "", success = true))
                    showMessage("Transfer initiated to $newOwner", false)
                    reloadReposQuietly()
                }
                is ApiResult.Error -> showError(res.error)
            }
            _isBusy.value = false
        }
    }

    fun publishRepo(repo: GithubRepo) {
        viewModelScope.launch {
            _isBusy.value = true
            val owner = repo.owner?.login ?: repo.fullName.substringBefore("/")
            val branch = repo.defaultBranch
            if (branch.isBlank()) {
                historyRepo.logAction(ActionLogEntity(repo.fullName, "PUBLISH_PAGES", tokenId = tokenRepo.activeTokenId.value ?: "", success = false, message = "Unknown default branch"))
                showMessage("Cannot publish: default branch is unknown for this repo.", true)
                _isBusy.value = false
                return@launch
            }
            when (val res = githubRepo.enablePages(owner, repo.name, branch)) {
                is ApiResult.Success -> {
                    historyRepo.logAction(ActionLogEntity(repo.fullName, "PUBLISH_PAGES", tokenId = tokenRepo.activeTokenId.value ?: "", success = true))
                    val site = res.data.htmlUrl ?: "https://$owner.github.io/${repo.name}/"
                    showMessage("Pages enabled. Site (may take a minute): $site", false)
                    reloadReposQuietly()
                }
                is ApiResult.Error -> {
                    if (res.error.isPrivatePagesError && repo.isPrivate) {
                        // GitHub Pages needs a public repo on free plans. Suggest
                        // making it public first, with a confirmable action.
                        pendingPublishRepo = repo
                        _message.value = UiMessage(
                            text = "Cannot publish: GitHub Pages requires a PUBLIC repository " +
                                "(your plan doesn't support Pages on private repos). Make this repo " +
                                "public, then publish?",
                            isError = true,
                            actionLabel = "Make public & publish",
                            action = UiAction.MAKE_PUBLIC_AND_PUBLISH
                        )
                    } else {
                        showError(res.error)
                    }
                }
            }
            _isBusy.value = false
        }
    }

    fun refreshRepos() {
        reloadAll()
    }

    /**
     * Called when the user confirms the "Make public & publish" suggestion after a
     * private-repo Pages failure: flips the repo to public, then retries publishing.
     */
    fun confirmMakePublicAndPublish() {
        val repo = pendingPublishRepo ?: return
        pendingPublishRepo = null
        _message.value = null
        viewModelScope.launch {
            _isBusy.value = true
            val owner = repo.owner?.login ?: repo.fullName.substringBefore("/")
            val branch = repo.defaultBranch
            // Step 1: make the repo public.
            when (val vis = githubRepo.updateRepo(owner, repo.name,
                com.ghmanager.app.data.remote.model.RenameRepoRequest(
                    name = repo.name, description = repo.description, `private` = false))) {
                is ApiResult.Error -> {
                    showError(vis.error)
                    _isBusy.value = false
                    return@launch
                }
                is ApiResult.Success -> {
                    historyRepo.logAction(ActionLogEntity(repo.fullName, "VISIBILITY->public", tokenId = tokenRepo.activeTokenId.value ?: "", success = true))
                    // Step 2: publish Pages now that it's public.
                    if (branch.isNotBlank()) {
                        when (val pub = githubRepo.enablePages(owner, repo.name, branch)) {
                            is ApiResult.Success -> {
                                historyRepo.logAction(ActionLogEntity(repo.fullName, "PUBLISH_PAGES", tokenId = tokenRepo.activeTokenId.value ?: "", success = true))
                                val site = pub.data.htmlUrl ?: "https://$owner.github.io/${repo.name}/"
                                showMessage("Made public and published. Site (may take a minute): $site", false)
                            }
                            is ApiResult.Error -> showError(pub.error)
                        }
                    } else {
                        showMessage("Made '${repo.fullName}' public. Cannot publish: default branch unknown.", true)
                    }
                    reloadReposQuietly()
                }
            }
            _isBusy.value = false
        }
    }

    /**
     * Entry point for "Clone to Phone". If a default save location is set,
     * clones immediately. Otherwise it asks the UI to prompt the user for a
     * folder (first run), which is then persisted and the clone continues.
     */
    fun cloneRepo(repo: GithubRepo) {
        val uri = _defaultSaveUri.value
        if (uri == null) {
            pendingCloneRepo = repo
            _needsSaveLocation.value = true
            return
        }
        requestClone(repo)
    }

    /**
     * Downloads the repo archive and extracts it into the SAF tree [treeUri].
     * Returns the target folder display name on success, or null on failure.
     * The extraction works against a DocumentFile tree, so no raw file-path
     * permissions are required.
     */
    suspend fun cloneRepoToUri(context: android.content.Context, repo: GithubRepo, treeUri: String): String? {
        _isBusy.value = true
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val token = tokenRepo.activeTokenId.value?.let { tokenRepo.getPlainToken(it) }
                val zipFile = java.io.File(context.cacheDir, "clone_${System.currentTimeMillis()}.zip")
                try {
                    downloadRepoZipToFile(repo, token, zipFile)
                    extractZipFileIntoTree(context, treeUri, zipFile, repo.name)
                } finally {
                    zipFile.delete()
                }
            }
        }.onSuccess {
            historyRepo.logAction(
                ActionLogEntity(
                    repo.fullName, "CLONE",
                    tokenId = tokenRepo.activeTokenId.value ?: "", success = true
                )
            )
            showMessage("Cloned '${repo.name}' to selected folder", false)
        }.onFailure {
            historyRepo.logAction(
                ActionLogEntity(
                    repo.fullName, "CLONE",
                    tokenId = tokenRepo.activeTokenId.value ?: "", success = false,
                    message = it.message
                )
            )
            showMessage("Clone failed: ${it.message ?: "unknown error"}", true)
        }
        _isBusy.value = false
        return result.getOrNull()
    }

    private fun requestClone(repo: GithubRepo) {
        // Cloning with a known URI is orchestrated by the UI (which holds a
        // Context for SAF extraction). Emit an event the UI observes.
        val uri = _defaultSaveUri.value ?: return
        _pendingCloneWithUri.value = uri to repo
    }

    /**
     * Downloads the repo archive from the GitHub API zipball endpoint and
     * STREAMS it to [destZipFile] (never buffers the whole archive in memory).
     * Uses the bare /zipball endpoint so GitHub resolves the true default
     * branch itself — no hardcoded "main"/"master" guessing. Works for private
     * repos with a token. Validates the file is a real zip before returning.
     */
    private fun downloadRepoZipToFile(repo: GithubRepo, token: String?, destZipFile: java.io.File) {
        val owner = repo.owner?.login ?: repo.fullName.substringBefore("/")
        // Bare /zipball: GitHub resolves the default branch automatically.
        val apiZipUrl = "https://api.github.com/repos/$owner/${repo.name}/zipball"

        val client = okhttp3.OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
            .writeTimeout(5, java.util.concurrent.TimeUnit.MINUTES)
            .build()

        val builder = okhttp3.Request.Builder()
            .url(apiZipUrl)
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("X-GitHub-Api-Version", "2022-11-28")
            // GitHub rejects API calls without a User-Agent.
            .addHeader("User-Agent", "GHManager-Android")
        if (!token.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $token")
        }

        client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw Exception(
                    "Download failed: HTTP ${resp.code}" +
                        when (resp.code) {
                            404 -> " (repo not found or no access)"
                            401, 403 -> " (token invalid or missing scope for this repo)"
                            else -> ""
                        }
                )
            }
            val body = resp.body ?: throw Exception("Empty response body")
            body.byteStream().use { input ->
                destZipFile.outputStream().buffered().use { out ->
                    input.copyTo(out)
                }
            }
        }

        // A valid zip must exist, be non-trivial, and start with the "PK" signature.
        if (!destZipFile.exists() || destZipFile.length() < 4L) {
            throw Exception("Downloaded file is empty or missing")
        }
        destZipFile.inputStream().use { fis ->
            val sig = ByteArray(2)
            fis.read(sig)
            if (sig[0] != 'P'.code.toByte() || sig[1] != 'K'.code.toByte()) {
                throw Exception("Downloaded file is not a valid zip (server returned an error page or a truncated download)")
            }
        }
    }

    /**
     * Extracts the downloaded [zipFile] into the SAF tree [treeUri], under a
     * subfolder named [repoName]. Uses DocumentFile so no raw filesystem
     * permission is needed. Includes zip-slip protection. Returns the created
     * folder's display name.
     */
    private fun extractZipFileIntoTree(
        context: android.content.Context,
        treeUri: String,
        zipFile: java.io.File,
        repoName: String
    ): String {
        val root = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, android.net.Uri.parse(treeUri))
            ?: throw Exception("Invalid save folder")

        // Pick a non-colliding folder name (repoName, repoName_1, repoName_2, ...).
        var targetFolder = if (root.findFile(repoName) == null) {
            root.createDirectory(repoName)
        } else {
            var i = 1
            var made = root.findFile("${repoName}_$i")
            while (made != null) {
                i++
                made = root.findFile("${repoName}_$i")
            }
            root.createDirectory("${repoName}_$i")
        } ?: throw Exception("Could not create folder")

        java.util.zip.ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                // Strip the top-level archive folder (e.g. repo-<sha>/).
                val rawName = entry.name
                val slash = rawName.indexOf('/')
                val path = if (slash >= 0) rawName.substring(slash + 1) else ""
                if (path.isBlank()) { zis.closeEntry(); entry = zis.nextEntry; continue }

                // Zip-slip guard: reject any entry that tries to escape the target.
                val normalized = path.replace('\\', '/')
                if (normalized.split("/").any { it == ".." }) {
                    throw Exception("Unsafe zip entry rejected: ${entry.name}")
                }

                if (entry.isDirectory) {
                    var cursor = targetFolder
                    for (p in normalized.trimEnd('/').split("/")) {
                        if (p.isBlank()) continue
                        cursor = cursor.findFile(p) ?: cursor.createDirectory(p)!!
                    }
                } else {
                    val parts = normalized.split("/")
                    var parent = targetFolder
                    for (i in 0 until parts.size - 1) {
                        if (parts[i].isBlank()) continue
                        parent = parent.findFile(parts[i]) ?: parent.createDirectory(parts[i])!!
                    }
                    val fileName = parts.last()
                    // Overwrite: delete any stale file so we never append/corrupt.
                    parent.findFile(fileName)?.delete()
                    val file = parent.createFile("application/octet-stream", fileName)
                    file?.uri?.let { uri ->
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            zis.copyTo(os)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return targetFolder.name ?: repoName
    }

    // ---- Messaging ----
    private fun showError(error: GithubError) {
        val scopeNote = if (error.isScopeError) {
            " ${if (error.requiredScope != null) "(requires '${error.requiredScope}' scope)" else "(token scope/permission issue)"}"
        } else ""
        _message.value = UiMessage("Error: ${error.message}$scopeNote", true)
    }

    fun showMessage(text: String, isError: Boolean) {
        _message.value = UiMessage(text, isError)
    }

    fun clearMessage() {
        _message.value = null
    }
}
