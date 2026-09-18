package com.example.networkauth.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.networkauth.auth.AuthRepository
import com.example.networkauth.auth.LoginRequest
import com.example.networkauth.core.network.Item
import com.example.networkauth.core.network.NetworkError
import com.example.networkauth.core.network.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ItemsViewModel(
    private val itemsRepository: ItemsRepository,
    private val authRepository: AuthRepository,
    tokenManager: TokenManager
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = tokenManager.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun login(username: String, password: String = "demo_password") {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            authRepository.login(LoginRequest(username, password)).onFailure { err ->
                _error.value = formatError(err)
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        authRepository.logout()
        _items.value = emptyList()
    }

    fun fetchItems() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            itemsRepository.getItems().fold(
                onSuccess = { items ->
                    _items.value = items
                },
                onFailure = { err ->
                    _error.value = formatError(err)
                }
            )
            _isLoading.value = false
        }
    }
    
    fun clearError() {
        _error.value = null
    }

    private fun formatError(error: Throwable): String {
        return when (error) {
            is NetworkError.Unauthorized -> "Session expired. Please log in again."
            is NetworkError.NoInternet -> "No internet connection."
            is NetworkError.Timeout -> "Request timed out."
            is NetworkError.ServerError -> "Server error (${error.code})."
            else -> error.message ?: "An unknown error occurred."
        }
    }
}
