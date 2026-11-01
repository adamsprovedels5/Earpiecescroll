package com.earpiecescroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

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
        // DEBUG TOAST: We will see this if the muscle is working!
        val actionName = when(action) {
            1 -> "Scroll Down"
            2 -> "Scroll Up"
            3 -> "Page Down (Long Press)"
            else -> "Action $action"
        }
        Toast.makeText(this, "Triggered: $actionName", Toast.LENGTH_SHORT).show()

        val root = rootInActiveWindow
        val scrollable = findScrollable(root)

        if (scrollable != null) {
            val direction = when (action) {
                1, 3 -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                else -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }
            val performed = scrollable.performAction(direction)
            if (performed) return
        }

        dispatchSwipeGesture(action)
    }

    private fun dispatchSwipeGesture(action: Int) {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()

        val swipeHeight = if (action == 3 || action == 4) 0.8f else 0.5f 

        val path = Path().apply {
            if (action == 1 || action == 3) {
                moveTo(width / 2f, height * 0.85f)
                lineTo(width / 2f, height * (0.85f - swipeHeight))
            } else {
                moveTo(width / 2f, height * 0.15f)
                lineTo(width / 2f, height * (0.15f + swipeHeight))
            }
        }

        val stroke = GestureDescription.StrokeDescription(path, 0L, 250L)
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
