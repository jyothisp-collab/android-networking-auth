package com.example.networkauth.core.network

import com.example.networkauth.auth.RefreshRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val lazyRetrofit: () -> Retrofit
) : Authenticator {

    private val mutex = Mutex()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Prevent infinite loop if the refresh token itself is unauthorized
        if (response.request.url.encodedPath.endsWith("/refresh")) {
            tokenManager.clearTokens()
            return null
        }

        if (response.retryCount() > 2) {
            return null
        }

        return runBlocking {
            mutex.withLock {
                val currentToken = tokenManager.getAccessToken()
                val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")

                if (currentToken != null && currentToken != requestToken) {
                    return@runBlocking response.request.newBuilder()
                        .header("Authorization", "Bearer $currentToken")
                        .build()
                }

                val refreshToken = tokenManager.getRefreshToken()
                if (refreshToken == null) {
                    tokenManager.clearTokens()
                    return@runBlocking null
                }

                val apiService = lazyRetrofit().create(ApiService::class.java)
                val refreshResponse = try {
                    apiService.refreshSync(RefreshRequest(refreshToken)).execute()
                } catch (e: Exception) {
                    null
                }

                if (refreshResponse != null && refreshResponse.isSuccessful) {
                    val body = refreshResponse.body()
                    if (body != null) {
                        tokenManager.saveTokens(body.accessToken, body.refreshToken)
                        return@runBlocking response.request.newBuilder()
                            .header("Authorization", "Bearer ${body.accessToken}")
                            .build()
                    }
                }

                tokenManager.clearTokens()
                null
            }
        }
    }

    private fun Response.retryCount(): Int {
        var currentResponse = priorResponse
        var result = 0
        while (currentResponse != null) {
            result++
            currentResponse = currentResponse.priorResponse
        }
        return result
    }
}
