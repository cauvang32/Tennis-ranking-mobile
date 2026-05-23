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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private val _isSandboxMode = MutableStateFlow(false)
    val isSandboxMode: StateFlow<Boolean> = _isSandboxMode.asStateFlow()

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

    fun setSandboxMode(enabled: Boolean) {
        _isSandboxMode.value = enabled
        sharedPrefs.edit().putBoolean("is_sandbox_mode", enabled).apply()
        if (enabled) {
            _isAuthenticated.value = true
            _currentUser.value = User(999, "demo_admin", "demo@example.com", "admin", "Demo Sandbox Admin")
            val cached = loadSandboxDataFromPrefs()
            if (cached != null) {
                _initData.value = cached
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    fetchInitData(showLoading = false)
                }
            }
        } else {
            clearSession()
            CoroutineScope(Dispatchers.IO).launch {
                fetchInitData(showLoading = true)
            }
        }
    }

    fun saveSandboxDataToPrefs() {
        _initData.value?.let { data ->
            try {
                val dataStr = moshi.adapter(InitResponse::class.java).toJson(data)
                sharedPrefs.edit().putString("sandbox_init_data", dataStr).apply()
            } catch (e: Exception) {
                Log.e("TennisRepository", "Error saving sandbox data", e)
            }
        }
    }

    private fun loadSandboxDataFromPrefs(): InitResponse? {
        val dataStr = sharedPrefs.getString("sandbox_init_data", null)
        if (dataStr != null) {
            try {
                return moshi.adapter(InitResponse::class.java).fromJson(dataStr)
            } catch (e: Exception) {
                Log.e("TennisRepository", "Error loading sandbox data", e)
            }
        }
        return null
    }

    fun initialize(androidContext: Context) {
        context = androidContext.applicationContext
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        cookieJar = PersistentCookieJar(context)
        
        moshi = Moshi.Builder()
            .add(CoerceIntAdapter())
            .add(CoerceDoubleAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()

        _isSandboxMode.value = false

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
                if (response.code() == 401 || response.code() == 403 && parsedError.contains("CSRF", ignoreCase = true)) {
                    Log.w("TennisRepository", "Auth expired, clearing credentials")
                    // If CSRF error or forbidden, refresh csrf optionally or log out
                    if (response.code() == 401) {
                        clearSession()
                    }
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

    // Fetch initial app summary bootstrap data
    suspend fun fetchInitData(showLoading: Boolean = true): InitResponse? {
        if (_isSandboxMode.value) {
            val cached = loadSandboxDataFromPrefs()
            if (cached != null) {
                _initData.value = cached
                return cached
            }
        }
        val result = safeApiCall(showLoading) { api.getInitData() }
        if (result != null) {
            if (_isSandboxMode.value) {
                val cached = loadSandboxDataFromPrefs()
                if (cached != null) {
                    _initData.value = cached
                    return cached
                } else {
                    _initData.value = result
                    saveSandboxDataToPrefs()
                }
            } else {
                _initData.value = result
            }
            // Auto update CSRF token if database provides one
            result.csrfToken?.let { token ->
                setCsrfToken(token)
            }
            if (!_isSandboxMode.value && result.user != null) {
                _currentUser.value = result.user
                _isAuthenticated.value = result.isAuthenticated ?: false
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

    // Refresh Session
    suspend fun refreshSession(): Boolean {
        val response = api.refreshAuth()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success) {
                saveSession(body)
                return true
            }
        }
        return false
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
        if (_isSandboxMode.value) {
            val current = _initData.value ?: return false
            val newId = (current.players?.maxOfOrNull { it.id } ?: 0) + 1
            val newPlayer = Player(newId, name, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            val updatedPlayers = (current.players ?: emptyList()) + newPlayer
            val updatedRankings = (current.lifetimeRankings ?: emptyList()) + RankingEntry(
                id = newId, name = name, wins = 0, losses = 0, totalMatches = 0, points = 0, winPercentage = 0.0, moneyLost = 0, form = emptyList()
            )
            val updated = current.copy(players = updatedPlayers, lifetimeRankings = updatedRankings)
            _initData.value = updated
            saveSandboxDataToPrefs()
            return true
        }
        val res = safeApiCall { api.createPlayer(CreatePlayerRequest(name)) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun deletePlayer(id: Int): Boolean {
        if (_isSandboxMode.value) {
            val current = _initData.value ?: return false
            val updatedPlayers = current.players?.filter { it.id != id }
            val updatedRankings = current.lifetimeRankings?.filter { it.id != id }
            val updated = current.copy(players = updatedPlayers, lifetimeRankings = updatedRankings)
            _initData.value = updated
            saveSandboxDataToPrefs()
            return true
        }
        val res = safeApiCall { api.deletePlayer(id) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun fetchPlayers(): List<Player>? {
        if (_isSandboxMode.value) {
            return _initData.value?.players
        }
        return safeApiCall { api.getPlayers() }
    }

    // Season Operations
    suspend fun createSeason(req: CreateSeasonRequest): Boolean {
        if (_isSandboxMode.value) {
            val current = _initData.value ?: return false
            val newId = (current.seasons?.maxOfOrNull { it.id } ?: 0) + 1
            val newSeason = Season(
                id = newId,
                name = req.name,
                startDate = req.startDate,
                endDate = req.endDate,
                isActive = true,
                autoEnd = req.autoEnd,
                description = req.description,
                loseMoneyPerLoss = req.loseMoneyPerLoss ?: 20000
            )
            val updatedSeasons = (current.seasons ?: emptyList()).map {
                it.copy(isActive = false)
            } + newSeason
            val updated = current.copy(seasons = updatedSeasons, activeSeason = newSeason)
            _initData.value = updated
            saveSandboxDataToPrefs()
            return true
        }
        val res = safeApiCall { api.createSeason(req) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun updateSeason(id: Int, req: CreateSeasonRequest): Boolean {
        if (_isSandboxMode.value) {
            val current = _initData.value ?: return false
            val updatedSeasons = current.seasons?.map {
                if (it.id == id) {
                    it.copy(
                        name = req.name,
                        startDate = req.startDate,
                        endDate = req.endDate,
                        autoEnd = req.autoEnd,
                        description = req.description,
                        loseMoneyPerLoss = req.loseMoneyPerLoss ?: 20000
                    )
                } else it
            }
            val activeSeason = if (current.activeSeason?.id == id) {
                current.activeSeason.copy(
                    name = req.name,
                    startDate = req.startDate,
                    endDate = req.endDate,
                    autoEnd = req.autoEnd,
                    description = req.description,
                    loseMoneyPerLoss = req.loseMoneyPerLoss ?: 20000
                )
            } else current.activeSeason
            val updated = current.copy(seasons = updatedSeasons, activeSeason = activeSeason)
            _initData.value = updated
            saveSandboxDataToPrefs()
            return true
        }
        val res = safeApiCall { api.updateSeason(id, req) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun deleteSeason(id: Int): Boolean {
        if (_isSandboxMode.value) {
            val current = _initData.value ?: return false
            val updatedSeasons = current.seasons?.filter { it.id != id }
            val activeSeason = if (current.activeSeason?.id == id) {
                updatedSeasons?.firstOrNull { it.isActive }
            } else current.activeSeason
            val updated = current.copy(seasons = updatedSeasons, activeSeason = activeSeason)
            _initData.value = updated
            saveSandboxDataToPrefs()
            return true
        }
        val res = safeApiCall { api.deleteSeason(id) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun endSeason(id: Int, endDate: String): Boolean {
        if (_isSandboxMode.value) {
            val current = _initData.value ?: return false
            val updatedSeasons = current.seasons?.map {
                if (it.id == id) it.copy(isActive = false, endDate = endDate) else it
            }
            val activeSeason = if (current.activeSeason?.id == id) current.activeSeason.copy(isActive = false, endDate = endDate) else current.activeSeason
            val updated = current.copy(seasons = updatedSeasons, activeSeason = activeSeason)
            _initData.value = updated
            saveSandboxDataToPrefs()
            return true
        }
        val res = safeApiCall { api.endSeason(id, EndSeasonRequest(endDate)) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun reactivateSeason(id: Int): Boolean {
        if (_isSandboxMode.value) {
            val current = _initData.value ?: return false
            val updatedSeasons = current.seasons?.map {
                if (it.id == id) it.copy(isActive = true, endDate = null) else it.copy(isActive = false)
            }
            val active = updatedSeasons?.find { it.id == id }
            val updated = current.copy(seasons = updatedSeasons, activeSeason = active)
            _initData.value = updated
            saveSandboxDataToPrefs()
            return true
        }
        val res = safeApiCall { api.reactivateSeason(id) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun updateSeasonPlayers(seasonId: Int, playerIds: List<Int>): Boolean {
        if (_isSandboxMode.value) {
            return true
        }
        val res = safeApiCall { api.replaceSeasonPlayers(seasonId, SeasonPlayersRequest(playerIds)) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun getSeasonPlayers(seasonId: Int): List<Player> {
        if (_isSandboxMode.value) return emptyList()
        val res = safeApiCall { api.getSeasonPlayers(seasonId) }
        return res ?: emptyList()
    }

    // Match Operations
    suspend fun createMatch(req: CreateMatchRequest): Boolean {
        if (_isSandboxMode.value) {
            val current = _initData.value ?: return false
            val newId = (current.defaultDateMatches?.maxOfOrNull { it.id } ?: 0) + 1
            val p1_name = current.players?.find { it.id == req.player1Id }?.name ?: "Player ${req.player1Id}"
            val p2_name = current.players?.find { it.id == req.player2Id }?.name
            val p3_name = current.players?.find { it.id == req.player3Id }?.name ?: "Player ${req.player3Id}"
            val p4_name = current.players?.find { it.id == req.player4Id }?.name
            
            val newMatch = Match(
                id = newId,
                seasonId = req.seasonId,
                playDate = req.playDate,
                player1Id = req.player1Id,
                player2Id = req.player2Id,
                player3Id = req.player3Id,
                player4Id = req.player4Id,
                team1Score = req.team1Score,
                team2Score = req.team2Score,
                winningTeam = req.winningTeam,
                matchType = req.matchType,
                player1_name = p1_name,
                player2_name = p2_name,
                player3_name = p3_name,
                player4_name = p4_name
            )
            
            val updatedMatches = listOf(newMatch) + (current.defaultDateMatches ?: emptyList())
            
            val updatedPlayDates = if (current.playDates?.any { it.playDate == req.playDate } == true) {
                current.playDates
            } else {
                listOf(PlayDateEntry(req.playDate)) + (current.playDates ?: emptyList())
            }
            
            val season = current.seasons?.find { it.id == req.seasonId }
            val lossPenalty = season?.loseMoneyPerLoss ?: 20000
            
            val updatedRankings = current.lifetimeRankings?.map { ranking ->
                var wins = ranking.wins
                var losses = ranking.losses
                var total = ranking.totalMatches
                var points = ranking.points
                var money = ranking.moneyLost ?: 0
                
                val pIdsInTeam1 = listOfNotNull(req.player1Id, req.player2Id)
                val pIdsInTeam2 = listOfNotNull(req.player3Id, req.player4Id)
                
                if (pIdsInTeam1.contains(ranking.id)) {
                    total += 1
                    if (req.winningTeam == 1) {
                        wins += 1
                        points += 3
                    } else {
                        losses += 1
                        money += lossPenalty
                    }
                } else if (pIdsInTeam2.contains(ranking.id)) {
                    total += 1
                    if (req.winningTeam == 2) {
                        wins += 1
                        points += 3
                    } else {
                        losses += 1
                        money += lossPenalty
                    }
                }
                
                val pct = if (total > 0) (wins.toDouble() / total.toDouble()) * 100.0 else 0.0
                ranking.copy(
                    wins = wins,
                    losses = losses,
                    totalMatches = total,
                    points = points,
                    winPercentage = pct,
                    moneyLost = money
                )
            }
            
            val updated = current.copy(
                defaultDateMatches = updatedMatches,
                playDates = updatedPlayDates,
                lifetimeRankings = updatedRankings
            )
            _initData.value = updated
            saveSandboxDataToPrefs()
            return true
        }
        
        val res = safeApiCall { api.createMatch(req) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun updateMatch(id: Int, req: CreateMatchRequest): Boolean {
        if (_isSandboxMode.value) {
            val current = _initData.value ?: return false
            val p1_name = current.players?.find { it.id == req.player1Id }?.name ?: "Player ${req.player1Id}"
            val p2_name = current.players?.find { it.id == req.player2Id }?.name
            val p3_name = current.players?.find { it.id == req.player3Id }?.name ?: "Player ${req.player3Id}"
            val p4_name = current.players?.find { it.id == req.player4Id }?.name
            
            val updatedMatches = current.defaultDateMatches?.map {
                if (it.id == id) {
                    it.copy(
                        seasonId = req.seasonId,
                        playDate = req.playDate,
                        player1Id = req.player1Id,
                        player2Id = req.player2Id,
                        player3Id = req.player3Id,
                        player4Id = req.player4Id,
                        team1Score = req.team1Score,
                        team2Score = req.team2Score,
                        winningTeam = req.winningTeam,
                        matchType = req.matchType,
                        player1_name = p1_name,
                        player2_name = p2_name,
                        player3_name = p3_name,
                        player4_name = p4_name
                    )
                } else it
            }
            val updated = current.copy(defaultDateMatches = updatedMatches)
            _initData.value = updated
            saveSandboxDataToPrefs()
            return true
        }
        val res = safeApiCall { api.updateMatch(id, req) }
        if (res?.success == true) {
            fetchInitData()
            return true
        }
        return false
    }

    suspend fun deleteMatch(id: Int): Boolean {
        if (_isSandboxMode.value) {
            val current = _initData.value ?: return false
            val match = current.defaultDateMatches?.find { it.id == id }
            val updatedMatches = current.defaultDateMatches?.filter { it.id != id }
            
            val updatedRankings = if (match != null) {
                val season = current.seasons?.find { it.id == match.seasonId }
                val lossPenalty = season?.loseMoneyPerLoss ?: 20000
                current.lifetimeRankings?.map { ranking ->
                    var wins = ranking.wins
                    var losses = ranking.losses
                    var total = ranking.totalMatches
                    var points = ranking.points
                    var money = ranking.moneyLost ?: 0
                    
                    val pIdsInTeam1 = listOfNotNull(match.player1Id, match.player2Id)
                    val pIdsInTeam2 = listOfNotNull(match.player3Id, match.player4Id)
                    
                    if (pIdsInTeam1.contains(ranking.id)) {
                        if (total > 0) total -= 1
                        if (match.winningTeam == 1) {
                            if (wins > 0) wins -= 1
                            if (points >= 3) points -= 3
                        } else {
                            if (losses > 0) losses -= 1
                            if (money >= lossPenalty) money -= lossPenalty
                        }
                    } else if (pIdsInTeam2.contains(ranking.id)) {
                        if (total > 0) total -= 1
                        if (match.winningTeam == 2) {
                            if (wins > 0) wins -= 1
                            if (points >= 3) points -= 3
                        } else {
                            if (losses > 0) losses -= 1
                            if (money >= lossPenalty) money -= lossPenalty
                        }
                    }
                    val pct = if (total > 0) (wins.toDouble() / total.toDouble()) * 100.0 else 0.0
                    ranking.copy(
                        wins = wins,
                        losses = losses,
                        totalMatches = total,
                        points = points,
                        winPercentage = pct,
                        moneyLost = money
                    )
                }
            } else {
                current.lifetimeRankings
            }
            
            val updated = current.copy(defaultDateMatches = updatedMatches, lifetimeRankings = updatedRankings)
            _initData.value = updated
            saveSandboxDataToPrefs()
            return true
        }
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

    fun startBackgroundSync() {
        if (isSseRunning) return
        isSseRunning = true

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        // 1. SSE Connection stream loop
        scope.launch {
            while (true) {
                startSseStream()
                // Wait 10 seconds before retrying SSE connection on failure/disconnect
                delay(10000)
            }
        }

        // 2. 15-second Polling fallback loop
        scope.launch {
            while (true) {
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
                    Log.e("TennisRepository", "Polling fallback error: ${e.message}")
                }
            }
        }
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
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("TennisRepository", "SSE connection failed: code ${response.code}")
                    return@withContext
                }

                val source = response.body?.source() ?: return@withContext
                Log.i("TennisRepository", "SSE stream established successfully")

                while (true) {
                    val line = source.readUtf8Line() ?: break
                    val trimmed = line.trim()
                    if (trimmed.startsWith("data:")) {
                        Log.i("TennisRepository", "SSE event received, triggering sync update")
                        fetchInitData(showLoading = false)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TennisRepository", "SSE stream disconnect / exception: ${e.message}")
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
