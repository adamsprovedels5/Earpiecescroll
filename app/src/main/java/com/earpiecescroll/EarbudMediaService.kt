package com.earpiecescroll

// ... [imports stay the same] ...

class EarbudMediaService : Service() {

    // ... [onCreate, notification setup stay the same] ...

    override fun onCreate() {
        super.onCreate()

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        mediaSession = MediaSession(this, "EarpieceScroll").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onMediaButtonEvent(mediaIntent: Intent): Boolean {
                    val event = mediaIntent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    if (event != null && event.repeatCount == 0) {
                        // Send to our classifier
                        ClickClassifier.onMediaKey(event.keyCode, event.action)
                        // RETURN TRUE to consume the event and prevent Google Assistant from launching!
                        return true 
                    }
                    return super.onMediaButtonEvent(mediaIntent)
                }
            })
            isActive = true
        }

        val component = ComponentName(this, MediaButtonReceiver::class.java)
        audioManager.registerMediaButtonEventReceiver(component)
    }

    // ... [rest of the service stays the same] ...
}
