package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    val id: Int? = null,
    val username: String? = null,
    val email: String? = null,
    val role: String? = null, // admin, editor, viewer
    @Json(name = "displayName") val displayName: String? = null
)

@JsonClass(generateAdapter = true)
data class Player(
    val id: Int,
    val name: String,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Season(
    val id: Int,
    val name: String,
    @Json(name = "start_date") val startDate: String,
    @Json(name = "end_date") val endDate: String?,
    @Json(name = "is_active") val isActive: Boolean,
    @Json(name = "auto_end") val autoEnd: Boolean,
    val description: String?,
    @Json(name = "lose_money_per_loss") val loseMoneyPerLoss: Int?
)

@JsonClass(generateAdapter = true)
data class Match(
    val id: Int,
    @Json(name = "season_id") val seasonId: Int,
    @Json(name = "play_date") val playDate: String,
    @Json(name = "player1_id") val player1Id: Int,
    @Json(name = "player2_id") val player2Id: Int?,
    @Json(name = "player3_id") val player3Id: Int,
    @Json(name = "player4_id") val player4Id: Int?,
    @Json(name = "team1_score") val team1Score: Int,
    @Json(name = "team2_score") val team2Score: Int,
    @Json(name = "winning_team") val winningTeam: Int, // 1 or 2
    @Json(name = "match_type") val matchType: String, // solo or duo
    
    // UI Helpers (sometimes returned as joined fields or we can look them up)
    val player1_name: String? = null,
    val player2_name: String? = null,
    val player3_name: String? = null,
    val player4_name: String? = null
)

@JsonClass(generateAdapter = true)
data class FormEntry(
    val result: String?,
    @Json(name = "play_date") val playDate: String?
)

@JsonClass(generateAdapter = true)
data class RankingEntry(
    val id: Int, // player ID
    val name: String,
    val wins: Int,
    val losses: Int,
    @Json(name = "total_matches") val totalMatches: Int,
    val points: Int,
    @Json(name = "win_percentage") val winPercentage: Double?,
    @Json(name = "money_lost") val moneyLost: Int?,
    val form: List<FormEntry>? = null
) {
    val formStrings: List<String>
        get() = form?.map { if (it.result?.lowercase() == "win") "W" else "L" } ?: emptyList()
}

@JsonClass(generateAdapter = true)
data class PlayDateEntry(
    @Json(name = "play_date") val playDate: String
)

@JsonClass(generateAdapter = true)
data class InitResponse(
    val lifetimeRankings: List<RankingEntry>? = null,
    val players: List<Player>? = null,
    val seasons: List<Season>? = null,
    val activeSeasons: List<Season>? = null,
    val playDates: List<PlayDateEntry>? = null,
    val activeSeason: Season? = null,
    val defaultDate: String? = null,
    val defaultDateRankings: List<RankingEntry>? = null,
    val defaultDateMatches: List<Match>? = null,
    val isAuthenticated: Boolean? = false,
    val user: User? = null,
    val csrfToken: String? = null,
    val version: Long? = null
) {
    val playDateStrings: List<String>
        get() = playDates?.map { it.playDate } ?: emptyList()
}

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val username: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val csrfToken: String?,
    val user: User?,
    val token: String?,
    val authMethod: String?
)

@JsonClass(generateAdapter = true)
data class GeneralResponse(
    val success: Boolean,
    val message: String?,
    val id: Int? = null,
    val name: String? = null
)

@JsonClass(generateAdapter = true)
data class CSRFResponse(
    val csrfToken: String
)

@JsonClass(generateAdapter = true)
data class DataVersionResponse(
    val version: Long
)

// Request payloads
@JsonClass(generateAdapter = true)
data class CreatePlayerRequest(
    val name: String
)

@JsonClass(generateAdapter = true)
data class CreateSeasonRequest(
    val name: String,
    val startDate: String,
    val endDate: String?,
    val autoEnd: Boolean,
    val loseMoneyPerLoss: Int,
    val playerIds: List<Int>,
    val description: String?
)

@JsonClass(generateAdapter = true)
data class CreateMatchRequest(
    val seasonId: Int,
    val playDate: String,
    val player1Id: Int,
    val player2Id: Int?,
    val player3Id: Int,
    val player4Id: Int?,
    val team1Score: Int,
    val team2Score: Int,
    val winningTeam: Int,
    val matchType: String
)

@JsonClass(generateAdapter = true)
data class EndSeasonRequest(
    val endDate: String
)

@JsonClass(generateAdapter = true)
data class SeasonPlayersRequest(
    val playerIds: List<Int>
)
