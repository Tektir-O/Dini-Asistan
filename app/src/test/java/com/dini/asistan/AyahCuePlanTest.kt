package com.dini.asistan

import org.junit.Assert.*
import org.junit.Test

class AyahCuePlanTest {
    @Test fun validAyahPlan() {
        val plan = (1..7).map { AyahCue(1, it, (it-1)*4000, it*4000) }
        assertTrue(AyahCuePlan.valid(plan))
        assertEquals("1:4", plan[AyahCuePlan.currentIndex(plan,12001)].label)
        assertEquals(0, AyahCuePlan.currentIndex(plan, 0))
    }

    @Test fun rejectsInvalidSequence() {
        assertFalse(AyahCuePlan.valid(emptyList()))
        assertFalse(AyahCuePlan.valid(listOf(AyahCue(1,1,0,1000), AyahCue(1,1,1000,2000))))
        assertFalse(AyahCuePlan.valid(listOf(AyahCue(1,1,0,1000), AyahCue(1,2,500,2000))))
        assertFalse(AyahCuePlan.valid(listOf(AyahCue(1,1,0,1000), AyahCue(1,2,6000,7000))))
        assertFalse(AyahCuePlan.valid(listOf(AyahCue(2,0,0,1000))))
    }

    @Test fun searchesLastAyahAtPageEnd() {
        val cues=listOf(AyahCue(2,6,0,2000),AyahCue(2,7,2000,10000))
        assertEquals(0,AyahCuePlan.currentIndex(cues,1999))
        assertEquals(1,AyahCuePlan.currentIndex(cues,2000))
        assertEquals(1,AyahCuePlan.currentIndex(cues,10000))
    }
}
