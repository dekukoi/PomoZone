package com.dekukoi.pomozone.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dekukoi.pomozone.R
import com.dekukoi.pomozone.domain.CompletionEvent
import com.dekukoi.pomozone.domain.TimerMode

private const val CHANNEL_ID = "pomozone_completion"
private const val NOTIFICATION_ID = 1001

/**
 * Shows a notification and vibrates when a block finishes.
 *
 * Channel creation is idempotent — safe to call on every app start.
 * On Android 13+ the caller must request POST_NOTIFICATIONS permission separately.
 */
class CompletionNotifier(private val context: Context) {

    init {
        ensureChannel()
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Block complete",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifies when a focus or break block finishes"
                vibrationPattern = longArrayOf(0, 300, 100, 300)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun notify(event: CompletionEvent) {
        val (title, body) = messageFor(event)
        vibrate()
        postNotification(title, body)
    }

    private fun messageFor(event: CompletionEvent): Pair<String, String> = when (event.finishedMode) {
        TimerMode.Focus -> when (event.nextMode) {
            TimerMode.ShortBreak -> "Focus complete" to "Time for a short break!"
            TimerMode.LongBreak  -> "Focus complete" to "Great work — take a long break!"
            else                 -> "Focus complete" to "Nice work!"
        }
        TimerMode.ShortBreak -> "Break over" to "Time to focus again."
        TimerMode.LongBreak  -> "Break over" to "Ready for the next session?"
    }

    private fun vibrate() {
        val pattern = longArrayOf(0, 300, 100, 300)
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }

    private fun postNotification(title: String, body: String) {
        // Guard: on API 33+ check permission at runtime before posting
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            if (granted != PackageManager.PERMISSION_GRANTED) return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
