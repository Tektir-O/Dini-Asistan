package com.dini.asistan

import org.junit.Assert.assertEquals
import org.junit.Test

class MushafNavigationTest {
    @Test fun leftSwipeMovesForward() {
        assertEquals(1, MushafNavigation.swipeDelta(-100f, 2f, 45f))
        assertEquals(55, MushafNavigation.move(54, 1))
    }

    @Test fun rightSwipeMovesBackwards() {
        assertEquals(-1, MushafNavigation.swipeDelta(100f, 2f, 45f))
        assertEquals(53, MushafNavigation.move(54, -1))
    }

    @Test fun shortOrVerticalMovementDoesNotTurnPages() {
        assertEquals(0, MushafNavigation.swipeDelta(-12f, 0f, 45f))
        assertEquals(0, MushafNavigation.swipeDelta(-80f, 140f, 45f))
        assertEquals(0, MushafNavigation.swipeDelta(Float.NaN, 0f, 45f))
    }

    @Test fun startsAtOneAndEndsAt604() {
        assertEquals(1, MushafNavigation.move(1, -1))
        assertEquals(604, MushafNavigation.move(604, 1))
        assertEquals(1, MushafNavigation.move(-200, -100))
        assertEquals(604, MushafNavigation.move(901, 10))
    }
}
