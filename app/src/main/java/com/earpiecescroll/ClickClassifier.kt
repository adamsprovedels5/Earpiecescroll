package com.earpiecescroll

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent

object ClickClassifier {

    private const val DEBOUNCE_MS = 350L
    private const val LONG_PRESS_MS = 600L

    private val clickTimes = mutableListOf<Long>()
    private var downTime = 0L

    // 1 = Scroll Down, 2 = Scroll Up, 3 = Page Down, 4 = Page Up
    var onAction: ((Int) -> Unit)? = null

    fun onMediaKey(keyCode: Int, action: Int) {
        val now = SystemClock.elapsedRealtime()

        // Volume buttons: instant, no classification needed
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && action == KeyEvent.ACTION_DOWN) {
            onAction?.invoke(1)
            return
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && action == KeyEvent.ACTION_DOWN) {
            onAction?.invoke(2)
            return
        }

        if (keyCode != KeyEvent.KEYCODE_HEADSETHOOK) return

        when (action) {
            KeyEvent.ACTION_DOWN -> {
                // Only record the first DOWN (ignore repeats)
                if (downTime == 0L) downTime = now
            }

            KeyEvent.ACTION_UP -> {
                if (downTime == 0L) return // Spurious UP with no DOWN

                val pressDuration = now - downTime
                downTime = 0L

                // Long press: measured on UP so we get the real duration
                if (pressDuration >= LONG_PRESS_MS) {
                    clickTimes.clear()
                    onAction?.invoke(3) // Page Down
                    return
                }

                // Short click: add to the debounce window
                clickTimes.add(now)
                clickTimes.removeAll { now - it > DEBOUNCE_MS }

                when (clickTimes.size) {
                    1 -> onAction?.invoke(1) // Single: Scroll Down
                    2 -> onAction?.invoke(2) // Double: Scroll Up
                    3 -> {                   // Triple: Page Up
                        clickTimes.clear()
                        onAction?.invoke(4)
                    }
                }
            }
        }
    }
}
