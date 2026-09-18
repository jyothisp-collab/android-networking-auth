package com.example.networkauth

import android.content.Context
import android.content.SharedPreferences
import com.example.networkauth.core.network.AuthInterceptor
import com.example.networkauth.core.network.TokenAuthenticator
import com.example.networkauth.core.network.TokenManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class NetworkTests {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var tokenManager: TokenManager
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var sharedPrefsEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        sharedPrefsEditor = mockk(relaxed = true)
        sharedPrefs = mockk()
        every { sharedPrefs.edit() } returns sharedPrefsEditor
        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs

        val prefsMap = mutableMapOf<String, String?>()
        every { sharedPrefs.getString(any(), any()) } answers {
            prefsMap[firstArg<String>()] ?: secondArg<String?>()
        }
        every { sharedPrefsEditor.putString(any(), any()) } answers {
            prefsMap[firstArg<String>()] = secondArg<String>()
            sharedPrefsEditor
        }
        every { sharedPrefsEditor.clear() } answers {
            prefsMap.clear()
            sharedPrefsEditor
        }

        tokenManager = TokenManager(context)

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofitFactory = {
            Retrofit.Builder()
                .baseUrl(mockWebServer.url("/"))
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
        }

        okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .authenticator(TokenAuthenticator(tokenManager, retrofitFactory))
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `successful API request attaches token`() {
        tokenManager.saveTokens("valid_access", "valid_refresh")
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        
        val request = Request.Builder().url(mockWebServer.url("/items")).build()
        val response = okHttpClient.newCall(request).execute()
        
        assertTrue(response.isSuccessful)
        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("Bearer valid_access", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `401 triggers refresh and retry`() {
        tokenManager.saveTokens("expired_access", "valid_refresh")
        
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"access_token": "new_access", "refresh_token": "new_refresh"}
        """.trimIndent()))
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        
        val request = Request.Builder().url(mockWebServer.url("/items")).build()
        val response = okHttpClient.newCall(request).execute()
        
        assertTrue(response.isSuccessful)
        
        val req1 = mockWebServer.takeRequest()
        assertEquals("Bearer expired_access", req1.getHeader("Authorization"))
        
        val req2 = mockWebServer.takeRequest()
        assertTrue(req2.path!!.endsWith("/refresh"))
        
        val req3 = mockWebServer.takeRequest()
        assertEquals("Bearer new_access", req3.getHeader("Authorization"))
        
        assertEquals("new_access", tokenManager.getAccessToken())
    }

    @Test
    fun `refresh failure clears tokens`() {
        tokenManager.saveTokens("expired_access", "expired_refresh")
        
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        
        val request = Request.Builder().url(mockWebServer.url("/items")).build()
        val response = okHttpClient.newCall(request).execute()
        
        assertFalse(response.isSuccessful)
        assertEquals(401, response.code)
        
        assertNull(tokenManager.getAccessToken())
        assertNull(tokenManager.getRefreshToken())
    }

    @Test
    fun `retry does not loop indefinitely`() {
        tokenManager.saveTokens("expired_access", "valid_refresh")
        
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"access_token": "new_access", "refresh_token": "new_refresh"}
        """.trimIndent()))
        
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"access_token": "newer_access", "refresh_token": "newer_refresh"}
        """.trimIndent()))
        
        mockWebServer.enqueue(MockResponse().setResponseCode(401))
        
        val request = Request.Builder().url(mockWebServer.url("/items")).build()
        val response = okHttpClient.newCall(request).execute()
        
        assertFalse(response.isSuccessful)
        assertEquals(401, response.code)
    }
}
