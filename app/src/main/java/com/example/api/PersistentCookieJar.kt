package com.example.api

import android.content.Context
import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class PersistentCookieJar(context: Context) : CookieJar {
    private val sharedPrefs = context.getSharedPreferences("tennis_cookies", Context.MODE_PRIVATE)
    private val cookiesStorage = ConcurrentHashMap<String, MutableMap<String, Cookie>>()

    init {
        // Load persistable cookies at initialization
        try {
            val allKeys = sharedPrefs.all
            for ((key, value) in allKeys) {
                if (value is Set<*>) {
                    val domainMap = ConcurrentHashMap<String, Cookie>()
                    for (cookieStr in value) {
                        if (cookieStr is String) {
                            // Dummy URL for parsing based on key (which represents host/domain)
                            val scheme = if (key.contains("localhost")) "http" else "https"
                            val parsedUrl = HttpUrl.Builder()
                                .scheme(scheme)
                                .host(key)
                                .build()
                            val cookie = Cookie.parse(parsedUrl, cookieStr)
                            if (cookie != null) {
                                domainMap[cookie.name] = cookie
                            }
                        }
                    }
                    if (domainMap.isNotEmpty()) {
                        cookiesStorage[key] = domainMap
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PersistentCookieJar", "Error reading stored cookies", e)
        }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val domainMap = cookiesStorage.getOrPut(host) { ConcurrentHashMap() }
        
        var modified = false
        for (cookie in cookies) {
            if (cookie.persistent && cookie.expiresAt < System.currentTimeMillis()) {
                // Expired, remove
                if (domainMap.remove(cookie.name) != null) {
                    modified = true
                }
            } else {
                domainMap[cookie.name] = cookie
                modified = true
            }
        }

        if (modified) {
            val activeCookies = domainMap.values.filter { !it.persistent || it.expiresAt >= System.currentTimeMillis() }
            val cookieStrings = activeCookies.map { it.toString() }.toSet()
            sharedPrefs.edit().putStringSet(host, cookieStrings).apply()
        }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val now = System.currentTimeMillis()
        val result = mutableListOf<Cookie>()
        
        // Load direct host cookies
        cookiesStorage[host]?.values?.let { result.addAll(it) }
        
        // Check for domain-matched cookies (like .hungsanity.com Matching hungsanity.com)
        for ((storedHost, domainMap) in cookiesStorage) {
            if (storedHost != host && (host.endsWith(".$storedHost") || storedHost.startsWith("."))) {
                result.addAll(domainMap.values)
            }
        }

        // Filter expired cookies and remove them from storage if expired
        val validCookies = result.filter { cookie ->
            val isExpired = cookie.persistent && cookie.expiresAt < now
            if (isExpired) {
                cookiesStorage[host]?.remove(cookie.name)
            }
            !isExpired
        }
        
        return validCookies
    }

    @Synchronized
    fun clear() {
        cookiesStorage.clear()
        sharedPrefs.edit().clear().apply()
    }
}
