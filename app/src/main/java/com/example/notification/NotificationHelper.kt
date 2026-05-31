package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * NotificationHelper - Manages Android local notifications for new matches and seasons.
 *
 * Uses NotificationCompat for backward compatibility with Android 7.0+ (API 24).
 * Creates a notification channel for Android 8.0+ (API 26) as required.
 */
object NotificationHelper {

    private const val MATCH_CHANNEL_ID = "tennis_match_channel"
    private const val MATCH_CHANNEL_NAME = "Match Updates"
    private const val MATCH_CHANNEL_DESC = "Notifications for new tennis matches"

    private const val SEASON_CHANNEL_ID = "tennis_season_channel"
    private const val SEASON_CHANNEL_NAME = "Season Updates"
    private const val SEASON_CHANNEL_DESC = "Notifications for new tennis seasons"

    private const val MATCH_NOTIFICATION_ID_BASE = 1000
    private const val SEASON_NOTIFICATION_ID_BASE = 2000

    /**
     * Initialize notification channels (required for Android 8.0+)
     */
    fun initialize(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Match update channel
            val matchChannel = NotificationChannel(
                MATCH_CHANNEL_ID,
                MATCH_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = MATCH_CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }

            // Season update channel
            val seasonChannel = NotificationChannel(
                SEASON_CHANNEL_ID,
                SEASON_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = SEASON_CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(matchChannel, seasonChannel))
        }
    }

    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below, notifications are allowed by default
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Show notification for a new match
     */
    fun showNewMatchNotification(
        context: Context,
        matchId: Int,
        player1Name: String,
        player2Name: String,
        matchDetails: String = ""
    ) {
        if (!hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "match")
            putExtra("match_id", matchId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            matchId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⚽ New Match Added!"
        val content = if (matchDetails.isNotEmpty()) {
            matchDetails
        } else {
            "$player1Name vs $player2Name"
        }

        val notification = NotificationCompat.Builder(context, MATCH_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$player1Name vs $player2Name\n$matchDetails"))
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(MATCH_NOTIFICATION_ID_BASE + matchId, notification)
        } catch (e: SecurityException) {
            // Permission was revoked between check and notify
        }
    }

    /**
     * Show notification for a new season
     */
    fun showNewSeasonNotification(
        context: Context,
        seasonId: Int,
        seasonName: String,
        startDate: String = "",
        endDate: String = ""
    ) {
        if (!hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "season")
            putExtra("season_id", seasonId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            seasonId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "📅 New Season Started!"
        val dateRange = if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
            "$startDate - $endDate"
        } else if (startDate.isNotEmpty()) {
            "Started: $startDate"
        } else {
            ""
        }
        val content = if (dateRange.isNotEmpty()) {
            "$seasonName\n$dateRange"
        } else {
            seasonName
        }

        val notification = NotificationCompat.Builder(context, SEASON_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText(seasonName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(SEASON_NOTIFICATION_ID_BASE + seasonId, notification)
        } catch (e: SecurityException) {
            // Permission was revoked between check and notify
        }
    }

    /**
     * Cancel a specific notification
     */
    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
