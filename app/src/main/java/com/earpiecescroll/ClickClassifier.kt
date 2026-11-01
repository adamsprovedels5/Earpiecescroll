package com.earpiecescroll

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent

object ClickClassifier {

    private const val HOLD_TRIGGER_MS = 5000L

    private val handler = Handler(Looper.getMainLooper())
    private var holdRunnable: Runnable? = null
    private var isHolding = false

    // 1 = Scroll Down, 2 = Scroll Up, 3 = Page Down, 4 = Page Up
    var onAction: ((Int) -> Unit)? = null

    fun onMediaKey(keyCode: Int, action: Int) {
        // Only Volume Up is used as the trigger now
        if (keyCode != KeyEvent.KEYCODE_VOLUME_UP) return

        when (action) {
            KeyEvent.ACTION_DOWN -> {
                // Ignore repeated DOWN events fired while the key is already held
                if (isHolding) return
                isHolding = true

                val runnable = Runnable {
                    onAction?.invoke(2) // Scroll Up, fires once after 5s hold
                }
                holdRunnable = runnable
                handler.postDelayed(runnable, HOLD_TRIGGER_MS)
            }

            KeyEvent.ACTION_UP -> {
                isHolding = false
                // Released before reaching 5s — cancel the pending trigger
                holdRunnable?.let { handler.removeCallbacks(it) }
                holdRunnable = null
            }
        }
    }
}
