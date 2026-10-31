package com.earpiecescroll

// ... [imports stay the same] ...

class ScrollAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        ClickClassifier.onAction = { action ->
            performScroll(action)
        }
    }

    // ... [onDestroy stays the same] ...

    private fun performScroll(action: Int) {
        val root = rootInActiveWindow
        val scrollable = findScrollable(root)

        if (scrollable != null) {
            val direction = when (action) {
                1, 3 -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD // Down / Page Down
                else -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD // Up / Page Up
            }
            val performed = scrollable.performAction(direction)
            if (performed) return
        }

        // If the app doesn't support Node scrolling, use a physical swipe
        dispatchSwipeGesture(action)
    }

    private fun dispatchSwipeGesture(action: Int) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()

        // Make the swipe longer for Page Up/Down (actions 3 and 4)
        val swipeHeight = if (action == 3 || action == 4) 0.8f else 0.5f 

        val path = Path().apply {
            if (action == 1 || action == 3) {
                // Swipe UP (Scroll Down)
                moveTo(width / 2f, height * 0.85f)
                lineTo(width / 2f, height * (0.85f - swipeHeight))
            } else {
                // Swipe DOWN (Scroll Up)
                moveTo(width / 2f, height * 0.15f)
                lineTo(width / 2f, height * (0.15f + swipeHeight))
            }
        }

        val stroke = GestureDescription.StrokeDescription(path, 0L, 250L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    // ... [findScrollable stays the same] ...
}
