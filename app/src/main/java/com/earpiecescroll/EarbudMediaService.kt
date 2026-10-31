package com.earpiecescroll

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaSession
import android.os.Build
import android.os.IBinder
import android.view.KeyEvent
import androidx.core.app.NotificationCompat

class EarbudMediaService : Service() {

    private lateinit var mediaSession: MediaSession
    private lateinit var audioManager: AudioManager

    override fun onCreate() {
        super.onCreate()

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        mediaSession = MediaSession(this, "EarpieceScroll").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onMediaButtonEvent(mediaIntent: Intent): Boolean {
                    val event = mediaIntent.getParcelableExtra<KeyEvent>(
                        Intent.EXTRA_KEY_EVENT
                    )
                    if (event != null && event.action == KeyEvent.ACTION_DOWN) {
                        ClickClassifier.onMediaKey(event.keyCode)
                    }
                    return true
                }
            })
            isActive = true
        }

        val component = ComponentName(this, MediaButtonReceiver::class.java)
        audioManager.registerMediaButtonEventReceiver(component)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        try {
            audioManager.unregisterMediaButtonEventReceiver(
                ComponentName(this, MediaButtonReceiver::class.java)
            )
        } catch (_: Exception) {
        }
        mediaSession.isActive = false
        mediaSession.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "earpiece_scroll_channel"
        const val NOTIFICATION_ID = 1
    }
}
