package com.earpiecescroll

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.Spinner
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
    private lateinit var serviceStatus: TextView

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

        setupControls()

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
            "• HEADSETHOOK (common single-button headset)"
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

        appendLog("Controls:")

        appendLog("• Single press: scroll")

        appendLog("• Double press: page")

        appendLog("• Hold for 0.7 seconds: page")

        appendLog("• Volume up: scroll up; volume down: scroll down")

        appendLog("")

        appendLog("Power button selection is blocked by Android.")

        appendLog("")

        appendLog(
            "Waiting for earpiece input..."
        )
    }

    private fun setupControls() {
        ScrollSettings.initialize(this)
        serviceStatus = findViewById(R.id.serviceStatus)
        findViewById<Button>(R.id.accessibilitySettingsButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        val actionNames = listOf(
            "Scroll down", "Scroll up", "Page down", "Page up", "Select focused item", "Back"
        )
        val actionValues = listOf(
            ClickClassifier.ACTION_SCROLL_DOWN,
            ClickClassifier.ACTION_SCROLL_UP,
            ClickClassifier.ACTION_PAGE_DOWN,
            ClickClassifier.ACTION_PAGE_UP,
            ClickClassifier.ACTION_SELECT,
            ClickClassifier.ACTION_BACK
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, actionNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        fun configureSpinner(id: Int, keyCode: Int, default: Int) {
            val spinner = findViewById<Spinner>(id)
            spinner.adapter = adapter
            val selected = ScrollSettings.singleAction(this, keyCode) ?: default
            spinner.setSelection(actionValues.indexOf(selected).coerceAtLeast(0))
            spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    ScrollSettings.setSingleAction(this@KeycodeLoggerActivity, keyCode, actionValues[position])
                }
            }
        }
        configureSpinner(R.id.volumeUpAction, KeyEvent.KEYCODE_VOLUME_UP, ClickClassifier.ACTION_SCROLL_UP)
        configureSpinner(R.id.volumeDownAction, KeyEvent.KEYCODE_VOLUME_DOWN, ClickClassifier.ACTION_SCROLL_DOWN)
        configureSpinner(R.id.headsetAction, KeyEvent.KEYCODE_HEADSETHOOK, ClickClassifier.ACTION_SELECT)

        findViewById<CheckBox>(R.id.volumeEnabled).apply {
            isChecked = ScrollSettings.volumeKeysEnabled(this@KeycodeLoggerActivity)
            setOnCheckedChangeListener { _, enabled ->
                ScrollSettings.setVolumeKeysEnabled(this@KeycodeLoggerActivity, enabled)
            }
        }
        findViewById<CheckBox>(R.id.useGestures).apply {
            isChecked = ScrollSettings.useGestures(this@KeycodeLoggerActivity)
            setOnCheckedChangeListener { _, enabled ->
                ScrollSettings.setUseGestures(this@KeycodeLoggerActivity, enabled)
            }
        }

        val distanceSeek = findViewById<SeekBar>(R.id.distanceSeek)
        val distanceLabel = findViewById<TextView>(R.id.distanceLabel)
        distanceSeek.progress = ScrollSettings.scrollDistance(this) - 20
        fun updateDistance(value: Int) {
            val distance = value + 20
            distanceLabel.text = "Scroll distance: $distance% of screen (page adds 24%)"
            ScrollSettings.setScrollDistance(this, distance)
        }
        updateDistance(distanceSeek.progress)
        distanceSeek.setOnSeekBarChangeListener(seekListener(::updateDistance))

        val speedSeek = findViewById<SeekBar>(R.id.speedSeek)
        val speedLabel = findViewById<TextView>(R.id.speedLabel)
        speedSeek.progress = ScrollSettings.gestureDuration(this) - 100
        fun updateSpeed(value: Int) {
            val duration = value + 100
            speedLabel.text = "Swipe duration: ${duration}ms"
            ScrollSettings.setGestureDuration(this, duration)
        }
        updateSpeed(speedSeek.progress)
        speedSeek.setOnSeekBarChangeListener(seekListener(::updateSpeed))

        val doubleSeek = findViewById<SeekBar>(R.id.doubleTapSeek)
        val holdSeek = findViewById<SeekBar>(R.id.holdSeek)
        val timingLabel = findViewById<TextView>(R.id.timingLabel)
        doubleSeek.progress = ScrollSettings.doubleTapMs(this) - 180
        holdSeek.progress = ScrollSettings.holdMs(this) - 350
        fun updateTiming() {
            val doubleMs = doubleSeek.progress + 180
            val holdMs = holdSeek.progress + 350
            timingLabel.text = "Double-tap window: ${doubleMs}ms · Hold threshold: ${holdMs}ms"
            ScrollSettings.setDoubleTapMs(this, doubleMs)
            ScrollSettings.setHoldMs(this, holdMs)
        }
        doubleSeek.setOnSeekBarChangeListener(seekListener { updateTiming() })
        holdSeek.setOnSeekBarChangeListener(seekListener { updateTiming() })
        updateTiming()
        refreshServiceStatus()
    }

    private fun seekListener(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = onChange(progress)
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }

    private fun refreshServiceStatus() {
        if (::serviceStatus.isInitialized) {
            serviceStatus.text = if (isAccessibilityServiceEnabled()) {
                "Accessibility service: ON"
            } else {
                "Accessibility service: OFF — enable it below to use controls outside this app"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::serviceStatus.isInitialized) refreshServiceStatus()
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

            KeyEvent.KEYCODE_HEADSETHOOK ->
                "KEYCODE_HEADSETHOOK"

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

            KeyEvent.KEYCODE_POWER ->
                "KEYCODE_POWER"

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
