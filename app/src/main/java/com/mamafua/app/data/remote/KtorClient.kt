package com.mamafua.app.data.remote

import com.mamafua.app.data.local.TokenManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createHttpClient(tokenManager: TokenManager): HttpClient {
    return HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            level = LogLevel.ALL
        }
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.mamafua.com" // Placeholder for actual backend URL
            }
            tokenManager.getToken()?.let { token ->
                header("Authorization", "Bearer $token")
            }
        }
    }
}
