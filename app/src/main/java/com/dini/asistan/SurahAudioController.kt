package com.dini.asistan

import android.content.Context
import android.media.MediaPlayer

/** Plays a complete embedded surah without any network permission. */
class SurahAudioController(
    private val context: Context,
    private val catalog: OfflineCatalog,
    private val onState: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var player: MediaPlayer? = null
    private var session = 0
    private var paused = false

    fun play(surah: Int): Boolean {
        stop()
        if (!catalog.hasSurahAudio(surah)) {
            onError("Bu surenin ses dosyasi APK icine henuz eklenmedi.")
            return false
        }
        session += 1
        val request = session
        return try {
            val media = MediaPlayer()
            player = media
            catalogAsset(surah, media)
            media.setOnPreparedListener {
                if (request == session) {
                    it.start()
                    paused = false
                    onState("$surah. surenin tilaveti oynatiliyor")
                }
            }
            media.setOnCompletionListener {
                if (request == session) {
                    stop()
                    onState("$surah. surenin tilaveti tamamlandi")
                }
            }
            media.setOnErrorListener { _, _, _ ->
                if (request == session) {
                    stop()
                    onError("Sure ses dosyasi oynatilamadi.")
                }
                true
            }
            media.prepareAsync()
            onState("$surah. surenin sesi hazirlaniyor")
            true
        } catch (ex: Exception) {
            stop()
            onError("Ses baslatilamadi: ${ex.javaClass.simpleName}")
            false
        }
    }

    private fun catalogAsset(surah: Int, player: MediaPlayer) {
        context.assets.openFd(catalog.surahAudioPath(surah)).use { fd ->
            player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        }
    }

    fun pauseOrResume() {
        val active = player ?: return
        try {
            if (paused) {
                active.start()
                paused = false
                onState("Tilavet devam ediyor")
            } else {
                active.pause()
                paused = true
                onState("Tilavet duraklatildi")
            }
        } catch (_: IllegalStateException) {
            stop()
            onError("Tilavet yeniden baslatilamadi.")
        }
    }

    fun stop() {
        session += 1
        val current = player
        player = null
        paused = false
        if (current != null) {
            try { current.reset() } catch (_: Exception) {}
            current.release()
        }
        onState("Durduruldu")
    }

    fun release() = stop()
}
