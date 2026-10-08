package com.dini.asistan

/** A segment of a complete offline surah recording, cropped to a page boundary. */
data class PageAudioSlice(val surah: Int, val fromMs: Long, val toMs: Long) {
    init {
        require(surah in 1..114) { "Sure numarasi 1..114 olmali" }
        require(fromMs >= 0L && toMs > fromMs) { "Gecersiz ses araligi" }
        require(toMs <= Int.MAX_VALUE.toLong()) { "Android oynatici zaman siniri asildi" }
    }
}

object PageAudioPlan {
    fun valid(page: Int, slices: List<PageAudioSlice>): Boolean {
        if (page !in 1..604 || slices.isEmpty()) return false
        var lastSurah = 0
        var lastEnd = 0L
        for (slice in slices) {
            if (slice.surah < lastSurah) return false
            if (slice.surah == lastSurah && slice.fromMs < lastEnd) return false
            lastSurah = slice.surah
            lastEnd = slice.toMs
        }
        return true
    }
}
