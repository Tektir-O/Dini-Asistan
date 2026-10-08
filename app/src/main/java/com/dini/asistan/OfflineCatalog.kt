package com.dini.asistan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

/**
 * All content is packaged with the APK. Never requests any remote URL.
 *
 * Expected files:
 *   mushaf/001.webp .. mushaf/604.webp  (verified 604-page print layout)
 *   audio/surah/001.opus .. audio/surah/114.opus
 *   quran/page-audio-index.json         (independently verified page time ranges)
 */
class OfflineCatalog(private val context: Context) {
    private val pageIndex: JSONObject by lazy {
        val text = context.assets.open("quran/page-audio-index.json").bufferedReader().use { it.readText() }
        JSONObject(text).optJSONObject("pages") ?: JSONObject()
    }

    private fun three(number: Int) = String.format(Locale.ROOT, "%03d", number)

    fun pageImagePath(page: Int): String = "mushaf/${three(page)}.webp"
    fun surahAudioPath(surah: Int): String = "audio/surah/${three(surah)}.opus"

    fun hasPageImage(page: Int): Boolean = page in 1..604 && try {
        context.assets.open(pageImagePath(page)).close()
        true
    } catch (_: IOException) {
        false
    }

    fun hasSurahAudio(surah: Int): Boolean = surah in 1..114 && try {
        context.assets.openFd(surahAudioPath(surah)).close()
        true
    } catch (_: IOException) {
        false
    }

    /** Load one page only. Keep memory usage low on low-end Android devices. */
    fun loadPageImage(page: Int, targetWidth: Int, targetHeight: Int): Bitmap? {
        if (page !in 1..604) return null
        return try {
            val filename = pageImagePath(page)
            val dimensions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(filename).use { BitmapFactory.decodeStream(it, null, dimensions) }
            var sample = 1
            while ((dimensions.outWidth / (sample * 2)) >= targetWidth &&
                   (dimensions.outHeight / (sample * 2)) >= targetHeight) sample *= 2
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            context.assets.open(filename).use { BitmapFactory.decodeStream(it, null, options) }
        } catch (_: IOException) {
            null
        }
    }

    fun pagePlan(page: Int): List<PageAudioSlice> {
        if (page !in 1..604) return emptyList()
        val array = pageIndex.optJSONArray(page.toString()) ?: return emptyList()
        val list = ArrayList<PageAudioSlice>(array.length())
        try {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                list.add(PageAudioSlice(
                    item.getInt("surah"),
                    item.getLong("fromMs"),
                    item.getLong("toMs")
                ))
            }
        } catch (_: Exception) {
            return emptyList()
        }
        if (!PageAudioPlan.valid(page, list)) return emptyList()
        if (!list.all { hasSurahAudio(it.surah) }) return emptyList()
        return list
    }
}
