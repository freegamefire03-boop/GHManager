package com.ghmanager.app.data.remote

import com.google.gson.Gson
import retrofit2.Response

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val error: GithubError) : ApiResult<Nothing>()
}

data class GithubError(
    val httpCode: Int,
    val message: String,
    val isScopeError: Boolean = false,
    val requiredScope: String? = null
)

object GithubErrorParser {

    private val gson = Gson()

    fun <T> parse(response: Response<T>): GithubError {
        val code = response.code()
        val bodyString = try {
            response.errorBody()?.string().orEmpty()
        } catch (e: Exception) {
            ""
        }

        var rawMessage: String? = null
        var scope: String? = null

        if (bodyString.isNotBlank()) {
            try {
                val err = gson.fromJson(bodyString, com.ghmanager.app.data.remote.model.ApiErrorResponse::class.java)
                rawMessage = err?.message
                val detail = err?.errors?.firstOrNull()
                if (detail?.message != null) {
                    rawMessage = (rawMessage?.let { "$it — " } ?: "") + detail.message
                }
            } catch (_: Exception) {
                rawMessage = bodyString
            }
        }

        // Detect scope / permission errors
        val text = "$rawMessage $bodyString".lowercase()
        val scopeError = when {
            code == 401 -> true
            code == 403 && (text.contains("scope") || text.contains("permission") || text.contains("forbidden")) -> true
            code == 422 && (text.contains("scope") || text.contains("permission") || text.contains("delete_repo") || text.contains("requires")) -> true
            text.contains("resource not accessible") -> true
            else -> false
        }

        if (scopeError) {
            scope = inferRequiredScope(text)
        }

        val message = when {
            rawMessage != null -> rawMessage
            code == 401 -> "Authentication failed. The token may be invalid or revoked."
            code == 403 -> "Access forbidden. Your token may lack the required permission."
            code == 404 -> "Resource not found (or your token lacks access)."
            code == 422 -> "Validation failed. The request was rejected by GitHub."
            else -> "Request failed with HTTP $code."
        }

        return GithubError(
            httpCode = code,
            message = message,
            isScopeError = scopeError,
            requiredScope = scope
        )
    }

    private fun inferRequiredScope(text: String): String? {
        return when {
            text.contains("delete_repo") -> "delete_repo"
            text.contains("repo") -> "repo"
            text.contains("admin") -> "admin:org"
            text.contains("workflow") -> "workflow"
            else -> null
        }
    }
}
