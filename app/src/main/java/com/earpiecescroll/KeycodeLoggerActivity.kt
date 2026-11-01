package com.earpiecescroll

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KeycodeLoggerActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var clearButton: Button

    private val logEntries =
        StringBuilder()

    private val timeFormat =
        SimpleDateFormat(
            "HH:mm:ss.SSS",
            Locale.US
        )

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_keycode_logger
        )

        logTextView =
            findViewById(
                R.id.logTextView
            )

        scrollView =
            findViewById(
                R.id.scrollView
            )

        clearButton =
            findViewById(
                R.id.clearButton
            )

        clearButton.setOnClickListener {

            logEntries.clear()

            logTextView.text = ""
        }

        /*
         * Receive diagnostics from the background input router.
         */
        MediaButtonInputRouter.onInputLog =
            { message ->

                runOnUiThread {

                    appendLog(
                        "[INPUT] $message"
                    )
                }
            }

        appendLog(
            "EARPIECE SCROLL INPUT DIAGNOSTICS"
        )

        appendLog(
            "────────────────────────────"
        )

        if (!isAccessibilityServiceEnabled()) {

            appendLog(
                "⚠ Accessibility Service is OFF."
            )

            appendLog(
                "Enable Earpiece Scroll in:"
            )

            appendLog(
                "Settings → Accessibility"
            )

            Toast.makeText(
                this,
                "Enable Earpiece Scroll Accessibility Service",
                Toast.LENGTH_LONG
            ).show()

            startActivity(
                Intent(
                    Settings.ACTION_ACCESSIBILITY_SETTINGS
                )
            )

        } else {

            appendLog(
                "✓ Accessibility Service is ON."
            )
        }

        appendLog(
            "✓ Diagnostic listener ready."
        )

        appendLog("")

        appendLog(
            "Supported media buttons:"
        )

        appendLog(
            "• PLAY / PAUSE"
        )

        appendLog(
            "• NEXT"
        )

        appendLog(
            "• PREVIOUS"
        )

        appendLog(
            "• FAST FORWARD"
        )

        appendLog(
            "• REWIND"
        )

        appendLog("")

        appendLog(
            "NOTE: Volume buttons are not treated as"
        )

        appendLog(
            "application-level media buttons."
        )

        appendLog("")

        appendLog(
            "Waiting for earpiece input..."
        )
    }

    private fun isAccessibilityServiceEnabled(): Boolean {

        val expected =
            ComponentName(
                this,
                ScrollAccessibilityService::class.java
            )

        val enabled =
            Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

        return enabled
            .split(':')
            .any {
                ComponentName
                    .unflattenFromString(it) == expected
            }
    }

    /**
     * Logs key events that actually reach the Activity.
     *
     * This is diagnostic only.
     *
     * Background media-button handling is performed by the
     * MediaSession / BroadcastReceiver / AccessibilityService paths.
     */
    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        logKeyEvent(event)

        return super.dispatchKeyEvent(event)
    }

    private fun logKeyEvent(
        event: KeyEvent
    ) {

        val action =
            when (event.action) {

                KeyEvent.ACTION_DOWN ->
                    "DOWN"

                KeyEvent.ACTION_UP ->
                    "UP"

                KeyEvent.ACTION_MULTIPLE ->
                    "MULTIPLE"

                else ->
                    "UNKNOWN(${event.action})"
            }

        val keyName =
            keyCodeToString(
                event.keyCode
            )

        val repeat =
            if (event.repeatCount > 0) {
                " repeat=${event.repeatCount}"
            } else {
                ""
            }

        val timestamp =
            timeFormat.format(
                Date()
            )

        val entry =
            "[$timestamp] $action " +
                "code=${event.keyCode} " +
                "name=$keyName$repeat " +
                "scan=${event.scanCode} " +
                "device=${event.deviceId} " +
                "flags=${event.flags}"

        appendLog(entry)
    }

    private fun appendLog(
        message: String
    ) {

        logEntries
            .append(message)
            .append("\n")

        logTextView.text =
            logEntries.toString()

        scrollView.post {

            scrollView.fullScroll(
                View.FOCUS_DOWN
            )
        }
    }

    private fun keyCodeToString(
        keyCode: Int
    ): String {

        return when (keyCode) {

            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ->
                "KEYCODE_MEDIA_PLAY_PAUSE"

            KeyEvent.KEYCODE_MEDIA_PLAY ->
                "KEYCODE_MEDIA_PLAY"

            KeyEvent.KEYCODE_MEDIA_PAUSE ->
                "KEYCODE_MEDIA_PAUSE"

            KeyEvent.KEYCODE_MEDIA_STOP ->
                "KEYCODE_MEDIA_STOP"

            KeyEvent.KEYCODE_MEDIA_NEXT ->
                "KEYCODE_MEDIA_NEXT"

            KeyEvent.KEYCODE_MEDIA_PREVIOUS ->
                "KEYCODE_MEDIA_PREVIOUS"

            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ->
                "KEYCODE_MEDIA_FAST_FORWARD"

            KeyEvent.KEYCODE_MEDIA_REWIND ->
                "KEYCODE_MEDIA_REWIND"

            KeyEvent.KEYCODE_HEADSETHOOK ->
                "KEYCODE_HEADSETHOOK"

            KeyEvent.KEYCODE_VOLUME_UP ->
                "KEYCODE_VOLUME_UP"

            KeyEvent.KEYCODE_VOLUME_DOWN ->
                "KEYCODE_VOLUME_DOWN"

            KeyEvent.KEYCODE_VOLUME_MUTE ->
                "KEYCODE_VOLUME_MUTE"

            KeyEvent.KEYCODE_CALL ->
                "KEYCODE_CALL"

            KeyEvent.KEYCODE_ENDCALL ->
                "KEYCODE_ENDCALL"

            KeyEvent.KEYCODE_VOICE_ASSIST ->
                "KEYCODE_VOICE_ASSIST"

            KeyEvent.KEYCODE_ASSIST ->
                "KEYCODE_ASSIST"

            else ->
                "KEYCODE_OTHER($keyCode)"
        }
    }

    override fun onDestroy() {

        MediaButtonInputRouter.onInputLog = null

        super.onDestroy()
    }
}
