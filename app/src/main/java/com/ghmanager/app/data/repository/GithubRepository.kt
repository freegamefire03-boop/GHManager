package com.ghmanager.app.data.repository

import com.ghmanager.app.data.remote.ApiResult
import com.ghmanager.app.data.remote.GithubApiService
import com.ghmanager.app.data.remote.GithubErrorParser
import com.ghmanager.app.data.remote.model.CreatePagesRequest
import com.ghmanager.app.data.remote.model.CreateRepoRequest
import com.ghmanager.app.data.remote.model.GithubRepo
import com.ghmanager.app.data.remote.model.GithubUser
import com.ghmanager.app.data.remote.model.PagesResponse
import com.ghmanager.app.data.remote.model.PagesSource
import com.ghmanager.app.data.remote.model.RenameRepoRequest
import com.ghmanager.app.data.remote.model.TransferRepoRequest
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class GithubRepository {

    private var apiService: GithubApiService? = null
    private var currentToken: String? = null

    fun setToken(token: String?) {
        if (token == currentToken && apiService != null) return
        currentToken = token
        apiService = if (token == null) {
            null
        } else {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Accept", "application/vnd.github+json")
                        .addHeader("X-GitHub-Api-Version", "2022-11-28")
                        .build()
                    chain.proceed(req)
                }
                .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().apply {
                    level = okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            Retrofit.Builder()
                .baseUrl("https://api.github.com/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GithubApiService::class.java)
        }
    }

    private suspend fun <T> serviceCall(block: suspend GithubApiService.() -> retrofit2.Response<T>): ApiResult<T> {
        val svc = apiService ?: return ApiResult.Error(
            com.ghmanager.app.data.remote.GithubError(
                httpCode = 0, message = "No active token. Please add and select a token."
            )
        )
        return try {
            val resp = block(svc)
            if (resp.isSuccessful) {
                val body = resp.body()
                if (body != null) ApiResult.Success(body)
                else ApiResult.Error(GithubErrorParser.parse(resp))
            } else {
                ApiResult.Error(GithubErrorParser.parse(resp))
            }
        } catch (e: Exception) {
            ApiResult.Error(
                com.ghmanager.app.data.remote.GithubError(
                    httpCode = 0, message = "Network error: ${e.message ?: e.javaClass.simpleName}"
                )
            )
        }
    }

    suspend fun getCurrentUser(): ApiResult<GithubUser> = serviceCall { getCurrentUser() }

    suspend fun getRepos(): ApiResult<List<GithubRepo>> {
        val all = mutableListOf<GithubRepo>()
        var page = 1
        while (true) {
            val pageResult = serviceCall { getCurrentUserReposPage(page) }
            when (pageResult) {
                is ApiResult.Success -> {
                    val batch = pageResult.data
                    all.addAll(batch)
                    if (batch.size < 100) break
                    page++
                }
                is ApiResult.Error -> return pageResult
            }
        }
        return ApiResult.Success(all)
    }
    suspend fun createRepo(req: CreateRepoRequest): ApiResult<GithubRepo> = serviceCall { createRepo(req) }
    suspend fun deleteRepo(owner: String, repo: String): ApiResult<Unit> = serviceCall { deleteRepo(owner, repo) }
    suspend fun updateRepo(owner: String, repo: String, body: RenameRepoRequest): ApiResult<GithubRepo> =
        serviceCall { updateRepo(owner, repo, body) }
    suspend fun forkRepo(owner: String, repo: String, org: String? = null): ApiResult<GithubRepo> =
        serviceCall { forkRepo(owner, repo, org) }
    suspend fun transferRepo(owner: String, repo: String, newOwner: String): ApiResult<GithubRepo> =
        serviceCall { transferRepo(owner, repo, TransferRepoRequest(owner = newOwner)) }

    suspend fun enablePages(owner: String, repo: String, branch: String): ApiResult<PagesResponse> =
        serviceCall { enablePages(owner, repo, CreatePagesRequest(PagesSource(branch = branch, path = "/"))) }
}
