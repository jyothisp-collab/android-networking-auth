package com.example.networkauth.core.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject

class MockBackendInterceptor : Interceptor {
    private var validAccessToken = "access_demo_1"
    private val validRefreshToken = "refresh_demo_1"
    
    // Configurable behaviors for testing/demoing
    var shouldFailRefresh = false
    var shouldFailNextRequestWith500 = false

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.encodedPath
        
        // Simulate network delay
        Thread.sleep(500)

        return when {
            url.endsWith("/login") -> handleLogin()
            url.endsWith("/refresh") -> handleRefresh(request)
            url.endsWith("/items") -> handleItems(request)
            else -> createErrorResponse(request, 404, "Not Found")
        }
    }

    private fun handleLogin(): Response {
        val json = JSONObject()
            .put("access_token", validAccessToken)
            .put("refresh_token", validRefreshToken)
            .toString()
            
        return createSuccessResponse(json)
    }

    private fun handleRefresh(request: okhttp3.Request): Response {
        if (shouldFailRefresh) {
            return createErrorResponse(request, 401, "Refresh token expired or invalid")
        }
        
        // In a real mock, we would parse the body to check the refresh token.
        // For demo, we just issue a new access token.
        validAccessToken = "access_demo_2"
        val json = JSONObject()
            .put("access_token", validAccessToken)
            .put("refresh_token", validRefreshToken)
            .toString()
            
        return createSuccessResponse(json, request = request)
    }

    private fun handleItems(request: okhttp3.Request): Response {
        if (shouldFailNextRequestWith500) {
            shouldFailNextRequestWith500 = false
            return createErrorResponse(request, 500, "Internal Server Error")
        }

        val authHeader = request.header("Authorization")
        if (authHeader != "Bearer $validAccessToken") {
            return createErrorResponse(request, 401, "Unauthorized")
        }

        val items = JSONArray()
            .put(JSONObject().put("id", 1).put("name", "Item 1"))
            .put(JSONObject().put("id", 2).put("name", "Item 2"))
            .put(JSONObject().put("id", 3).put("name", "Item 3"))

        return createSuccessResponse(items.toString(), request = request)
    }

    private fun createSuccessResponse(body: String, request: okhttp3.Request? = null): Response {
        return Response.Builder()
            .code(200)
            .message("OK")
            .request(request ?: okhttp3.Request.Builder().url("http://localhost/").build())
            .protocol(Protocol.HTTP_1_1)
            .body(body.toResponseBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()
    }

    private fun createErrorResponse(request: okhttp3.Request, code: Int, message: String): Response {
        return Response.Builder()
            .code(code)
            .message(message)
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .body("{\"error\": \"$message\"}".toResponseBody("application/json".toMediaType()))
            .build()
    }
}
