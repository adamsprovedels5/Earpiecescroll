package com.earpiecescroll

import android.os.SystemClock
import android.view.KeyEvent

/**
 * Central input router for supported earpiece media buttons.
 *
 * Android may expose the same physical button through more than one path:
 *
 * 1. MediaSession
 * 2. ACTION_MEDIA_BUTTON BroadcastReceiver
 * 3. AccessibilityService key-event filtering
 *
 * This router provides one entry point and prevents duplicate events
 * from triggering duplicate scroll actions.
 */
object MediaButtonInputRouter {

    private const val DUPLICATE_WINDOW_MS = 250L

    private var lastKeyCode = KeyEvent.KEYCODE_UNKNOWN
    private var lastAction = -1
    private var lastEventTime = 0L

    var onInputLog: ((String) -> Unit)? = null

    @Synchronized
    fun dispatch(
        event: KeyEvent,
        source: String
    ): Boolean {

        if (!ClickClassifier.isSupported(event.keyCode)) {
            return false
        }

        val now = SystemClock.elapsedRealtime()

        val duplicate =
            event.keyCode == lastKeyCode &&
            event.action == lastAction &&
            now - lastEventTime <= DUPLICATE_WINDOW_MS

        if (duplicate) {
            log(
                "DEDUP ${ClickClassifier.keyName(event.keyCode)} " +
                    "${actionName(event.action)} from $source"
            )

            return true
        }

        lastKeyCode = event.keyCode
        lastAction = event.action
        lastEventTime = now

        log(
            "INPUT ${ClickClassifier.keyName(event.keyCode)} " +
                "${actionName(event.action)} from $source"
        )

        return ClickClassifier.onMediaKey(
            event.keyCode,
            event.action,
            event.repeatCount
        )
    }

    fun reset() {
        lastKeyCode = KeyEvent.KEYCODE_UNKNOWN
        lastAction = -1
        lastEventTime = 0L

        ClickClassifier.reset()
    }

    private fun actionName(action: Int): String {
        return when (action) {
            KeyEvent.ACTION_DOWN -> "DOWN"
            KeyEvent.ACTION_UP -> "UP"
            KeyEvent.ACTION_MULTIPLE -> "MULTIPLE"
            else -> action.toString()
        }
    }

    private fun log(message: String) {
        onInputLog?.invoke(message)
    }
}
