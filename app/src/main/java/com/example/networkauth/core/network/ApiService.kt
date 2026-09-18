package com.example.networkauth.core.network

import com.example.networkauth.auth.LoginRequest
import com.example.networkauth.auth.RefreshRequest
import com.example.networkauth.auth.TokenResponse
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class Item(val id: Int, val name: String)

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("refresh")
    fun refreshSync(@Body request: RefreshRequest): retrofit2.Call<TokenResponse>

    @GET("items")
    suspend fun getItems(): List<Item>
}
