package com.dini.asistan

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Dini Asistan v0.1: native, offline-only shell.
 *
 * Home -> Categories -> Quran -> 604-page reader.
 * The reader refuses to falsely match a printed page or audio timestamps.
 * Assets are embedded at build time; NEVER loaded over the network.
 */
class MainActivity : Activity() {
    private enum class Screen { HOME, QURAN, SURAH, SETTINGS }

    private val background = Color.rgb(245, 247, 244)
    private val darkGreen = Color.rgb(11, 72, 64)
    private val green = Color.rgb(22, 118, 97)
    private val cream = Color.rgb(236, 237, 224)
    private val ink = Color.rgb(30, 45, 41)
    private val muted = Color.rgb(103, 116, 109)

    private lateinit var catalog: OfflineCatalog
    private lateinit var audio: PageAudioController
    private lateinit var surahAudio: SurahAudioController
    private var screen = Screen.HOME
    private var currentPage = 1
    private var currentSurah = 1
    private var playbackStatus = "Hazir"
    private var statusText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentSurah = (savedInstanceState?.getInt("surah") ?: getPreferences(MODE_PRIVATE).getInt("surah", 1)).coerceIn(1, 114)
        currentPage = savedInstanceState?.getInt("page")
            ?: getPreferences(MODE_PRIVATE).getInt("page", 1).coerceIn(1, 604)
        screen = when (savedInstanceState?.getString("screen")) {
            "QURAN" -> Screen.QURAN
            "SURAH" -> Screen.SURAH
            "SETTINGS" -> Screen.SETTINGS
            else -> Screen.HOME
        }
        catalog = OfflineCatalog(this)
        audio = PageAudioController(
            context = this,
            catalog = catalog,
            onState = { text ->
                playbackStatus = text
                runOnUiThread { statusText?.text = text }
            },
            onError = { text ->
                runOnUiThread {
                    statusText?.text = text
                    Toast.makeText(this, text, Toast.LENGTH_LONG).show()
                }
            }
        )
        surahAudio = SurahAudioController(this, catalog,
            onState = { msg ->
                playbackStatus = msg
                runOnUiThread { statusText?.text = msg }
            },
            onError = { msg ->
                runOnUiThread { statusText?.text = msg; Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
            }
        )
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("page", currentPage)
        outState.putInt("surah", currentSurah)
        outState.putString("screen", screen.name)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        audio.release()
        surahAudio.release()
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)

    private fun rounded(color: Int, radius: Int = 18): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun text(
        value: String,
        sp: Float = 16f,
        color: Int = ink,
        bold: Boolean = false,
        center: Boolean = false
    ): TextView = TextView(this).apply {
        text = value
        textSize = sp
        setTextColor(color)
        typeface = if (bold) Typeface.create("sans-serif-medium", Typeface.NORMAL)
                   else Typeface.create("sans-serif", Typeface.NORMAL)
        if (center) gravity = Gravity.CENTER
    }

    private fun sectionPadding(view: View, h: Int = 20, v: Int = 18) {
        view.setPadding(dp(h), dp(v), dp(h), dp(v))
    }

    private fun gap(parent: LinearLayout, height: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun row(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun column(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
    }

    private fun button(label: String, color: Int = green, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(color)
            setOnClickListener { action() }
        }

    private fun navigate(target: Screen) {
        if (screen == Screen.QURAN && target != Screen.QURAN) audio.stop()
        if (screen == Screen.SURAH && target != Screen.SURAH) surahAudio.stop()
        screen = target
        render()
    }

    private fun render() {
        statusText = null
        val root = column().apply { setBackgroundColor(this@MainActivity.background) }
        when (screen) {
            Screen.HOME -> root.addView(home(), LinearLayout.LayoutParams(-1, 0, 1f))
            Screen.QURAN -> root.addView(reader(), LinearLayout.LayoutParams(-1, 0, 1f))
            Screen.SURAH -> root.addView(surahReader(), LinearLayout.LayoutParams(-1, 0, 1f))
            Screen.SETTINGS -> root.addView(settings(), LinearLayout.LayoutParams(-1, 0, 1f))
        }
        root.addView(bottomNav(), LinearLayout.LayoutParams(-1, dp(72)))
        setContentView(root)
    }

    private fun bottomNav(): View {
        val bar = row().apply {
            setBackgroundColor(Color.WHITE)
            elevation = dp(8).toFloat()
        }
        val tabs = listOf(
            Triple(Screen.HOME, "⌂", "Ana Menü"),
            Triple(Screen.QURAN, "☷", "Kur’an"),
            Triple(Screen.SURAH, "♫", "Tilavet"),
            Triple(Screen.SETTINGS, "⚙", "Ayarlar")
        )
        for ((destination, glyph, label) in tabs) {
            val active = destination == screen
            val tab = column().apply {
                gravity = Gravity.CENTER
                setOnClickListener { navigate(destination) }
                contentDescription = label
                isClickable = true
                isFocusable = true
            }
            tab.addView(text(glyph, 24f, if (active) green else muted, center = true))
            tab.addView(text(label, 12f, if (active) green else muted, active, true))
            bar.addView(tab, LinearLayout.LayoutParams(0, -1, 1f))
        }
        return bar
    }

    private fun home(): View {
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = column()

        val hero = column().apply {
            setBackgroundColor(darkGreen)
            sectionPadding(this, 24, 30)
        }
        hero.addView(text("DİNİ ASİSTAN", 27f, Color.WHITE, true))
        gap(hero, 8)
        hero.addView(text("Kur’an-ı Kerim’i huzurla oku.", 17f, cream))
        gap(hero, 6)
        hero.addView(text("İnternet olmadan okuma ve tilavet", 13f, Color.rgb(207, 231, 218)))
        body.addView(hero, matchWrap())

        val categories = column().apply { sectionPadding(this, 20, 26) }
        categories.addView(text("Kategoriler", 23f, darkGreen, true))
        gap(categories, 17)

        val quranCard = column().apply {
            background = rounded(Color.WHITE)
            elevation = dp(2).toFloat()
            sectionPadding(this, 19, 20)
            isClickable = true
            isFocusable = true
            setOnClickListener { navigate(Screen.QURAN) }
        }
        quranCard.addView(text("۞  KUR’AN-I KERİM", 20f, darkGreen, true))
        gap(quranCard, 9)
        quranCard.addView(text("Mushaf sayfaları • Arapça tilavet", 15f, muted))
        gap(quranCard, 9)
        quranCard.addView(text("604 sayfalık okuyucuyu aç  ›", 14f, green, true))
        categories.addView(quranCard, matchWrap())
        gap(categories, 12)
        val surahCard = column().apply {
            background = rounded(Color.WHITE)
            elevation = dp(2).toFloat()
            sectionPadding(this, 19, 18)
            isClickable = true
            isFocusable = true
            setOnClickListener { navigate(Screen.SURAH) }
        }
        surahCard.addView(text("♫  ARAPÇA TİLAVET", 20f, darkGreen, true))
        gap(surahCard, 8)
        surahCard.addView(text("Mahir el-Muaykılî • 114 tam sure • Çevrimdışı", 14f, muted))
        gap(surahCard, 8)
        surahCard.addView(text("Sureleri dinle  ›", 14f, green, true))
        categories.addView(surahCard, matchWrap())
        gap(categories, 20)
        categories.addView(text("İçerik durumu", 19f, darkGreen, true))
        gap(categories, 8)
        categories.addView(text(
            "Mushaf sayfaları ve 114 tam sure APK içinde hazırlanıyor. Bir sayfayla sınırlı tilavet için doğru ses zamanları ayrıca kontrol edilmeli.",
            14f, muted
        ))
        body.addView(categories, matchWrap())

        scroll.addView(body)
        return scroll
    }

    private fun changePage(delta: Int) {
        val target = (currentPage + delta).coerceIn(1, 604)
        if (target == currentPage) return
        audio.stop()
        currentPage = target
        getPreferences(MODE_PRIVATE).edit().putInt("page", target).apply()
        render()
    }

    private fun changeSurah(delta: Int) {
        val target = (currentSurah + delta).coerceIn(1, 114)
        if (target == currentSurah) return
        surahAudio.stop()
        currentSurah = target
        getPreferences(MODE_PRIVATE).edit().putInt("surah", target).apply()
        render()
    }

    private fun surahReader(): View {
        val body = column()
        val top = column().apply {
            setBackgroundColor(darkGreen)
            sectionPadding(this, 20, 19)
        }
        top.addView(text("ARAPÇA TİLAVET", 21f, Color.WHITE, true))
        gap(top, 5)
        top.addView(text("Mahir el-Muaykılî (Hafs) • 114 tam sure", 13f, cream))
        body.addView(top, matchWrap())

        val main = column().apply { sectionPadding(this, 18, 30) }
        main.addView(text("♫", 58f, green, center = true))
        gap(main, 15)
        main.addView(text("Sure $currentSurah / 114", 23f, darkGreen, true, true))
        gap(main, 12)
        val state = if (catalog.hasSurahAudio(currentSurah))
            "Ses APK içinde • İnternet gerekmiyor"
        else "Bu surenin sesi henüz APK içinde değil"
        main.addView(text(state, 15f, muted, center = true))
        gap(main, 20)
        val pager = row()
        pager.addView(button("‹ Önceki sure", darkGreen) { changeSurah(-1) }.apply {
            isEnabled = currentSurah > 1
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        pager.addView(button("Sonraki sure ›", darkGreen) { changeSurah(1) }.apply {
            isEnabled = currentSurah < 114
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        main.addView(pager, matchWrap())
        gap(main, 12)
        val controls = row()
        controls.addView(button("▶ Başlat") {
            audio.stop()
            surahAudio.play(currentSurah)
        }, LinearLayout.LayoutParams(0, dp(55), 1f))
        controls.addView(button("Ⅱ Duraklat", Color.rgb(117, 120, 99)) {
            surahAudio.pauseOrResume()
        }, LinearLayout.LayoutParams(0, dp(55), 1f))
        controls.addView(button("■ Durdur", Color.rgb(145, 76, 64)) {
            surahAudio.stop()
        }, LinearLayout.LayoutParams(0, dp(55), 1f))
        main.addView(controls, matchWrap())
        gap(main, 12)
        val status = text(playbackStatus, 13f, muted, center = true)
        statusText = status
        main.addView(status, matchWrap())
        gap(main, 30)
        main.addView(text("Not: Sure dinleme bütün sureyi çalar. Mushafın yalnızca açık sayfasını okutan ayrı kontrol, doğrulanmış sayfa ses zamanları eklenince çalışacaktır.", 13f, muted))
        body.addView(main, matchWrap())
        return body
    }

    private fun reader(): View {
        val body = column()

        val top = column().apply {
            setBackgroundColor(darkGreen)
            sectionPadding(this, 20, 15)
        }
        top.addView(text("KUR’AN-I KERİM", 19f, Color.WHITE, true))
        gap(top, 4)
        top.addView(text("Medine Mushafı • Hafs", 13f, cream))
        body.addView(top, matchWrap())

        val pageLabel = text("Sayfa $currentPage / 604", 18f, darkGreen, true, true)
        pageLabel.setPadding(0, dp(12), 0, dp(9))
        body.addView(pageLabel, matchWrap())

        val paperFrame = FrameLayout(this).apply {
            background = rounded(Color.WHITE, 12)
            elevation = dp(2).toFloat()
        }
        val pageImage = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            contentDescription = "Mushaf sayfa $currentPage"
        }
        paperFrame.addView(pageImage, FrameLayout.LayoutParams(-1, -1))
        if (!catalog.hasPageImage(currentPage)) {
            val missing = text(
                "Sayfa $currentPage\n\nMushafın bu sayfası henüz\nAPK içeriğine eklenmedi.",
                17f, muted, center = true
            )
            paperFrame.addView(missing, FrameLayout.LayoutParams(-1, -1))
        } else {
            val selectedPage = currentPage
            pageImage.post {
                if (screen == Screen.QURAN && selectedPage == currentPage) {
                    val bitmap = catalog.loadPageImage(
                        selectedPage,
                        (paperFrame.width - dp(16)).coerceAtLeast(dp(220)),
                        (paperFrame.height - dp(16)).coerceAtLeast(dp(260))
                    )
                    if (bitmap != null) pageImage.setImageBitmap(bitmap)
                }
            }
        }
        val paperHolder = FrameLayout(this).apply {
            setPadding(dp(16), 0, dp(16), 0)
            addView(paperFrame, FrameLayout.LayoutParams(-1, -1))
        }
        body.addView(paperHolder, LinearLayout.LayoutParams(-1, 0, 1f))

        val controls = column().apply { sectionPadding(this, 12, 9) }

        val pager = row()
        val previous = button("‹  Geri Sayfa", darkGreen) { changePage(-1) }.apply {
            isEnabled = currentPage > 1
        }
        val next = button("İleri Sayfa  ›", darkGreen) { changePage(1) }.apply {
            isEnabled = currentPage < 604
        }
        pager.addView(previous, LinearLayout.LayoutParams(0, dp(49), 1f))
        pager.addView(next, LinearLayout.LayoutParams(0, dp(49), 1f))
        controls.addView(pager, matchWrap())

        val transport = row()
        transport.addView(button("▶ Başlat") {
            if (audio.playPage(currentPage)) playbackStatus = "Hazırlanıyor"
        }, LinearLayout.LayoutParams(0, dp(49), 1f))
        transport.addView(button("Ⅱ Duraklat", Color.rgb(117, 120, 99)) {
            audio.togglePause()
        }, LinearLayout.LayoutParams(0, dp(49), 1f))
        transport.addView(button("■ Durdur", Color.rgb(145, 76, 64)) {
            audio.stop()
        }, LinearLayout.LayoutParams(0, dp(49), 1f))
        controls.addView(transport, matchWrap())

        val status = text(playbackStatus, 12f, muted, center = true)
        status.setPadding(0, dp(5), 0, 0)
        statusText = status
        controls.addView(status, matchWrap())
        body.addView(controls, matchWrap())

        return body
    }

    private fun settings(): View {
        val scroll = ScrollView(this)
        val body = column().apply { sectionPadding(this, 22, 26) }
        body.addView(text("Ayarlar", 25f, darkGreen, true))
        gap(body, 18)
        body.addView(text("Dini Asistan • İlk Android sürümü", 16f, ink, true))
        gap(body, 10)
        body.addView(text("İnternet izni yok. Uygulama hiçbir sunucudan ses veya mushaf indirmez.", 15f, muted))
        gap(body, 16)
        body.addView(text("Mushaf: 604 matbu sayfa hedeflenir. Kaynak PDF’nin 640 sayfası ile basılı mushafın 604 sayfası henüz tek tek eşleştirilmedi.", 14f, muted))
        gap(body, 16)
        body.addView(text("Tilavet: Mahir el-Muaykılî (Hafs). Elimizdeki 114 sure kaydı için sayfaya özel, doğrulanmış zaman bilgileri oluşturuluyor.", 14f, muted))
        gap(body, 16)
        body.addView(text("Üçüncü taraf ses arşivinin yayın izni ve orijinal kaynak eşleşmesi doğrulanmadan genel dağıtım yapılmayacak.", 14f, muted))
        scroll.addView(body)
        return scroll
    }
}
