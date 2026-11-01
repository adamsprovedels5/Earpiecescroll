package com.earpiecescroll

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent

/**
 * Converts supported earpiece media-button events into scroll commands.
 *
 * IMPORTANT:
 *
 * This class intentionally does NOT attempt to capture:
 *
 *     KEYCODE_VOLUME_UP
 *     KEYCODE_VOLUME_DOWN
 *
 * Android handles those through the system volume/audio pipeline and they
 * cannot be reliably hijacked by an ordinary application.
 *
 * Supported inputs are standard media-button events.
 */
object ClickClassifier {

    const val ACTION_SCROLL_DOWN = 1
    const val ACTION_SCROLL_UP = 2
    const val ACTION_PAGE_DOWN = 3
    const val ACTION_PAGE_UP = 4

    private const val HOLD_MS = 700L
    private const val DOUBLE_TAP_MS = 300L

    private val handler = Handler(Looper.getMainLooper())

    private val pressedKeys = mutableSetOf<Int>()

    private val holdRunnables =
        mutableMapOf<Int, Runnable>()

    private val pendingSingleRunnables =
        mutableMapOf<Int, Runnable>()

    private val tapCounts =
        mutableMapOf<Int, Int>()

    private val holdTriggeredKeys =
        mutableSetOf<Int>()

    var onAction: ((Int) -> Unit)? = null

    var onInputLog: ((String) -> Unit)? = null

    /**
     * Receives a media-button event after the central router has performed
     * duplicate filtering.
     */
    fun onMediaKey(
        keyCode: Int,
        action: Int,
        repeatCount: Int = 0
    ): Boolean {

        if (!isSupported(keyCode)) {
            return false
        }

        when (action) {

            KeyEvent.ACTION_DOWN -> {

                // Ignore repeated ACTION_DOWN events generated while
                // Android is holding the key.
                if (keyCode in pressedKeys) {
                    return true
                }

                pressedKeys += keyCode

                scheduleHold(keyCode)

                log(
                    "DOWN ${keyName(keyCode)}" +
                        if (repeatCount > 0) {
                            " repeat=$repeatCount"
                        } else {
                            ""
                        }
                )
            }

            KeyEvent.ACTION_UP -> {

                if (keyCode !in pressedKeys) {
                    return true
                }

                pressedKeys -= keyCode

                cancelHold(keyCode)

                log("UP ${keyName(keyCode)}")

                /*
                 * If the hold already fired, the release should NOT
                 * generate another click action.
                 */
                if (holdTriggeredKeys.remove(keyCode)) {
                    return true
                }

                registerTap(keyCode)
            }
        }

        return true
    }

    /**
     * Starts the long-press timer.
     */
    private fun scheduleHold(keyCode: Int) {

        cancelHold(keyCode)

        val runnable = Runnable {

            if (keyCode !in pressedKeys) {
                return@Runnable
            }

            /*
             * Cancel any pending single tap.
             */
            tapCounts[keyCode] = 0

            holdTriggeredKeys += keyCode

            log("HOLD ${keyName(keyCode)}")

            onAction?.invoke(
                holdActionFor(keyCode)
            )
        }

        holdRunnables[keyCode] = runnable

        handler.postDelayed(
            runnable,
            HOLD_MS
        )
    }

    private fun cancelHold(keyCode: Int) {

        holdRunnables
            .remove(keyCode)
            ?.let(handler::removeCallbacks)
    }

    /**
     * Registers a possible single/double tap.
     *
     * A single tap isn't executed immediately because we must wait briefly
     * to determine whether another tap follows.
     */
    private fun registerTap(keyCode: Int) {

        val nextCount =
            (tapCounts[keyCode] ?: 0) + 1

        tapCounts[keyCode] = nextCount

        pendingSingleRunnables
            .remove(keyCode)
            ?.let(handler::removeCallbacks)

        /*
         * Second tap = double tap.
         */
        if (nextCount >= 2) {

            tapCounts[keyCode] = 0

            log(
                "DOUBLE ${keyName(keyCode)}"
            )

            onAction?.invoke(
                doubleTapActionFor(keyCode)
            )

            return
        }

        /*
         * Wait briefly before treating the event as a single tap.
         */
        val runnable = Runnable {

            if ((tapCounts[keyCode] ?: 0) == 1) {

                tapCounts[keyCode] = 0

                log(
                    "SINGLE ${keyName(keyCode)}"
                )

                onAction?.invoke(
                    singleTapActionFor(keyCode)
                )
            }
        }

        pendingSingleRunnables[keyCode] = runnable

        handler.postDelayed(
            runnable,
            DOUBLE_TAP_MS
        )
    }

    /**
     * Single-tap mapping.
     */
    private fun singleTapActionFor(
        keyCode: Int
    ): Int {

        return when (keyCode) {

            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND ->
                ACTION_SCROLL_UP

            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ->
                ACTION_SCROLL_DOWN

            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ->
                ACTION_SCROLL_DOWN

            else ->
                ACTION_SCROLL_DOWN
        }
    }

    /**
     * Double-tap mapping.
     */
    private fun doubleTapActionFor(
        keyCode: Int
    ): Int {

        return when (keyCode) {

            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND ->
                ACTION_PAGE_UP

            else ->
                ACTION_PAGE_DOWN
        }
    }

    /**
     * Long-press mapping.
     */
    private fun holdActionFor(
        keyCode: Int
    ): Int {

        return when (keyCode) {

            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND ->
                ACTION_PAGE_UP

            else ->
                ACTION_PAGE_DOWN
        }
    }

    /**
     * Only standard media buttons are accepted.
     */
    fun isSupported(
        keyCode: Int
    ): Boolean {

        return when (keyCode) {

            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_REWIND -> true

            else -> false
        }
    }

    /**
     * Human-readable key names for diagnostics.
     */
    fun keyName(
        keyCode: Int
    ): String {

        return when (keyCode) {

            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ->
                "PLAY_PAUSE"

            KeyEvent.KEYCODE_MEDIA_PLAY ->
                "PLAY"

            KeyEvent.KEYCODE_MEDIA_PAUSE ->
                "PAUSE"

            KeyEvent.KEYCODE_MEDIA_STOP ->
                "STOP"

            KeyEvent.KEYCODE_MEDIA_NEXT ->
                "NEXT"

            KeyEvent.KEYCODE_MEDIA_PREVIOUS ->
                "PREVIOUS"

            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ->
                "FAST_FORWARD"

            KeyEvent.KEYCODE_MEDIA_REWIND ->
                "REWIND"

            else ->
                "KEY_$keyCode"
        }
    }

    private fun log(
        message: String
    ) {
        onInputLog?.invoke(message)
    }

    /**
     * Clears all pending input state.
     */
    fun reset() {

        holdRunnables
            .values
            .forEach(handler::removeCallbacks)

        pendingSingleRunnables
            .values
            .forEach(handler::removeCallbacks)

        holdRunnables.clear()
        pendingSingleRunnables.clear()

        pressedKeys.clear()
        tapCounts.clear()
        holdTriggeredKeys.clear()
    }
}
