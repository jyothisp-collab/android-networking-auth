package com.example.networkauth.core.network

sealed class NetworkError : Exception() {
    class Unauthorized(message: String = "Unauthorized") : NetworkError()
    class NoInternet(message: String = "No Internet Connection") : NetworkError()
    class Timeout(message: String = "Request Timed Out") : NetworkError()
    class ServerError(val code: Int, message: String = "Server Error") : NetworkError()
    class Unknown(message: String = "Unknown Error", cause: Throwable? = null) : NetworkError()
}

suspend inline fun <T> safeApiCall(crossinline apiCall: suspend () -> T): Result<T> {
    return try {
        Result.success(apiCall())
    } catch (e: Exception) {
        val error = when (e) {
            is retrofit2.HttpException -> {
                when (e.code()) {
                    401 -> NetworkError.Unauthorized()
                    in 500..599 -> NetworkError.ServerError(e.code(), e.message())
                    else -> NetworkError.Unknown("HTTP ${e.code()}", e)
                }
            }
            is java.net.SocketTimeoutException -> NetworkError.Timeout()
            is java.io.IOException -> NetworkError.NoInternet()
            else -> NetworkError.Unknown(cause = e)
        }
        Result.failure(error)
    }
}
