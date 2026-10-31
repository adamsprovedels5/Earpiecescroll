package com.earpiecescroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ScrollAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        ClickClassifier.onAction = { action ->
            performScroll(action)
        }
    }

    override fun onDestroy() {
        if (instance == this) {
            instance = null
        }
        ClickClassifier.onAction = null
        super.onDestroy()
    }

    private fun performScroll(action: Int) {
        val root = rootInActiveWindow
        val scrollable = findScrollable(root)

        if (scrollable != null) {
            val direction = if (action == 1)
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            val performed = scrollable.performAction(direction)
            if (performed) return
        }

        dispatchSwipeGesture(action)
    }

    private fun dispatchSwipeGesture(action: Int) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()

        val path = Path().apply {
            if (action == 1) {
                moveTo(width / 2f, height * 0.75f)
                lineTo(width / 2f, height * 0.25f)
            } else {
                moveTo(width / 2f, height * 0.25f)
                lineTo(width / 2f, height * 0.75f)
            }
        }

        val stroke = GestureDescription.StrokeDescription(path, 0L, 300L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun findScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findScrollable(child)
            if (result != null) return result
        }
        return null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    companion object {
        var instance: ScrollAccessibilityService? = null
            private set
    }
}
