package com.example.networkauth.auth

import com.example.networkauth.core.network.ApiService
import com.example.networkauth.core.network.TokenManager
import com.example.networkauth.core.network.safeApiCall

class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    suspend fun login(request: LoginRequest): Result<Unit> {
        return safeApiCall { apiService.login(request) }.map { tokenResponse ->
            tokenManager.saveTokens(tokenResponse.accessToken, tokenResponse.refreshToken)
        }
    }

    fun logout() {
        tokenManager.clearTokens()
    }
}
