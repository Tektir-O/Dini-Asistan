package com.dini.asistan

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/** The green-and-gold Mushaf reader. All four download buttons are intentionally disabled. */
class PremiumMushafScreen(
    private val activity: Activity,
    private val page: Int,
    private val catalog: OfflineCatalog,
    private val player: AyahPagePlayer,
    private val onPage: (Int) -> Unit
) {
    private val dark=Color.rgb(2,53,43)
    private val deep=Color.rgb(0,71,56)
    private val lightGreen=Color.rgb(15,91,68)
    private val gold=Color.rgb(218,175,92)
    private val goldPale=Color.rgb(251,224,164)
    private val ivory=Color.rgb(251,245,230)
    private val ink=Color.rgb(24,74,57)
    private val density=activity.resources.displayMetrics.density
    private fun dp(n:Int)= (n*density+.5f).toInt()
    private fun lp(w:Int,h:Int)=LinearLayout.LayoutParams(w,h)
    private fun round(fill:Int,border:Int=gold,radius:Int=16,borderWidth:Int=1):GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(fill,fill,if(fill==dark||fill==deep) Color.rgb(0,44,39) else fill)).apply {
            cornerRadius=dp(radius).toFloat()
            setStroke(dp(borderWidth),border)
        }
    private fun text(label:String,sp:Float,color:Int,bold:Boolean=false,serif:Boolean=false):TextView=
        TextView(activity).apply {
            text=label
            textSize=sp
            setTextColor(color)
            gravity=Gravity.CENTER
            typeface=Typeface.create(if(serif) "serif" else "sans-serif",
                if(bold) Typeface.BOLD else Typeface.NORMAL)
            includeFontPadding=false
        }
    private fun col():LinearLayout=LinearLayout(activity).apply{orientation=LinearLayout.VERTICAL}
    private fun row():LinearLayout=LinearLayout(activity).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
    private fun space(h:Int)=View(activity).apply{layoutParams=lp(1,dp(h))}
    private fun darkButton(label:String,action:()->Unit):Button=Button(activity).apply{
        text=label
        textSize=14f
        isAllCaps=false
        setTextColor(goldPale)
        typeface=Typeface.create("serif",Typeface.BOLD)
        background=round(deep,gold,20,1)
        minHeight=0
        minWidth=0
        setPadding(dp(2),0,dp(2),0)
        setOnClickListener{action()}
    }
    private fun disabledButton(label:String):Button=darkButton(label){}.apply{
        isEnabled=false
        alpha=.46f
        contentDescription="$label; yakında, şu anda kullanılamıyor"
    }

    var status:TextView?=null
        private set
    var cueLabel:TextView?=null
        private set
    var elapsed:TextView?=null
        private set
    var remaining:TextView?=null
        private set
    var bar:SeekBar?=null
        private set
    var playButton:Button?=null
        private set
    var isSeeking=false
        private set

    fun setProgress(position:Int,duration:Int,verse:String,isPlaying:Boolean) {
        if(!isSeeking) bar?.apply{
            max=duration.coerceAtLeast(1)
            progress=position.coerceIn(0,max)
        }
        elapsed?.text=time(position)
        remaining?.text=time(duration)
        cueLabel?.text=if(verse.isNotEmpty()) "Okunan ayet: $verse" else "Ayet ayet Arapça tilavet"
        playButton?.text=if(isPlaying) "Ⅱ" else "▶"
    }
    fun setStatus(message:String){status?.text=message}
    private fun time(ms:Int):String{
        val all=ms.coerceAtLeast(0)/1000
        return "%02d:%02d".format(java.util.Locale.ROOT,all/60,all%60)
    }

    fun create():View {
        val root=col().apply{
            setBackgroundColor(ivory)
            layoutDirection=View.LAYOUT_DIRECTION_LTR
        }

        val header=FrameLayout(activity).apply{
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(dark,deep,dark))
        }
        header.addView(OrnateArch(activity,gold),FrameLayout.LayoutParams(-1,-1))
        val headline=col().apply{
            gravity=Gravity.CENTER
            setPadding(dp(12),dp(9),dp(12),dp(6))
        }
        headline.addView(text("☪  ۞  ☪",19f,goldPale,true,true),lp(-1,dp(23)))
        headline.addView(text("KUR’AN-I KERİM",26f,goldPale,true,true),lp(-1,dp(33)))
        headline.addView(text("Medine Mushafı • Hafs",15f,ivory,false,true),lp(-1,dp(23)))
        headline.addView(text("✦  ──────────  ◈  ──────────  ✦",12f,gold),lp(-1,dp(13)))
        header.addView(headline,FrameLayout.LayoutParams(-1,-1))
        root.addView(header,lp(-1,dp(102)))

        val surface=col().apply{
            background=round(ivory,gold,23,2)
            setPadding(dp(8),dp(5),dp(8),dp(6))
        }
        val indicator=row()
        indicator.addView(text("──── ◇",13f,gold),LinearLayout.LayoutParams(0,dp(34),1f))
        val pageLabel=text("Sayfa $page / 604",21f,dark,true,true).apply{
            background=round(ivory,gold,12,1)
            setPadding(dp(12),0,dp(12),0)
        }
        indicator.addView(pageLabel,lp(dp(194),dp(32)))
        indicator.addView(text("◇ ────",13f,gold),LinearLayout.LayoutParams(0,dp(34),1f))
        surface.addView(indicator,lp(-1,dp(35)))
        surface.addView(space(3))

        val holder=FrameLayout(activity).apply {
            background=round(Color.WHITE,gold,17,2)
            clipToOutline=true
        }
        val image=ImageView(activity).apply {
            scaleType=ImageView.ScaleType.FIT_CENTER
            adjustViewBounds=false
            setPadding(dp(3),dp(3),dp(3),dp(3))
            contentDescription="Mushaf sayfa $page"
        }
        var touchX=0f
        var touchY=0f
        image.setOnTouchListener { v,e ->
            when(e.actionMasked){
                MotionEvent.ACTION_DOWN->{touchX=e.x;touchY=e.y;true}
                MotionEvent.ACTION_UP->{
                    val next=MushafNavigation.swipeDelta(e.x-touchX,e.y-touchY,dp(45).toFloat())
                    if(next!=0) onPage(next) else v.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL->true
                else->true
            }
        }
        holder.addView(image,FrameLayout.LayoutParams(-1,-1))
        if(catalog.hasPageImage(page)){
            image.post {
                if(image.isAttachedToWindow) {
                    image.setImageBitmap(catalog.loadPageImage(page,
                        (image.width-dp(4)).coerceAtLeast(dp(200)),
                        (image.height-dp(4)).coerceAtLeast(dp(250))))
                }
            }
        } else {
            holder.addView(text("Mushaf görseli henüz eklenmedi",15f,ink),FrameLayout.LayoutParams(-1,-1))
        }
        surface.addView(holder,LinearLayout.LayoutParams(-1,0,1f))
        surface.addView(space(4))

        // Source image has direction reversed; the Mushaf requirement is LEFT = next.
        val pager=row()
        pager.addView(darkButton("‹  İleri Sayfa"){ onPage(1) }.apply{isEnabled=page<604},
            LinearLayout.LayoutParams(0,dp(34),1f))
        pager.addView(View(activity),lp(dp(8),dp(1)))
        pager.addView(darkButton("Geri Sayfa  ›"){onPage(-1)}.apply{isEnabled=page>1},
            LinearLayout.LayoutParams(0,dp(34),1f))
        surface.addView(pager,lp(-1,dp(43)))
        surface.addView(space(4))

        val playerPanel=col().apply {
            background=round(dark,gold,15,2)
            setPadding(dp(8),dp(4),dp(8),dp(3))
        }
        val timeline=row()
        timeline.addView(text("◖))",17f,goldPale),lp(dp(37),dp(27)))
        val seek=SeekBar(activity).apply {
            max=1
            progress=0
            progressTintList=ColorStateList.valueOf(goldPale)
            thumbTintList=ColorStateList.valueOf(gold)
            progressBackgroundTintList=ColorStateList.valueOf(Color.rgb(79,109,91))
            setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(b:SeekBar?,p:Int,fromUser:Boolean){
                    if(fromUser) elapsed?.text=time(p)
                }
                override fun onStartTrackingTouch(b:SeekBar?){isSeeking=true}
                override fun onStopTrackingTouch(b:SeekBar?){
                    isSeeking=false
                    if(b!=null) player.seekTo(b.progress)
                }
            })
        }
        bar=seek
        timeline.addView(seek,LinearLayout.LayoutParams(0,dp(29),1f))
        timeline.addView(text("☷",22f,goldPale),lp(dp(33),dp(29)))
        playerPanel.addView(timeline,lp(-1,dp(29)))

        val labels=row()
        elapsed=text("00:00",13f,ivory,true,true)
        remaining=text("00:00",13f,ivory,true,true)
        labels.addView(elapsed,LinearLayout.LayoutParams(0,dp(19),1f))
        val c=text("Ayet ayet Arapça tilavet",11f,goldPale)
        cueLabel=c
        labels.addView(c,LinearLayout.LayoutParams(0,dp(19),3f))
        labels.addView(remaining,LinearLayout.LayoutParams(0,dp(19),1f))
        playerPanel.addView(labels,lp(-1,dp(18)))

        val transport=row()
        val previous=darkButton("Ⅰ◀"){player.previousAyah()}
        val next=darkButton("▶Ⅰ"){player.nextAyah()}
        val toggle=darkButton("▶"){
            if(player.currentPage==page) player.togglePause()
            else player.play(page)
        }.apply{textSize=20f}
        playButton=toggle
        transport.addView(previous,LinearLayout.LayoutParams(0,dp(34),1f))
        transport.addView(toggle,LinearLayout.LayoutParams(0,dp(43),1f))
        transport.addView(next,LinearLayout.LayoutParams(0,dp(43),1f))
        playerPanel.addView(transport,lp(-1,dp(34)))
        val statusMessage=text("",11f,goldPale)
        status=statusMessage
        playerPanel.addView(statusMessage,lp(-1,dp(13)))
        surface.addView(playerPanel,lp(-1,dp(107)))
        surface.addView(space(4))

        val downloads=col()
        val first=row()
        first.addView(disabledButton("⇩  Mushaf İndir"),LinearLayout.LayoutParams(0,dp(34),1f))
        first.addView(View(activity),lp(dp(7),1))
        first.addView(disabledButton("⇩  Tilavet İndir"),LinearLayout.LayoutParams(0,dp(34),1f))
        downloads.addView(first,lp(-1,dp(34)))
        downloads.addView(space(4))
        val second=row()
        second.addView(disabledButton("⇩  Meal İndir"),LinearLayout.LayoutParams(0,dp(34),1f))
        second.addView(View(activity),lp(dp(7),1))
        second.addView(disabledButton("▣  Video Oluştur"),LinearLayout.LayoutParams(0,dp(34),1f))
        downloads.addView(second,lp(-1,dp(34)))
        surface.addView(downloads,lp(-1,dp(72)))

        root.addView(surface,LinearLayout.LayoutParams(-1,0,1f).apply{
            setMargins(dp(5),dp(3),dp(5),dp(3))
        })
        return root
    }
}

/** Gold double-outline pointed mosque arch on emerald background, vector drawn. */
private class OrnateArch(activity:Activity,private val gold:Int):View(activity) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style=Paint.Style.STROKE
        strokeWidth=2.5f
        color=gold
    }
    override fun onDraw(c:Canvas) {
        val w=width.toFloat()
        val h=height.toFloat()
        val d=resources.displayMetrics.density
        for (inset in listOf(0f, 9f*d)) {
            val path=Path()
            path.moveTo(inset,h)
            path.lineTo(inset,h*.57f)
            path.cubicTo(inset,h*.25f,w*.16f,h*.25f,w*.20f,h*.18f)
            path.cubicTo(w*.29f,h*.02f,w*.38f,h*.06f,w*.50f,h*.0f)
            path.cubicTo(w*.62f,h*.06f,w*.72f,h*.02f,w*.80f,h*.18f)
            path.cubicTo(w*.84f,h*.25f,w-inset,h*.25f,w-inset,h*.57f)
            path.lineTo(w-inset,h)
            c.drawPath(path,paint)
        }
        paint.style=Paint.Style.FILL
        paint.color=Color.argb(100,235,196,115)
        for(x in listOf(.08f,.92f)){
            c.drawCircle(w*x,h*.37f,2.5f*d,paint)
            c.drawLine(w*x,h*.0f,w*x,h*.34f,paint)
        }
        paint.style=Paint.Style.STROKE
        paint.color=gold
    }
}
