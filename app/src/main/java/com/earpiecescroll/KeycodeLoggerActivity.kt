package com.earpiecescroll

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KeycodeLoggerActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var clearButton: Button
    private val logEntries = StringBuilder()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keycode_logger)

        logTextView = findViewById(R.id.logTextView)
        scrollView = findViewById(R.id.scrollView)
        clearButton = findViewById(R.id.clearButton)

        clearButton.setOnClickListener {
            logEntries.clear()
            logTextView.text = ""
        }

        appendLog(getString(R.string.waiting_for_events))
        appendLog("─────────────────────────")
        appendLog(getString(R.string.plug_in_hint))
        appendLog("")
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        logKeyEvent(event)
        return super.dispatchKeyEvent(event)
    }

    private fun logKeyEvent(event: KeyEvent) {
        val keyCode = event.keyCode
        val action = when (event.action) {
            KeyEvent.ACTION_DOWN -> "DOWN"
            KeyEvent.ACTION_UP -> "UP"
            KeyEvent.ACTION_MULTIPLE -> "MULTIPLE"
            else -> "UNKNOWN(${event.action})"
        }
        val keyName = keyCodeToString(keyCode)
        val repeat = if (event.repeatCount > 0) " repeat=${event.repeatCount}" else ""
        val timestamp = timeFormat.format(Date())
        val scanCode = event.scanCode
        val deviceId = event.deviceId
        val flags = event.flags

        val entry = "[$timestamp] $action  " +
                "code=$keyCode  name=$keyName$repeat  " +
                "scan=$scanCode  device=$deviceId  flags=$flags"

        appendLog(entry)
    }

    private fun appendLog(message: String) {
        logEntries.append(message).append("\n")
        logTextView.text = logEntries.toString()
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun keyCodeToString(keyCode: Int): String {
        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "KEYCODE_MEDIA_PLAY_PAUSE"
            KeyEvent.KEYCODE_MEDIA_PLAY -> "KEYCODE_MEDIA_PLAY"
            KeyEvent.KEYCODE_MEDIA_PAUSE -> "KEYCODE_MEDIA_PAUSE"
            KeyEvent.KEYCODE_MEDIA_STOP -> "KEYCODE_MEDIA_STOP"
            KeyEvent.KEYCODE_MEDIA_NEXT -> "KEYCODE_MEDIA_NEXT"
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "KEYCODE_MEDIA_PREVIOUS"
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> "KEYCODE_MEDIA_FAST_FORWARD"
            KeyEvent.KEYCODE_MEDIA_REWIND -> "KEYCODE_MEDIA_REWIND"
            KeyEvent.KEYCODE_HEADSETHOOK -> "KEYCODE_HEADSETHOOK"
            KeyEvent.KEYCODE_VOLUME_UP -> "KEYCODE_VOLUME_UP"
            KeyEvent.KEYCODE_VOLUME_DOWN -> "KEYCODE_VOLUME_DOWN"
            KeyEvent.KEYCODE_VOLUME_MUTE -> "KEYCODE_VOLUME_MUTE"
            KeyEvent.KEYCODE_CALL -> "KEYCODE_CALL"
            KeyEvent.KEYCODE_ENDCALL -> "KEYCODE_ENDCALL"
            KeyEvent.KEYCODE_VOICE_ASSIST -> "KEYCODE_VOICE_ASSIST"
            KeyEvent.KEYCODE_ASSIST -> "KEYCODE_ASSIST"
            KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK -> "KEYCODE_MEDIA_AUDIO_TRACK"
            KeyEvent.KEYCODE_MEDIA_CLOSE -> "KEYCODE_MEDIA_CLOSE"
            KeyEvent.KEYCODE_MEDIA_EJECT -> "KEYCODE_MEDIA_EJECT"
            KeyEvent.KEYCODE_MEDIA_RECORD -> "KEYCODE_MEDIA_RECORD"
            else -> "KEYCODE_UNKNOWN_OR_OTHER($keyCode)"
        }
    }
}
