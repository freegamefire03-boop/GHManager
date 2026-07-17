package com.ghmanager.app.data.remote

import com.ghmanager.app.data.remote.model.CreatePagesRequest
import com.ghmanager.app.data.remote.model.CreateRepoRequest
import com.ghmanager.app.data.remote.model.GithubRepo
import com.ghmanager.app.data.remote.model.GithubUser
import com.ghmanager.app.data.remote.model.PagesResponse
import com.ghmanager.app.data.remote.model.RenameRepoRequest
import com.ghmanager.app.data.remote.model.TransferRepoRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GithubApiService {

    @GET("user")
    suspend fun getCurrentUser(): Response<GithubUser>

    @GET("user/repos?per_page=100&sort=updated")
    suspend fun getCurrentUserRepos(): Response<List<GithubRepo>>

    @GET("users/{username}/repos?per_page=100&sort=updated")
    suspend fun getUserRepos(@Path("username") username: String): Response<List<GithubRepo>>

    @POST("user/repos")
    @Headers("Accept: application/vnd.github+json")
    suspend fun createRepo(@Body body: CreateRepoRequest): Response<GithubRepo>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GithubRepo>

    @DELETE("repos/{owner}/{repo}")
    suspend fun deleteRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    @PATCH("repos/{owner}/{repo}")
    suspend fun updateRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: RenameRepoRequest
    ): Response<GithubRepo>

    @POST("repos/{owner}/{repo}/forks")
    suspend fun forkRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("organization") organization: String? = null
    ): Response<GithubRepo>

    @POST("repos/{owner}/{repo}/transfer")
    suspend fun transferRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: TransferRepoRequest
    ): Response<GithubRepo>

    @POST("repos/{owner}/{repo}/pages")
    @Headers("Accept: application/vnd.github+json")
    suspend fun enablePages(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreatePagesRequest
    ): Response<PagesResponse>
}
