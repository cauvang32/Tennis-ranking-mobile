package com.example.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_settings")

/**
 * NotificationRepository - Tracks the last seen match and season IDs
 * to detect new items and trigger notifications.
 *
 * Uses DataStore for persistence across app restarts.
 */
object NotificationRepository {

    private lateinit var context: Context

    private val LAST_SEEN_MATCH_ID = intPreferencesKey("last_seen_match_id")
    private val LAST_SEEN_SEASON_ID = intPreferencesKey("last_seen_season_id")
    private val LAST_FETCH_TIMESTAMP = longPreferencesKey("last_fetch_timestamp")

    fun initialize(androidContext: Context) {
        context = androidContext.applicationContext
    }

    /**
     * Get the last seen match ID (thread-safe for initialization)
     */
    fun getLastSeenMatchId(): Int {
        return runBlocking {
            context.notificationDataStore.data.first()[LAST_SEEN_MATCH_ID] ?: 0
        }
    }

    /**
     * Get the last seen season ID (thread-safe for initialization)
     */
    fun getLastSeenSeasonId(): Int {
        return runBlocking {
            context.notificationDataStore.data.first()[LAST_SEEN_SEASON_ID] ?: 0
        }
    }

    /**
     * Update the last seen match ID
     */
    suspend fun updateLastSeenMatchId(matchId: Int) {
        context.notificationDataStore.edit { prefs ->
            val current = prefs[LAST_SEEN_MATCH_ID] ?: 0
            if (matchId > current) {
                prefs[LAST_SEEN_MATCH_ID] = matchId
                Log.d("NotificationRepo", "Updated last seen match ID to $matchId")
            }
        }
    }

    /**
     * Update the last seen season ID
     */
    suspend fun updateLastSeenSeasonId(seasonId: Int) {
        context.notificationDataStore.edit { prefs ->
            val current = prefs[LAST_SEEN_SEASON_ID] ?: 0
            if (seasonId > current) {
                prefs[LAST_SEEN_SEASON_ID] = seasonId
                Log.d("NotificationRepo", "Updated last seen season ID to $seasonId")
            }
        }
    }

    /**
     * Check if a match is new (has a higher ID than last seen)
     */
    suspend fun isNewMatch(matchId: Int): Boolean {
        val lastSeen = getLastSeenMatchId()
        return matchId > lastSeen
    }

    /**
     * Check if a season is new (has a higher ID than last seen)
     */
    suspend fun isNewSeason(seasonId: Int): Boolean {
        val lastSeen = getLastSeenSeasonId()
        return seasonId > lastSeen
    }

    /**
     * Find new matches by comparing current list with last seen ID
     * Returns list of new matches (sorted by ID descending - newest first)
     */
    fun findNewMatches(currentMatchIds: List<Int>): List<Int> {
        val lastSeen = getLastSeenMatchId()
        return currentMatchIds
            .filter { it > lastSeen }
            .sortedDescending()
    }

    /**
     * Find new seasons by comparing current list with last seen ID
     * Returns list of new seasons (sorted by ID descending - newest first)
     */
    fun findNewSeasons(currentSeasonIds: List<Int>): List<Int> {
        val lastSeen = getLastSeenSeasonId()
        return currentSeasonIds
            .filter { it > lastSeen }
            .sortedDescending()
    }

    /**
     * Mark all matches as seen up to a certain ID
     */
    suspend fun markAllMatchesSeen(upToId: Int) {
        val current = getLastSeenMatchId()
        if (upToId > current) {
            context.notificationDataStore.edit { prefs ->
                prefs[LAST_SEEN_MATCH_ID] = upToId
            }
            Log.d("NotificationRepo", "Marked all matches seen up to ID $upToId")
        }
    }

    /**
     * Mark all seasons as seen up to a certain ID
     */
    suspend fun markAllSeasonsSeen(upToId: Int) {
        val current = getLastSeenSeasonId()
        if (upToId > current) {
            context.notificationDataStore.edit { prefs ->
                prefs[LAST_SEEN_SEASON_ID] = upToId
            }
            Log.d("NotificationRepo", "Marked all seasons seen up to ID $upToId")
        }
    }
}
