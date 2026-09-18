package com.example.networkauth

import android.content.Context
import com.example.networkauth.auth.AuthRepository
import com.example.networkauth.core.network.ApiClient
import com.example.networkauth.core.network.TokenManager
import com.example.networkauth.items.ItemsRepository

class AppContainer(context: Context) {
    val tokenManager = TokenManager(context)
    val apiClient = ApiClient(tokenManager)
    val authRepository = AuthRepository(apiClient.apiService, tokenManager)
    val itemsRepository = ItemsRepository(apiClient.apiService)
}
