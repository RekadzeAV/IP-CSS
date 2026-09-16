package com.company.ipcamera.android.data

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClient {
    private var baseUrl: String = "http://10.0.2.2:8080"
    private var authToken: String? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    fun configure(serverUrl: String, token: String? = null) {
        baseUrl = serverUrl.trimEnd('/')
        authToken = token
    }

    suspend fun get(path: String): String {
        val response = client.get("$baseUrl$path") {
            authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
        return response.bodyAsText()
    }

    suspend fun post(path: String, body: String = ""): String {
        val response = client.post("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            setBody(body)
        }
        return response.bodyAsText()
    }

    suspend fun delete(path: String): String {
        val response = client.delete("$baseUrl$path") {
            authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }
        return response.bodyAsText()
    }

    fun close() {
        client.close()
    }
}
