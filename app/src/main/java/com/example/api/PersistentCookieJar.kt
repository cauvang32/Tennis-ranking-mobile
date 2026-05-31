package com.example.api

import android.content.Context
import android.util.Log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class PersistentCookieJar(context: Context) : CookieJar {
    // SECURITY FIX: Use EncryptedSharedPreferences for secure cookie storage
    private val masterKey = androidx.security.crypto.MasterKey.Builder(context)
        .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val sharedPrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
        context,
        "tennis_cookies_encrypted",
        masterKey,
        androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
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
        
        var cookiesModified = false
        for (cookie in cookies) {
            if (cookie.persistent && cookie.expiresAt < System.currentTimeMillis()) {
                // Expired, remove
                if (domainMap.remove(cookie.name) != null) { cookiesModified = true }
            } else {
                domainMap[cookie.name] = cookie
                cookiesModified = true
            }
        }

        if (cookiesModified) {
            val activeCookies = domainMap.values.filter { !it.persistent || it.expiresAt >= System.currentTimeMillis() }
            val cookieStrings = activeCookies.map { it.toString() }.toSet()
            sharedPrefs.edit().putStringSet(host, cookieStrings).apply()
        }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val now = System.currentTimeMillis()
        // Track each cookie alongside its actual storage key (origin host)
        val candidates = mutableListOf<Pair<String, Cookie>>()
        
        // Load direct host cookies
        cookiesStorage[host]?.values?.forEach { candidates.add(host to it) }
        
        // Check for domain-matched cookies (like .hungsanity.com matching hungsanity.com)
        for ((storedHost, domainMap) in cookiesStorage) {
            if (storedHost != host && (host.endsWith(".$storedHost") || storedHost.startsWith("."))) {
                domainMap.values.forEach { candidates.add(storedHost to it) }
            }
        }

        // Filter expired cookies, remove from correct storage key, and persist changes
        val modifiedHosts = mutableSetOf<String>()
        val validCookies = mutableListOf<Cookie>()

        for ((originHost, cookie) in candidates) {
            val isExpired = cookie.persistent && cookie.expiresAt < now
            if (isExpired) {
                cookiesStorage[originHost]?.remove(cookie.name)
                modifiedHosts.add(originHost)
            } else {
                validCookies.add(cookie)
            }
        }

        // Persist any expired cookie removals to disk
        if (modifiedHosts.isNotEmpty()) {
            val editor = sharedPrefs.edit()
            for (modifiedHost in modifiedHosts) {
                val remaining = cookiesStorage[modifiedHost]?.values
                    ?.filter { !it.persistent || it.expiresAt >= now }
                    ?.map { it.toString() }?.toSet()
                if (remaining.isNullOrEmpty()) {
                    editor.remove(modifiedHost)
                } else {
                    editor.putStringSet(modifiedHost, remaining)
                }
            }
            editor.apply()
        }
        
        return validCookies
    }

    @Synchronized
    fun clear() {
        cookiesStorage.clear()
        sharedPrefs.edit().clear().apply()
    }
}
