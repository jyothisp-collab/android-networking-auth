package com.example.networkauth.items

import com.example.networkauth.core.network.ApiService
import com.example.networkauth.core.network.Item
import com.example.networkauth.core.network.safeApiCall

class ItemsRepository(private val apiService: ApiService) {
    suspend fun getItems(): Result<List<Item>> {
        return safeApiCall { apiService.getItems() }
    }
}
