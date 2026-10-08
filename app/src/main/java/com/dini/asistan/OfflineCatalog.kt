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

    private val ayahPageIndex: JSONObject by lazy {
        try {
            val json = context.assets.open("quran/ayah-page-index.json").bufferedReader().use { it.readText() }
            JSONObject(json).optJSONObject("pages") ?: JSONObject()
        } catch (_: IOException) { JSONObject() }
    }

    fun ayahAudioPath(page: Int): String = "audio/pages/${three(page)}.opus"

    fun ayahCues(page: Int): List<AyahCue> {
        if (page !in 1..604) return emptyList()
        val arr=ayahPageIndex.optJSONArray(page.toString()) ?: return emptyList()
        val result=ArrayList<AyahCue>(arr.length())
        try {
            for (i in 0 until arr.length()) {
                val node=arr.getJSONObject(i)
                result.add(AyahCue(node.getInt("surah"),node.getInt("ayah"),node.getInt("fromMs"),node.getInt("toMs")))
            }
        } catch (_: Exception) { return emptyList() }
        return if (AyahCuePlan.valid(result)) result else emptyList()
    }

    data class SurahPagePart(val page: Int, val fromMs: Int, val toMs: Int)

    fun surahPlan(surah: Int): List<SurahPagePart> {
        if(surah !in 1..114) return emptyList()
        val total= try { JSONObject(
            context.assets.open("quran/ayah-page-index.json").bufferedReader().use { it.readText() }
        ).optJSONObject("surahVerseCounts")?.optInt(surah.toString(),0) ?: 0 }
        catch (_:Exception) { 0 }
        if(total<=0)return emptyList()
        val clips=ArrayList<SurahPagePart>()
        var count=0
        for(page in 1..604){
            val all=ayahCues(page)
            val selected=all.filter{it.surah==surah}
            if(selected.isEmpty())continue
            if(selected.first().ayah!=count+1)return emptyList()
            count+=selected.size
            if(!hasAyahPage(page))return emptyList()
            clips.add(SurahPagePart(page,selected.first().fromMs,selected.last().toMs))
        }
        return if(count==total)clips else emptyList()
    }

    fun hasAyahPage(page: Int): Boolean = page in 1..604 && ayahCues(page).isNotEmpty() && try {
        context.assets.openFd(ayahAudioPath(page)).close()
        true
    } catch (_: IOException) { false }

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
