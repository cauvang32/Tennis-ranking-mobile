package com.example.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.api.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException

object TennisRepository {
    private const val PREFS_NAME = "tennis_prefs"
    private const val KEY_BEARER_TOKEN = "bearer_token"
    private const val KEY_CSRF_TOKEN = "csrf_token"
    private const val KEY_USER_JSON = "user_json"

    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var cookieJar: PersistentCookieJar
    private lateinit var moshi: Moshi
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var sseClient: OkHttpClient // P1: Reusable SSE client
    private lateinit var api: TennisApi

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _csrfToken = MutableStateFlow<String?>(null)
    val csrfToken: StateFlow<String?> = _csrfToken.asStateFlow()

    private val _initData = MutableStateFlow<InitResponse?>(null)
    val initData: StateFlow<InitResponse?> = _initData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _themeOverride = MutableStateFlow<Boolean?>(null)
    val themeOverride: StateFlow<Boolean?> = _themeOverride.asStateFlow()

    fun toggleThemeOption() {
        val current = _themeOverride.value
        _themeOverride.value = if (current == null) false else if (current == false) true else null
    }

    fun setTheme(dark: Boolean?) {
        _themeOverride.value = dark
    }

    private var currentBearerToken: String? = null
    const val BASE_URL = "https://hungsanity.com/tennis/api/"

    fun initialize(androidContext: Context) {
        context = androidContext.applicationContext
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        cookieJar = PersistentCookieJar(context)
        
        moshi = Moshi.Builder()
            .add(CoerceIntAdapter())
            .add(CoerceDoubleAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()

        // Load credentials from Prefs
        currentBearerToken = sharedPrefs.getString(KEY_BEARER_TOKEN, null)
        _csrfToken.value = sharedPrefs.getString(KEY_CSRF_TOKEN, null)
        val userJson = sharedPrefs.getString(KEY_USER_JSON, null)
        if (userJson != null) {
            try {
                _currentUser.value = moshi.adapter(User::class.java).fromJson(userJson)
                _isAuthenticated.value = true
            } catch (e: Exception) {
                Log.e("TennisRepository", "Error reading stored user", e)
            }
        }

        // Configure HttpLogging Interceptor
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (com.example.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        // Configure Custom Header Interceptor for dynamic tokens
        val headerInterceptor = Interceptor { chain ->
            val builder = chain.request().newBuilder()
            
            // Inject Bearer token if present
            currentBearerToken?.let { token ->
                builder.addHeader("Authorization", "Bearer $token")
            }
            
            // Inject CSRF token if present
            _csrfToken.value?.let { csrf ->
                builder.addHeader("X-CSRF-Token", csrf)
            }
            
            // We are an API client: apply default only if no Accept header is present
            if (chain.request().header("Accept") == null) {
                builder.header("Accept", "application/json")
            }
            
            chain.proceed(builder.build())
        }

        okHttpClient = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(headerInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        // P1: Build SSE client once with no read timeout for long-lived connections
        sseClient = okHttpClient.newBuilder()
            .readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        api = retrofit.create(TennisApi::class.java)
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Parse standard backend error message body
    private fun parseError(response: Response<*>): String {
        try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val adapter = moshi.adapter(Map::class.java)
                val map = adapter.fromJson(errorBody)
                val errorMsg = map?.get("error") ?: map?.get("message")
                if (errorMsg != null) {
                    return errorMsg.toString()
                }
            }
        } catch (e: Exception) {
            Log.e("TennisRepository", "Error parsing api error body", e)
        }
        return "Server error: ${response.code()}"
    }

    // Call API with loading & error orchestration
    private suspend fun <T> safeApiCall(showLoading: Boolean = true, call: suspend () -> Response<T>): T? {
        if (showLoading) {
            _isLoading.value = true
        }
        _errorMessage.value = null
        try {
            val response = call()
            if (response.isSuccessful) {
                if (showLoading) {
                    _isLoading.value = false
                }
                return response.body()
            } else {
                val parsedError = parseError(response)
                // Handle 401 unauth / 403 CSRF problems
                if (response.code() == 401 || (response.code() == 403 && parsedError.contains("CSRF", ignoreCase = true))) {
                    Log.w("TennisRepository", "Auth expired or CSRF error, clearing credentials")
                    clearSession()
                }
                _errorMessage.value = parsedError
                if (showLoading) {
                    _isLoading.value = false
                }
                return null
            }
        } catch (e: IOException) {
            _errorMessage.value = "Network error. Please check your connection."
            Log.e("TennisRepository", "Network call failed", e)
            if (showLoading) {
                _isLoading.value = false
            }
            return null
        } catch (e: Exception) {
            _errorMessage.value = "Unexpected error occurred."
            Log.e("TennisRepository", "API execution error", e)
            if (showLoading) {
                _isLoading.value = false
            }
            return null
        }
    }

    // Fetch initial app summary bootstrap data (P2: simplified, no redundant deserialization)
    suspend fun fetchInitData(showLoading: Boolean = true): InitResponse? {
        val result = safeApiCall(showLoading) { api.getInitData() }
        if (result != null) {
            _initData.value = result
            // Auto update CSRF token if provided
            result.csrfToken?.let { token ->
                setCsrfToken(token)
            }
            _currentUser.value = result.user
            _isAuthenticated.value = result.isAuthenticated ?: false
            if (result.user == null && _isAuthenticated.value) {
                clearSession()
            }
        }
        return result
    }

    // Login Action
    suspend fun login(req: LoginRequest): Boolean {
        _isLoading.value = true
        _errorMessage.value = null
        try {
            val response = api.login(req)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    saveSession(body)
                    _isLoading.value = false
                    fetchInitData() // reload everything for authenticated environment
                    return true
                } else {
                    _errorMessage.value = body?.message ?: "Login failed"
                }
            } else {
                _errorMessage.value = parseError(response)
            }
        } catch (e: Exception) {
            _errorMessage.value = "Login error: ${e.message}"
            Log.e("TennisRepository", "Login exception", e)
        }
        _isLoading.value = false
        return false
    }

    // P9: Refresh Session with proper error handling
    suspend fun refreshSession(): Boolean {
        return try {
            val response = api.refreshAuth()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    saveSession(body)
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e("TennisRepository", "Session refresh failed", e)
            false
        }
    }

    // Logout Action
    suspend fun logout(): Boolean {
        safeApiCall { api.logout() }
        clearSession()
        fetchInitData() // reload clean init state for guest
        return true
    }

    // CSRF Fetch
    suspend fun fetchCsrfToken(): String? {
        val result = safeApiCall { api.getCsrfToken() }
        if (result != null) {
            setCsrfToken(result.csrfToken)
            return result.csrfToken
        }
        return null
    }

    // Poll current server data version 
    suspend fun checkDataVersion(showLoading: Boolean = false): Long? {
        val result = safeApiCall(showLoading) { api.getDataVersion() }
        return result?.version
    }

    // Player Operations
    suspend fun createPlayer(name: String): Boolean {
        val res = safeApiCall { api.createPlayer(CreatePlayerRequest(name)) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun deletePlayer(id: Int): Boolean {
        val res = safeApiCall { api.deletePlayer(id) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun fetchPlayers(): List<Player>? {
        return safeApiCall { api.getPlayers() }
    }

    // Season Operations
    suspend fun createSeason(req: CreateSeasonRequest): Boolean {
        val res = safeApiCall { api.createSeason(req) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun updateSeason(id: Int, req: CreateSeasonRequest): Boolean {
        val res = safeApiCall { api.updateSeason(id, req) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun deleteSeason(id: Int): Boolean {
        val res = safeApiCall { api.deleteSeason(id) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun endSeason(id: Int, endDate: String): Boolean {
        val res = safeApiCall { api.endSeason(id, EndSeasonRequest(endDate)) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun reactivateSeason(id: Int): Boolean {
        val res = safeApiCall { api.reactivateSeason(id) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun updateSeasonPlayers(seasonId: Int, playerIds: List<Int>): Boolean {
        val res = safeApiCall { api.replaceSeasonPlayers(seasonId, SeasonPlayersRequest(playerIds)) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun getSeasonPlayers(seasonId: Int): List<Player> {
        val res = safeApiCall { api.getSeasonPlayers(seasonId) }
        return res ?: emptyList()
    }

    // Match Operations
    suspend fun createMatch(req: CreateMatchRequest): Boolean {
        val res = safeApiCall { api.createMatch(req) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun updateMatch(id: Int, req: CreateMatchRequest): Boolean {
        val res = safeApiCall { api.updateMatch(id, req) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun deleteMatch(id: Int): Boolean {
        val res = safeApiCall { api.deleteMatch(id) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun fetchMatchesByDate(date: String): List<Match>? {
        return safeApiCall { api.getMatchesByDate(date) }
    }

    suspend fun fetchMatchesBySeason(seasonId: Int): List<Match>? {
        return safeApiCall { api.getMatchesBySeason(seasonId) }
    }

    // Rankings Operations
    suspend fun fetchLifetimeRankings(): List<RankingEntry>? {
        return safeApiCall { api.getLifetimeRankings() }
    }

    suspend fun fetchSeasonRankings(seasonId: Int): List<RankingEntry>? {
        return safeApiCall { api.getSeasonRankings(seasonId) }
    }

    suspend fun fetchDateRankings(date: String): List<RankingEntry>? {
        return safeApiCall { api.getDateRankings(date) }
    }

    // Session Management Implementation
    private fun saveSession(loginResponse: LoginResponse) {
        val editor = sharedPrefs.edit()
        
        loginResponse.token?.let { token ->
            currentBearerToken = token
            editor.putString(KEY_BEARER_TOKEN, token)
        }
        
        loginResponse.csrfToken?.let { csrf ->
            _csrfToken.value = csrf
            editor.putString(KEY_CSRF_TOKEN, csrf)
        }
        
        loginResponse.user?.let { user ->
            _currentUser.value = user
            _isAuthenticated.value = true
            
            try {
                val userStr = moshi.adapter(User::class.java).toJson(user)
                editor.putString(KEY_USER_JSON, userStr)
            } catch (e: Exception) {
                Log.e("TennisRepository", "Error serializing user object", e)
            }
        }
        
        editor.apply()
    }

    private fun setCsrfToken(csrf: String?) {
        _csrfToken.value = csrf
        sharedPrefs.edit().putString(KEY_CSRF_TOKEN, csrf).apply()
    }

    private fun clearSession() {
        currentBearerToken = null
        _currentUser.value = null
        _isAuthenticated.value = false
        _csrfToken.value = null
        
        sharedPrefs.edit()
            .remove(KEY_BEARER_TOKEN)
            .remove(KEY_CSRF_TOKEN)
            .remove(KEY_USER_JSON)
            .apply()

        cookieJar.clear()
    }

    private var isSseRunning = false
    private var syncScope: CoroutineScope? = null
    private var currentSseCall: okhttp3.Call? = null
    // P4: Debounce timestamp for SSE-triggered fetches
    private var lastSseFetchTime = 0L

    fun startBackgroundSync() {
        if (isSseRunning) return
        isSseRunning = true

        syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        // 1. SSE Connection stream loop
        syncScope?.launch {
            while (isActive) {
                startSseStream()
                // Wait 3 seconds before retrying SSE connection on failure/disconnect
                delay(3000)
            }
        }

        // 2. 15-second Polling fallback loop
        syncScope?.launch {
            while (isActive) {
                delay(15000)
                try {
                    // Poll data-version
                    val latestVersion = checkDataVersion(showLoading = false)
                    val currentVersion = _initData.value?.version
                    Log.d("TennisRepository", "Polling... Server version: $latestVersion, Local version: $currentVersion")
                    if (latestVersion != null && (currentVersion == null || latestVersion > currentVersion)) {
                        Log.d("TennisRepository", "Newer version detected via polling. Fetching init data...")
                        fetchInitData(showLoading = false)
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Log.e("TennisRepository", "Polling fallback error: ${e.message}")
                    }
                }
            }
        }
    }

    fun stopBackgroundSync() {
        if (!isSseRunning) return
        isSseRunning = false
        Log.i("TennisRepository", "Stopping background sync...")
        syncScope?.cancel()
        syncScope = null
        currentSseCall?.cancel()
        currentSseCall = null
    }

    private suspend fun startSseStream() = withContext(Dispatchers.IO) {
        val request = okhttp3.Request.Builder()
            .url(BASE_URL + "events")
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .header("Connection", "keep-alive")
            .build()

        try {
            Log.d("TennisRepository", "Connecting to SSE stream at ${BASE_URL}events")
            // P1: Reuse pre-built SSE client instead of rebuilding on every reconnect
            val call = sseClient.newCall(request)
            currentSseCall = call
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("TennisRepository", "SSE connection failed: code ${response.code}")
                    return@withContext
                }

                val source = response.body?.source() ?: return@withContext
                Log.i("TennisRepository", "SSE stream established successfully")

                while (isActive) {
                    val line = source.readUtf8Line() ?: break
                    val trimmed = line.trim()
                    if (trimmed.startsWith("data:")) {
                        // P4: Debounce — skip if last fetch was less than 2 seconds ago
                        val now = System.currentTimeMillis()
                        if (now - lastSseFetchTime > 2000) {
                            lastSseFetchTime = now
                            Log.i("TennisRepository", "SSE event received, triggering sync update")
                            fetchInitData(showLoading = false)
                        } else {
                            Log.d("TennisRepository", "SSE event debounced, skipping fetch")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (e !is CancellationException && e.message?.equals("canceled", ignoreCase = true) != true) {
                Log.e("TennisRepository", "SSE stream disconnect / exception: ${e.message}")
            }
        } finally {
            currentSseCall = null
        }
    }
}

class CoerceIntAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Int? {
        if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<Any>()
            return null
        }
        if (reader.peek() == JsonReader.Token.NUMBER) {
            return reader.nextInt()
        }
        val str = reader.nextString()
        return str.toDoubleOrNull()?.toInt() ?: str.toIntOrNull()
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: Int?) {
        writer.value(value)
    }
}

class CoerceDoubleAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Double? {
        if (reader.peek() == JsonReader.Token.NULL) {
            reader.nextNull<Any>()
            return null
        }
        if (reader.peek() == JsonReader.Token.NUMBER) {
            return reader.nextDouble()
        }
        val str = reader.nextString()
        return str.toDoubleOrNull()
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: Double?) {
        writer.value(value)
    }
}
