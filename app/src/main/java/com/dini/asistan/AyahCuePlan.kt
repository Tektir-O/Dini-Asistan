package com.dini.asistan

/** A verified MP3 ayah joined into one compressed, page-specific Opus stream. */
data class AyahCue(val surah: Int, val ayah: Int, val fromMs: Int, val toMs: Int) {
    val label: String get() = "$surah:$ayah"
}
object AyahCuePlan {
    /** Strictly contiguous verse order and non-overlapping monotone timeline. */
    fun valid(cues: List<AyahCue>): Boolean {
        if (cues.isEmpty()) return false
        var lastEnd = 0
        var previous = 0
        for ((index, cue) in cues.withIndex()) {
            if (cue.surah !in 1..114 || cue.ayah !in 1..286 ||
                cue.fromMs < 0 || cue.toMs <= cue.fromMs || cue.toMs - cue.fromMs < 100 ||
                cue.fromMs < lastEnd || cue.fromMs - lastEnd > 3000) return false
            val key = cue.surah * 1000 + cue.ayah
            if (index > 0 && key <= previous) return false
            previous = key
            lastEnd = cue.toMs
        }
        return true
    }

    fun currentIndex(cues: List<AyahCue>, positionMs: Int): Int {
        if (cues.isEmpty()) return -1
        var lo = 0
        var hi = cues.size - 1
        while (lo <= hi) {
            val m = (lo + hi) ushr 1
            if (cues[m].toMs <= positionMs) lo = m + 1
            else hi = m - 1
        }
        return lo.coerceIn(0, cues.lastIndex)
    }
}
