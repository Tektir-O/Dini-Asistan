package com.dini.asistan

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageAudioPlanTest {
    @Test fun acceptsFullPageClip() {
        assertTrue(PageAudioPlan.valid(1, listOf(PageAudioSlice(1, 0, 51000))))
    }

    @Test fun acceptsPageCrossingSurahBoundary() {
        assertTrue(PageAudioPlan.valid(600, listOf(
            PageAudioSlice(112, 0, 9000),
            PageAudioSlice(113, 0, 16000)
        )))
    }

    @Test fun rejectsEmptyOrOverlappingOrUnorderedSegments() {
        assertFalse(PageAudioPlan.valid(1, emptyList()))
        assertFalse(PageAudioPlan.valid(605, listOf(PageAudioSlice(1, 0, 100))))
        assertFalse(PageAudioPlan.valid(8, listOf(
            PageAudioSlice(2, 500, 2000),
            PageAudioSlice(2, 1900, 3000)
        )))
        assertFalse(PageAudioPlan.valid(8, listOf(
            PageAudioSlice(5, 0, 1000),
            PageAudioSlice(4, 0, 1000)
        )))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsZeroLengthClip() { PageAudioSlice(1, 0, 0) }
}
