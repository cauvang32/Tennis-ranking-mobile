package com.example.api

import retrofit2.Response
import retrofit2.http.*

interface TennisApi {

    // Auth
    @POST("auth/login")
    @Headers("Accept: application/json")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("auth/status")
    suspend fun checkAuthStatus(): Response<LoginResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<GeneralResponse>

    @POST("auth/refresh")
    suspend fun refreshAuth(): Response<LoginResponse>

    // System
    @GET("init")
    suspend fun getInitData(): Response<InitResponse>

    @GET("data-version")
    suspend fun getDataVersion(): Response<DataVersionResponse>

    @POST("csrf-token")
    suspend fun getCsrfToken(): Response<CSRFResponse>

    // Players
    @GET("players")
    suspend fun getPlayers(): Response<List<Player>>

    @POST("players")
    suspend fun createPlayer(@Body request: CreatePlayerRequest): Response<GeneralResponse>

    @DELETE("players/{id}")
    suspend fun deletePlayer(@Path("id") id: Int): Response<GeneralResponse>

    // Seasons
    @GET("seasons")
    suspend fun getSeasons(): Response<List<Season>>

    @GET("seasons/active")
    suspend fun getActiveSeasons(): Response<List<Season>>

    @POST("seasons")
    suspend fun createSeason(@Body request: CreateSeasonRequest): Response<GeneralResponse>

    @PUT("seasons/{id}")
    suspend fun updateSeason(
        @Path("id") id: Int,
        @Body request: CreateSeasonRequest
    ): Response<GeneralResponse>

    @DELETE("seasons/{id}")
    suspend fun deleteSeason(@Path("id") id: Int): Response<GeneralResponse>

    @GET("seasons/{id}/players")
    suspend fun getSeasonPlayers(@Path("id") id: Int): Response<List<Player>>

    @POST("seasons/{id}/players")
    suspend fun replaceSeasonPlayers(
        @Path("id") id: Int,
        @Body request: SeasonPlayersRequest
    ): Response<GeneralResponse>

    @POST("seasons/{id}/end")
    suspend fun endSeason(
        @Path("id") id: Int,
        @Body request: EndSeasonRequest
    ): Response<GeneralResponse>

    @POST("seasons/{id}/reactivate")
    suspend fun reactivateSeason(@Path("id") id: Int): Response<GeneralResponse>

    // Matches
    @GET("matches")
    suspend fun getMatches(@Query("limit") limit: Int? = 100): Response<List<Match>>

    @GET("matches/by-date/{date}")
    suspend fun getMatchesByDate(@Path("date") date: String): Response<List<Match>>

    @GET("matches/by-season/{seasonId}")
    suspend fun getMatchesBySeason(@Path("seasonId") seasonId: Int): Response<List<Match>>

    @POST("matches")
    suspend fun createMatch(@Body request: CreateMatchRequest): Response<GeneralResponse>

    @PUT("matches/{id}")
    suspend fun updateMatch(
        @Path("id") id: Int,
        @Body request: CreateMatchRequest
    ): Response<GeneralResponse>

    @DELETE("matches/{id}")
    suspend fun deleteMatch(@Path("id") id: Int): Response<GeneralResponse>

    @GET("matches/play-dates/latest")
    suspend fun getLatestPlayDate(): Response<Map<String, String>>

    // Rankings
    @GET("rankings/lifetime")
    suspend fun getLifetimeRankings(): Response<List<RankingEntry>>

    @GET("rankings/season/{seasonId}")
    suspend fun getSeasonRankings(@Path("seasonId") seasonId: Int): Response<List<RankingEntry>>

    @GET("rankings/date/{date}")
    suspend fun getDateRankings(@Path("date") date: String): Response<List<RankingEntry>>
}
