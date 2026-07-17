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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiMessage(val text: String, val isError: Boolean)

class MainViewModel(
    private val tokenRepo: TokenRepository,
    private val githubRepo: GithubRepository,
    private val historyRepo: HistoryRepository,
    private val saveLocationStore: SaveLocationStore
) : ViewModel() {

    val tokens = tokenRepo.tokens
    val activeTokenId = tokenRepo.activeTokenId

    private val _defaultSaveUri = MutableStateFlow<String?>(null)
    val defaultSaveUri: StateFlow<String?> = _defaultSaveUri.asStateFlow()

    private val _needsSaveLocation = MutableStateFlow(false)
    val needsSaveLocation: StateFlow<Boolean> = _needsSaveLocation.asStateFlow()

    private var pendingCloneRepo: GithubRepo? = null

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
            _defaultSaveUri.value = saveLocationStore.getDefaultUri()
            tokenRepo.refresh()
            applyActiveToken()
            tokenRepo.activeTokenId.collect {
                // reactive: when active token changes, reload
            }
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
                        tokenId = tokenRepo.activeTokenId.value ?: ""
                    )
                    historyRepo.recordCreatedRepo(entity)
                    showMessage("Repository '${repo.fullName}' created", false)
                    reloadAll()
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
                    reloadAll()
                }
                is ApiResult.Error -> showError(res.error)
            }
            _isBusy.value = false
            onDone()
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
                    reloadAll()
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
                    reloadAll()
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
                    reloadAll()
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
                    reloadAll()
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
            val branch = repo.defaultBranch.ifBlank { "main" }
            when (val res = githubRepo.enablePages(owner, repo.name, branch)) {
                is ApiResult.Success -> {
                    historyRepo.logAction(ActionLogEntity(repo.fullName, "PUBLISH_PAGES", tokenId = tokenRepo.activeTokenId.value ?: "", success = true))
                    val site = res.data.htmlUrl ?: "https://$owner.github.io/${repo.name}/"
                    showMessage("Pages enabled. Site (may take a minute): $site", false)
                    reloadAll()
                }
                is ApiResult.Error -> showError(res.error)
            }
            _isBusy.value = false
        }
    }

    fun refreshRepos() {
        reloadAll()
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
            val zipBytes = downloadRepoZipBytes(repo.cloneUrl)
            extractZipIntoTree(context, treeUri, zipBytes, repo.name)
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

    private suspend fun downloadRepoZipBytes(cloneUrl: String): ByteArray {
        val zipUrl = cloneUrl.removeSuffix(".git") + "/archive/refs/heads/main.zip"
        val client = okhttp3.OkHttpClient()
        val req = okhttp3.Request.Builder().url(zipUrl).build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
        return resp.body?.bytes()
            ?: throw Exception("Empty response body")
    }

    /**
     * Extracts the downloaded zip [bytes] into the SAF tree [treeUri], under a
     * subfolder named [repoName]. Uses DocumentFile so no raw filesystem
     * permission is needed. Returns the created folder's display name.
     */
    private fun extractZipIntoTree(
        context: android.content.Context,
        treeUri: String,
        bytes: ByteArray,
        repoName: String
    ): String {
        val root = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, android.net.Uri.parse(treeUri))
            ?: throw Exception("Invalid save folder")
        val folderName = repoName
        var target = root.findFile(folderName)
        if (target != null && target.isDirectory) {
            // Avoid collisions by suffixing
            var i = 1
            while (target != null && target.isDirectory) {
                target = root.findFile("${folderName}_$i")
                if (target == null) target = root.createDirectory("${folderName}_$i")
                i++
            }
        } else {
            target = root.createDirectory(folderName)
        }
        val targetFolder = target ?: throw Exception("Could not create folder")

        java.util.zip.ZipInputStream(bytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                // Strip the top-level archive folder (e.g. repo-main/)
                val path = entry.name.substringAfter("/")
                if (path.isBlank()) { entry = zis.nextEntry; continue }
                if (entry.isDirectory) {
                    var cursor = targetFolder
                    val parts = path.trimEnd('/').split("/")
                    for (p in parts) {
                        cursor = cursor.findFile(p) ?: cursor.createDirectory(p)!!
                    }
                } else {
                    val parts = path.split("/")
                    var parent = targetFolder
                    for (i in 0 until parts.size - 1) {
                        parent = parent.findFile(parts[i]) ?: parent.createDirectory(parts[i])!!
                    }
                    val fileName = parts.last()
                    val existing = parent.findFile(fileName)
                    val file = existing ?: parent.createFile("application/octet-stream", fileName)
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
        return targetFolder.name ?: folderName
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
