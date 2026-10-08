package com.dini.asistan

/**
 * Quran mushaf physical reading direction is right to left.
 * A swipe to the LEFT advances to the next printed page.
 * A swipe to the RIGHT goes back to the previous printed page.
 * Never page outside the canonical 1..604 range.
 */
object MushafNavigation {
    const val MIN_PAGE = 1
    const val MAX_PAGE = 604

    fun move(current: Int, delta: Int): Int =
        (current.coerceIn(MIN_PAGE, MAX_PAGE).toLong() + delta.toLong())
            .coerceIn(MIN_PAGE.toLong(), MAX_PAGE.toLong()).toInt()

    fun swipeDelta(dx: Float, dy: Float, thresholdPx: Float): Int {
        if (!dx.isFinite() || !dy.isFinite() || thresholdPx <= 0f) return 0
        if (kotlin.math.abs(dx) < thresholdPx ||
            kotlin.math.abs(dx) <= kotlin.math.abs(dy) * 1.3f) return 0
        return if (dx < 0f) 1 else -1
    }
}
