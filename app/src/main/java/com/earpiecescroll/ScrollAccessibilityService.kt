package com.earpiecescroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.Path
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Main background service.
 *
 * Responsibilities:
 *
 * 1. Capture supported media-button events.
 * 2. Classify button interactions.
 * 3. Convert actions into Accessibility scrolling.
 * 4. Fall back to gesture scrolling when necessary.
 */
class ScrollAccessibilityService : AccessibilityService() {

    private lateinit var mediaSession: MediaSession

    override fun onServiceConnected() {

        super.onServiceConnected()

        instance = this

        /*
         * Connect the classifier to the scrolling engine.
         */
        ClickClassifier.onAction = ::performScroll

        /*
         * Start the media-button input path.
         */
        createMediaSession()
    }

    override fun onDestroy() {

        if (instance == this) {
            instance = null
        }

        ClickClassifier.onAction = null

        MediaButtonInputRouter.reset()

        if (::mediaSession.isInitialized) {

            mediaSession.isActive = false

            mediaSession.release()
        }

        super.onDestroy()
    }

    /**
     * Creates a MediaSession specifically for receiving standard
     * media-button events.
     */
    private fun createMediaSession() {

        val receiverComponent =
            ComponentName(
                this,
                MediaButtonReceiver::class.java
            )

        val receiverIntent =
            Intent(Intent.ACTION_MEDIA_BUTTON)
                .setComponent(receiverComponent)

        val receiverPendingIntent =
            PendingIntent.getBroadcast(
                this,
                1001,
                receiverIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        mediaSession =
            MediaSession(
                this,
                "EarpieceScroll"
            ).apply {

                /*
                 * Tell Android this session handles media buttons.
                 */
                setFlags(
                    MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                )

                setMediaButtonReceiver(
                    receiverPendingIntent
                )

                /*
                 * Direct MediaSession callback.
                 */
                setCallback(
                    object : MediaSession.Callback() {

                        override fun onMediaButtonEvent(
                            mediaIntent: Intent
                        ): Boolean {

                            val event =
                                mediaIntent.getParcelableExtra<KeyEvent>(
                                    Intent.EXTRA_KEY_EVENT
                                ) ?: return false

                            return MediaButtonInputRouter.dispatch(
                                event,
                                "MediaSession"
                            )
                        }
                    }
                )

                /*
                 * Advertise the media controls that the session supports.
                 */
                val state =
                    PlaybackState.Builder()
                        .setActions(
                            PlaybackState.ACTION_PLAY or
                                PlaybackState.ACTION_PAUSE or
                                PlaybackState.ACTION_PLAY_PAUSE or
                                PlaybackState.ACTION_SKIP_TO_NEXT or
                                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                                PlaybackState.ACTION_FAST_FORWARD or
                                PlaybackState.ACTION_REWIND or
                                PlaybackState.ACTION_STOP
                        )
                        .setState(
                            PlaybackState.STATE_PAUSED,
                            0L,
                            1.0f
                        )
                        .build()

                setPlaybackState(state)

                /*
                 * Activate the session.
                 */
                isActive = true
            }
    }

    /**
     * Second input path.
     *
     * AccessibilityService can request key-event filtering through
     * accessibility_service_config.xml.
     *
     * This allows Android to deliver supported hardware key events here
     * on devices where that path is available.
     */
    override fun onKeyEvent(
        event: KeyEvent
    ): Boolean {

        return MediaButtonInputRouter.dispatch(
            event,
            "Accessibility"
        )
    }

    /**
     * Executes the classified scroll command.
     */
    private fun performScroll(
        action: Int
    ) {

        val root =
            rootInActiveWindow

        val direction =
            when (action) {

                ClickClassifier.ACTION_SCROLL_DOWN,
                ClickClassifier.ACTION_PAGE_DOWN ->
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD

                ClickClassifier.ACTION_SCROLL_UP,
                ClickClassifier.ACTION_PAGE_UP ->
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD

                else ->
                    return
            }

        /*
         * First attempt:
         *
         * Ask the active application's Accessibility tree to scroll.
         */
        val scrollable =
            findBestScrollable(root)

        if (
            scrollable != null &&
            scrollable.performAction(direction)
        ) {

            return
        }

        /*
         * Second attempt:
         *
         * If the app does not expose a usable accessibility scroll node,
         * perform a physical swipe gesture.
         */
        dispatchSwipeGesture(action)
    }

    /**
     * Gesture fallback.
     */
    private fun dispatchSwipeGesture(
        action: Int
    ) {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.N
        ) {
            return
        }

        val metrics =
            resources.displayMetrics

        val width =
            metrics.widthPixels.toFloat()

        val height =
            metrics.heightPixels.toFloat()

        val isPageAction =
            action ==
                ClickClassifier.ACTION_PAGE_DOWN ||
            action ==
                ClickClassifier.ACTION_PAGE_UP

        /*
         * Page actions move farther than ordinary scrolling.
         */
        val distance =
            if (isPageAction) {
                0.72f
            } else {
                0.48f
            }

        val path =
            Path()

        when (action) {

            ClickClassifier.ACTION_SCROLL_DOWN,
            ClickClassifier.ACTION_PAGE_DOWN -> {

                val startY =
                    height * 0.82f

                val endY =
                    height *
                        (
                            startY / height -
                                distance
                            ).coerceAtLeast(0.08f)

                path.moveTo(
                    width * 0.50f,
                    startY
                )

                path.lineTo(
                    width * 0.50f,
                    endY
                )
            }

            ClickClassifier.ACTION_SCROLL_UP,
            ClickClassifier.ACTION_PAGE_UP -> {

                val startY =
                    height * 0.18f

                val endY =
                    height *
                        (
                            startY / height +
                                distance
                            ).coerceAtMost(0.92f)

                path.moveTo(
                    width * 0.50f,
                    startY
                )

                path.lineTo(
                    width * 0.50f,
                    endY
                )
            }

            else -> return
        }

        val stroke =
            GestureDescription.StrokeDescription(
                path,
                0L,
                260L
            )

        val gesture =
            GestureDescription.Builder()
                .addStroke(stroke)
                .build()

        dispatchGesture(
            gesture,
            null,
            null
        )
    }

    /**
     * Finds the most useful scrollable node in the current window.
     *
     * Instead of taking the first scrollable node encountered, candidates
     * are ranked by visibility, focus and child count.
     */
    private fun findBestScrollable(
        root: AccessibilityNodeInfo?
    ): AccessibilityNodeInfo? {

        if (root == null) {
            return null
        }

        val candidates =
            mutableListOf<AccessibilityNodeInfo>()

        collectScrollable(
            root,
            candidates
        )

        return candidates
            .sortedWith(
                compareByDescending<AccessibilityNodeInfo> {
                    it.isVisibleToUser
                }
                    .thenByDescending {
                        it.isFocused
                    }
                    .thenByDescending {
                        it.childCount
                    }
            )
            .firstOrNull()
    }

    private fun collectScrollable(
        node: AccessibilityNodeInfo,
        output: MutableList<AccessibilityNodeInfo>
    ) {

        if (
            node.isScrollable &&
            node.isVisibleToUser
        ) {
            output += node
        }

        for (
            index in 0 until node.childCount
        ) {

            node.getChild(index)
                ?.let { child ->

                    collectScrollable(
                        child,
                        output
                    )
                }
        }
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
        // No event processing currently required.
    }

    override fun onInterrupt() {
        // Required by AccessibilityService.
    }

    companion object {

        var instance:
            ScrollAccessibilityService? = null
            private set
    }
}
