package com.dini.asistan

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

/**
 * Offline page-only audio player.
 *
 * Does not auto-advance to another mushaf page. Every page's exact segment
 * boundaries must come from a verified alignment file. The audio player will
 * refuse to play when the timing index is missing.
 */
class PageAudioController(
    private val context: Context,
    private val catalog: OfflineCatalog,
    private val onState: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var segments: List<PageAudioSlice> = emptyList()
    private var segmentIndex = 0
    private var generation = 0
    private var paused = false

    private val tick = object : Runnable {
        override fun run() {
            val current = player ?: return
            val segment = segments.getOrNull(segmentIndex) ?: return
            if (!paused) {
                try {
                    if (current.currentPosition.toLong() >= segment.toMs) {
                        advance()
                        return
                    }
                } catch (_: IllegalStateException) {
                    onError("Ses oynatma konumu okunamadi.")
                    stop()
                    return
                }
            }
            mainHandler.postDelayed(this, 40)
        }
    }

    fun isActive() = player != null
    fun isPaused() = player != null && paused

    fun playPage(page: Int): Boolean {
        stop()
        val plan = catalog.pagePlan(page)
        if (plan.isEmpty()) {
            onError("Bu sayfa icin dogrulanmis ses sinirlari henuz eklenmedi.")
            return false
        }
        segments = plan
        segmentIndex = 0
        paused = false
        playCurrent()
        return true
    }

    fun togglePause() {
        val current = player ?: return
        try {
            if (paused) {
                paused = false
                current.start()
                mainHandler.removeCallbacks(tick)
                mainHandler.post(tick)
                onState("Tilavet devam ediyor")
            } else {
                current.pause()
                paused = true
                mainHandler.removeCallbacks(tick)
                onState("Tilavet duraklatildi")
            }
        } catch (_: IllegalStateException) {
            stop()
            onError("Ses oynaticisi yeniden baslatilamadi.")
        }
    }

    private fun playCurrent() {
        val clip = segments.getOrNull(segmentIndex) ?: run {
            stop()
            onState("Sayfa tilaveti tamamlandi")
            return
        }
        releasePlayer()
        generation += 1
        val requestGeneration = generation
        try {
            val afd = context.assets.openFd(catalog.surahAudioPath(clip.surah))
            val media = MediaPlayer()
            player = media
            afd.use { media.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            media.setOnErrorListener { _, _, _ ->
                onError("Sure ses dosyasi oynatilamadi.")
                stop()
                true
            }
            media.setOnPreparedListener {
                if (requestGeneration != generation) return@setOnPreparedListener
                media.seekTo(clip.fromMs, MediaPlayer.SEEK_CLOSEST)
            }
            media.setOnSeekCompleteListener {
                if (requestGeneration != generation) return@setOnSeekCompleteListener
                try {
                    media.start()
                    onState("Sayfa tilaveti oynatiliyor")
                    mainHandler.removeCallbacks(tick)
                    mainHandler.post(tick)
                } catch (_: IllegalStateException) {
                    stop()
                    onError("Ses baslatilamadi.")
                }
            }
            media.setOnCompletionListener {
                if (requestGeneration == generation) advance()
            }
            media.prepareAsync()
        } catch (e: Exception) {
            stop()
            onError("Ses dosyasi acilamadi: ${e.javaClass.simpleName}")
        }
    }

    private fun advance() {
        mainHandler.removeCallbacks(tick)
        segmentIndex++
        if (segmentIndex < segments.size) {
            playCurrent()
        } else {
            stop()
            onState("Bu sayfanin tilaveti bitti")
        }
    }

    private fun releasePlayer() {
        mainHandler.removeCallbacks(tick)
        player?.let {
            try { it.reset() } catch (_: Exception) {}
            it.release()
        }
        player = null
    }

    fun stop() {
        generation++
        releasePlayer()
        segments = emptyList()
        segmentIndex = 0
        paused = false
        onState("Durduruldu")
    }

    fun release() = stop()
}
