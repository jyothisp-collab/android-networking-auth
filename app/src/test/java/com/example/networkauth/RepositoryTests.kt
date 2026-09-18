package com.example.networkauth

import com.example.networkauth.auth.AuthRepository
import com.example.networkauth.auth.LoginRequest
import com.example.networkauth.auth.TokenResponse
import com.example.networkauth.core.network.ApiService
import com.example.networkauth.core.network.NetworkError
import com.example.networkauth.core.network.TokenManager
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RepositoryTests {

    private val apiService = mockk<ApiService>()
    private val tokenManager = mockk<TokenManager>(relaxed = true)
    private val authRepository = AuthRepository(apiService, tokenManager)

    @Test
    fun `login success saves tokens`() = runBlocking {
        val request = LoginRequest("user", "pass")
        val response = TokenResponse("access123", "refresh123")
        coEvery { apiService.login(request) } returns response

        val result = authRepository.login(request)

        assertTrue(result.isSuccess)
        verify { tokenManager.saveTokens("access123", "refresh123") }
    }

    @Test
    fun `login handles 401 as Unauthorized NetworkError`() = runBlocking {
        val request = LoginRequest("user", "pass")
        val responseBody = "{\"error\":\"unauthorized\"}".toResponseBody("application/json".toMediaTypeOrNull())
        val httpException = HttpException(Response.error<Any>(401, responseBody))
        
        coEvery { apiService.login(request) } throws httpException

        val result = authRepository.login(request)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkError.Unauthorized)
    }

    @Test
    fun `login handles 500 as ServerError`() = runBlocking {
        val request = LoginRequest("user", "pass")
        val responseBody = "{\"error\":\"server error\"}".toResponseBody("application/json".toMediaTypeOrNull())
        val httpException = HttpException(Response.error<Any>(500, responseBody))
        
        coEvery { apiService.login(request) } throws httpException

        val result = authRepository.login(request)

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as NetworkError.ServerError
        assertTrue(exception.code == 500)
    }
}
