package com.ghmanager.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class GithubRepo(
    val id: Long = 0,
    val name: String = "",
    val description: String? = null,
    @SerializedName("private") val isPrivate: Boolean = false,
    val fork: Boolean = false,
    @SerializedName("has_pages") val hasPages: Boolean = false,
    @SerializedName("homepage") val homepage: String? = null,
    @SerializedName("html_url") val htmlUrl: String = "",
    @SerializedName("clone_url") val cloneUrl: String = "",
    @SerializedName("ssh_url") val sshUrl: String = "",
    @SerializedName("default_branch") val defaultBranch: String = "main",
    @SerializedName("full_name") val fullName: String = "",
    val owner: Owner? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

data class Owner(
    @SerializedName("login") val login: String = "",
    @SerializedName("avatar_url") val avatarUrl: String = ""
)

data class CreateRepoRequest(
    val name: String,
    val description: String? = null,
    val `private`: Boolean = false,
    @SerializedName("auto_init") val autoInit: Boolean = false
)

data class RenameRepoRequest(
    val name: String,
    val description: String? = null,
    @SerializedName("homepage") val homepage: String? = null,
    @SerializedName("private") val `private`: Boolean? = null
)

data class TransferRepoRequest(
    val owner: String,
    val teamIds: List<Long> = emptyList()
)

data class PagesSource(
    val branch: String,
    val path: String = "/"
)

data class CreatePagesRequest(
    val source: PagesSource
)

data class PagesResponse(
    @SerializedName("html_url") val htmlUrl: String? = null,
    val status: String? = null,
    val url: String? = null
)

data class GithubUser(
    @SerializedName("login") val login: String = "",
    val id: Long = 0,
    @SerializedName("name") val name: String? = null
)

data class ApiErrorResponse(
    val message: String? = null,
    val errors: List<ApiErrorDetail>? = null,
    val documentationUrl: String? = null
)

data class ApiErrorDetail(
    val resource: String? = null,
    val field: String? = null,
    val code: String? = null,
    val message: String? = null
)
