package com.earpiecescroll

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent

/**
 * Fallback receiver for Android ACTION_MEDIA_BUTTON broadcasts.
 *
 * This is one of several input paths used by the application.
 */
class MediaButtonReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (intent.action != Intent.ACTION_MEDIA_BUTTON) {
            return
        }

        val event =
            intent.getParcelableExtra<KeyEvent>(
                Intent.EXTRA_KEY_EVENT
            ) ?: return

        MediaButtonInputRouter.dispatch(
            event,
            "BroadcastReceiver"
        )
    }
}
