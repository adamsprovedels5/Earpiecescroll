package com.earpiecescroll

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent

class MediaButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MEDIA_BUTTON) return

        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return
        
        // Pass both the keycode and the action to the classifier
        if (event.repeatCount == 0) {
            ClickClassifier.onMediaKey(event.keyCode, event.action)
        }
    }
}
