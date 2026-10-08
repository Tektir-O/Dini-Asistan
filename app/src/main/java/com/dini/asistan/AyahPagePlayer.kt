package com.dini.asistan

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

/**
 * Offline Maher Al-Muaiqly per-ayah player. Each page asset is assembled from
 * real independently recorded ayahs, never invented surah time cuts.
 * Stops exactly at the end of its page; does not automatically change pages.
 */
class AyahPagePlayer(
    private val context: Context,
    private val catalog: OfflineCatalog,
    private val state: (String) -> Unit,
    private val progress: (Int, Int, String, Boolean) -> Unit,
    private val error: (String) -> Unit
) {
    private val handler=Handler(Looper.getMainLooper())
    private var media: MediaPlayer?=null
    private var cues: List<AyahCue> = emptyList()
    private var page = 0
    private var generation = 0
    private var prepared = false
    private var playing = false
    private var wantedSeek = 0
    private var durationMs = 0
    private var paused = false

    val isPlaying: Boolean get() = playing && !paused
    val isReady: Boolean get() = media != null && prepared
    val currentPage: Int get() = page

    private val heartbeat=object:Runnable {
        override fun run() {
            val current=media ?: return
            val position=try { current.currentPosition } catch (_:IllegalStateException) { return }
            update(position)
            if (playing && !paused && position>=durationMs-120) {
                finish()
                return
            }
            handler.postDelayed(this, 180)
        }
    }

    private fun update(pos:Int) {
        val index=AyahCuePlan.currentIndex(cues, pos)
        val label=if(index>=0) cues[index].label else ""
        progress(pos.coerceAtLeast(0),durationMs,label,isPlaying)
    }

    fun play(pageNumber:Int) {
        if(pageNumber !in 1..604 || !catalog.hasAyahPage(pageNumber)) {
            error("Bu sayfanın ayet ayet tilavet dosyası henüz eklenmedi.")
            return
        }
        if(page==pageNumber && prepared) {
            if(paused) togglePause() else if(!playing) {
                try { media?.start();playing=true;paused=false;loop() }
                catch (_:Exception) { stop();error("Tilavet başlatılamadı") }
            }
            return
        }
        stop()
        page=pageNumber
        cues=catalog.ayahCues(pageNumber)
        durationMs=cues.last().toMs
        val id=++generation
        try {
            val player=MediaPlayer()
            media=player
            context.assets.openFd(catalog.ayahAudioPath(pageNumber)).use { a ->
                player.setDataSource(a.fileDescriptor,a.startOffset,a.length)
            }
            player.setOnPreparedListener {
                if(id!=generation) return@setOnPreparedListener
                prepared=true
                durationMs=kotlin.math.min(it.duration, cues.last().toMs).coerceAtLeast(1)
                if(wantedSeek>0) {
                    it.seekTo(wantedSeek.coerceIn(0,durationMs-1),MediaPlayer.SEEK_CLOSEST)
                }
                it.start()
                playing=true
                paused=false
                state("Ayet ayet tilavet başladı")
                loop()
            }
            player.setOnCompletionListener {
                if(id==generation) finish()
            }
            player.setOnErrorListener { _,_,_ ->
                if(id==generation) { stop();error("Sayfa tilaveti açılamadı.") }
                true
            }
            state("Sayfa tilaveti hazırlanıyor")
            player.prepareAsync()
        } catch (_:Exception) { stop();error("Ses dosyası yüklenemedi.") }
    }

    fun togglePause() {
        val m=media ?: return
        if(!prepared) return
        try {
            if(paused) {
                m.start();paused=false;playing=true;state("Tilavet devam ediyor");loop()
            } else {
                m.pause();paused=true;playing=true;state("Duraklatıldı");loop()
            }
        } catch (_:Exception) { stop();error("Oynatıcı hatası") }
    }

    fun seekTo(milliseconds: Int) {
        val m=media ?: return
        if(!prepared)return
        val position=milliseconds.coerceIn(0,(durationMs-1).coerceAtLeast(0))
        try {
            m.seekTo(position,MediaPlayer.SEEK_CLOSEST)
            update(position)
        } catch (_:Exception) { error("Süre çubuğunda atlama başarısız") }
    }

    fun nextAyah() = jumpAyah(1)
    fun previousAyah() = jumpAyah(-1)

    private fun jumpAyah(dir:Int) {
        if(!prepared || cues.isEmpty())return
        val current=try{media?.currentPosition?:0}catch(_:Exception){0}
        val idx=AyahCuePlan.currentIndex(cues,current)
        val next=if(dir>0) (idx+1).coerceAtMost(cues.lastIndex)
            else if(current > cues[idx].fromMs+2500) idx
            else (idx-1).coerceAtLeast(0)
        seekTo(cues[next].fromMs)
    }

    private fun loop() {
        handler.removeCallbacks(heartbeat)
        handler.post(heartbeat)
    }

    private fun finish() {
        stop()
        state("Bu sayfanın tilaveti tamamlandı")
    }

    fun stop() {
        generation++
        handler.removeCallbacks(heartbeat)
        media?.let { try{it.reset()}catch(_:Exception){};it.release() }
        media=null
        cues=emptyList()
        page=0
        wantedSeek=0
        durationMs=0
        prepared=false
        playing=false
        paused=false
        progress(0,0,"",false)
    }

    fun release()=stop()
}
