package com.earpiecescroll

import android.os.SystemClock

object ClickClassifier {

    private const val DEBOUNCE_MS = 400L
    private const val PLUG_COOLDOWN_MS = 1000L

    private val clickTimes = mutableListOf<Long>()
    private var lastEventTime = 0L

    var onAction: ((Int) -> Unit)? = null

    fun onMediaKey(keyCode: Int) {
        val now = SystemClock.elapsedRealtime()

        if (now - lastEventTime < PLUG_COOLDOWN_MS && lastEventTime != 0L) {
            lastEventTime = now
            return
        }
        lastEventTime = now

        clickTimes.add(now)
        clickTimes.removeAll { now - it > DEBOUNCE_MS }

        val count = clickTimes.size
        when (count) {
            1 -> onAction?.invoke(1)
            2 -> onAction?.invoke(2)
            3 -> onAction?.invoke(3)
        }
    }

    fun reset() {
        clickTimes.clear()
        lastEventTime = 0L
    }
}
