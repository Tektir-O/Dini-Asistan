package com.dini.asistan

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper

/**
 * Purely offline page and surah recitation. Each page clip is assembled from
 * exact verse-separated audio recordings and independently checked timings.
 * Does not split ayahs that span two printed pages at made-up timestamps.
 */
class AyahPagePlayer(
    private val context: Context,
    private val catalog: OfflineCatalog,
    private val state: (String) -> Unit,
    private val progress: (Int,Int,String,Boolean) -> Unit,
    private val error: (String) -> Unit
) {
    private val main=Handler(Looper.getMainLooper())
    private var media:MediaPlayer?=null
    private var generation=0
    private var currentPart=0
    private var plan:List<OfflineCatalog.SurahPagePart> = emptyList()
    private var cues:List<AyahCue> = emptyList()
    private var paused=false
    private var prepared=false
    private var playing=false
    private var chapterMode=false
    private var targetChapter=0
    private var positionMs=0

    val currentPage: Int
        get() = if(!chapterMode) plan.getOrNull(currentPart)?.page?:0 else 0
    val isPlaying:Boolean get()=playing&&!paused
    val isReady:Boolean get()=media!=null&&prepared

    private val ticker=object:Runnable {
        override fun run() {
            val m=media?:return
            val part=plan.getOrNull(currentPart)?:return
            val pos=try{m.currentPosition}catch(_:IllegalStateException){return}
            positionMs=pos
            val cue=cues.getOrNull(AyahCuePlan.currentIndex(cues,pos))
            progress(pos.coerceAtLeast(0),part.toMs, cue?.label?:"",isPlaying)
            if(playing&&!paused&&pos>=part.toMs-110){
                nextPart()
                return
            }
            main.postDelayed(this,200)
        }
    }

    fun play(page:Int) {
        if(page !in 1..604||!catalog.hasAyahPage(page)){
            error("Bu sayfanın ayet ayet tilaveti henüz hazır değil.")
            return
        }
        if(!chapterMode && currentPage==page && isReady){
            if(paused) togglePause()
            return
        }
        stop()
        chapterMode=false
        plan=listOf(OfflineCatalog.SurahPagePart(page,0,catalog.ayahCues(page).last().toMs))
        startPart()
    }

    fun playSurah(surah:Int):Boolean {
        val parts=catalog.surahPlan(surah)
        if(parts.isEmpty()){
            error("Bu surenin ayet kayıtları eksik; oynatma başlatılamadı.")
            return false
        }
        stop()
        chapterMode=true
        targetChapter=surah
        plan=parts
        startPart()
        return true
    }

    private fun startPart() {
        val part=plan.getOrNull(currentPart)?:run{finish();return}
        releaseMedia()
        cues=catalog.ayahCues(part.page)
        generation++
        val request=generation
        try {
            val next=MediaPlayer()
            media=next
            context.assets.openFd(catalog.ayahAudioPath(part.page)).use {
                next.setDataSource(it.fileDescriptor,it.startOffset,it.length)
            }
            next.setOnPreparedListener { m ->
                if(request!=generation)return@setOnPreparedListener
                prepared=true
                if(part.fromMs>0) {
                    m.seekTo(part.fromMs,MediaPlayer.SEEK_CLOSEST)
                } else {
                    startPrepared(m,request)
                }
            }
            next.setOnSeekCompleteListener { m ->
                if(request==generation && prepared && !playing)startPrepared(m,request)
            }
            next.setOnCompletionListener{
                if(request==generation)nextPart()
            }
            next.setOnErrorListener{_,_,_->
                if(request==generation){stop();error("Tilavet ses dosyası okunamadı.")}
                true
            }
            state(if(chapterMode) "$targetChapter. sure hazırlanıyor" else "Sayfa tilaveti hazırlanıyor")
            next.prepareAsync()
        }catch(_:Exception){stop();error("Ayet ses kaydı açılamadı.")}
    }

    private fun startPrepared(m:MediaPlayer,request:Int){
        if(request!=generation)return
        try {
            m.start()
            playing=true
            paused=false
            state(if(chapterMode) "$targetChapter. sure ayet ayet okunuyor" else "Ayet ayet sayfa tilaveti")
            tick()
        }catch(_:Exception){stop();error("Tilavet başlatılamadı.")}
    }

    private fun nextPart() {
        if(plan.isEmpty())return
        main.removeCallbacks(ticker)
        currentPart++
        if(currentPart>=plan.size)finish()
        else startPart()
    }

    private fun finish() {
        val done=if(chapterMode) "$targetChapter. sure tamamlandı" else "Sayfanın tilaveti tamamlandı"
        stop()
        state(done)
    }

    fun togglePause() {
        val m=media?:return
        if(!prepared)return
        try{
            if(paused){
                m.start();paused=false;playing=true
                state("Tilavet devam ediyor")
            } else {
                m.pause();paused=true
                state("Duraklatıldı")
            }
            tick()
        }catch(_:Exception){stop();error("Duraklatma hatası")}
    }

    fun seekTo(ms:Int) {
        val m=media?:return
        val part=plan.getOrNull(currentPart)?:return
        if(!prepared)return
        val bounded=ms.coerceIn(part.fromMs,(part.toMs-1).coerceAtLeast(part.fromMs))
        try{
            // Seeking an existing stream must not restart playback.
            m.seekTo(bounded,MediaPlayer.SEEK_CLOSEST)
            positionMs=bounded
            val cue=cues.getOrNull(AyahCuePlan.currentIndex(cues,bounded))
            progress(bounded,part.toMs,cue?.label?:"",isPlaying)
        }catch(_:Exception){error("Süre ayarı başarısız")}
    }

    fun nextAyah()=skipVerse(1)
    fun previousAyah()=skipVerse(-1)

    private fun skipVerse(direction:Int) {
        if(!prepared||cues.isEmpty())return
        val pos=try{media?.currentPosition?:0}catch(_:Exception){0}
        val part=plan.getOrNull(currentPart)?:return
        val now=AyahCuePlan.currentIndex(cues,pos)
        if(now<0)return
        val idx=if(direction>0)(now+1).coerceAtMost(cues.lastIndex)
            else if(pos>cues[now].fromMs+2500)now else (now-1).coerceAtLeast(0)
        val within=cues.indexOfFirst{it.fromMs>=part.fromMs && it.toMs<=part.toMs && it==cues[idx]}
        if(within>=0)seekTo(cues[idx].fromMs)
        else if(direction>0 && currentPart+1<plan.size)nextPart()
    }

    private fun tick(){
        main.removeCallbacks(ticker)
        main.post(ticker)
    }
    private fun releaseMedia(){
        main.removeCallbacks(ticker)
        media?.let{try{it.reset()}catch(_:Exception){};it.release()}
        media=null
        prepared=false
        playing=false
        paused=false
    }
    fun stop(){
        generation++
        releaseMedia()
        plan=emptyList()
        cues=emptyList()
        currentPart=0
        chapterMode=false
        targetChapter=0
        positionMs=0
        progress(0,0,"",false)
    }
    fun release()=stop()
}
