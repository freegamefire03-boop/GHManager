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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiMessage(val text: String, val isError: Boolean)

class MainViewModel(
    private val tokenRepo: TokenRepository,
    private val githubRepo: GithubRepository,
    private val historyRepo: HistoryRepository
) : ViewModel() {

    val tokens = tokenRepo.tokens
    val activeTokenId = tokenRepo.activeTokenId

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
            tokenRepo.refresh()
            applyActiveToken()
            tokenRepo.activeTokenId.collect {
                // reactive: when active token changes, reload
            }
        }
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
        val userRes = try {
            // validate token by fetching user
            githubRepo.setToken(token)
            githubRepo.getCurrentUser()
        } finally {
            applyActiveToken() // restore active
        }
        return when (userRes) {
            is ApiResult.Success -> {
                val username = userRes.data.login
                tokenRepo.addToken(name, token, username)
                showMessage("Token '$name' added for user $username", false)
                _isBusy.value = false
                true
            }
            is ApiResult.Error -> {
                showError(userRes.error)
                _isBusy.value = false
                false
            }
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

    fun cloneRepo(repo: GithubRepo) {
        viewModelScope.launch {
            _isBusy.value = true
            val ok = downloadRepoZip(repo.cloneUrl, repo.name)
            if (ok) {
                historyRepo.logAction(ActionLogEntity(repo.fullName, "CLONE", tokenId = tokenRepo.activeTokenId.value ?: "", success = true))
                showMessage("Cloned '${repo.name}' to Downloads/GHManager", false)
            } else {
                showMessage("Clone failed: could not write to storage", true)
            }
            _isBusy.value = false
        }
    }

    private suspend fun downloadRepoZip(cloneUrl: String, repoName: String): Boolean {
        return try {
            val zipUrl = cloneUrl.removeSuffix(".git") + "/archive/refs/heads/main.zip"
            val client = okhttp3.OkHttpClient()
            val req = okhttp3.Request.Builder().url(zipUrl).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return false
            val dir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val target = java.io.File(dir, "GHManager")
            if (!target.exists()) target.mkdirs()
            val out = java.io.File(target, "$repoName.zip")
            resp.body?.byteStream()?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            true
        } catch (e: Exception) {
            false
        }
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
