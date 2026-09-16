package com.company.ipcamera.android.media

import android.util.Log
import com.company.ipcamera.core.network.ApiClient
import io.ktor.http.Cookie as KtorCookie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * Прокидывает куки из Ktor [ApiClient] (httpOnly JWT и др.) в OkHttp для ExoPlayer HLS.
 */
class ApiClientCookieJar(
    private val apiClient: ApiClient,
) : CookieJar {

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val fullUrl = url.toString()
        return runBlocking(Dispatchers.IO) {
            try {
                apiClient.cookiesForHttpUrl(fullUrl).mapNotNull { it.toOkHttp3(url) }
            } catch (e: Exception) {
                Log.w(TAG, "loadForRequest: $fullUrl", e)
                emptyList()
            }
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        // Обновление сессии — через REST; сегменты HLS обычно не меняют auth cookies.
    }

    companion object {
        private const val TAG = "ApiClientCookieJar"
    }
}

private fun KtorCookie.toOkHttp3(requestUrl: HttpUrl): Cookie? {
    if (name.isBlank()) return null
    val d = domain?.takeIf { it.isNotEmpty() } ?: requestUrl.host
    val p = path?.takeIf { it.isNotEmpty() } ?: "/"
    val b = Cookie.Builder()
        .name(name)
        .value(value)
        .domain(d)
        .path(p)
    if (secure) b.secure()
    if (httpOnly) b.httpOnly()
    if (maxAge > 0) {
        b.expiresAt(System.currentTimeMillis() + maxAge * 1000L)
    }
    return b.build()
}
