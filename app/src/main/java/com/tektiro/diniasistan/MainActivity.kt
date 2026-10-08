package com.tektiro.diniasistan

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import kotlin.math.abs

class MainActivity : Activity() {

    data class Surah(val number: Int, val name: String, val page: Int)

    private val pageCount = 604
    private val prefs by lazy { getSharedPreferences("reader", MODE_PRIVATE) }
    private var surahs: List<Surah> = emptyList()
    private var currentPage = 1
    private var currentBitmap: Bitmap? = null
    private var isReaderOpen = false
    private var loadToken = 0

    private val paper = Color.rgb(247, 243, 232)
    private val ink = Color.rgb(30, 42, 34)
    private val accent = Color.rgb(31, 107, 79)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        surahs = loadSurahs()

        val restoreReader = savedInstanceState?.getBoolean("reader_open", false) ?: false
        val restorePage = savedInstanceState?.getInt("page", prefs.getInt("last_page", 1))
            ?: prefs.getInt("last_page", 1)

        if (restoreReader) {
            showReader(restorePage)
        } else {
            showHome()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("reader_open", isReaderOpen)
        outState.putInt("page", currentPage)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (isReaderOpen) {
            showHome()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        loadToken++
        currentBitmap?.recycle()
        currentBitmap = null
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun loadSurahs(): List<Surah> {
        return try {
            val json = assets.open("surahs.json").bufferedReader().use { it.readText() }
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        Surah(
                            number = item.getInt("number"),
                            name = item.getString("name"),
                            page = item.getInt("page")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun showHome() {
        isReaderOpen = false
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = paper
        window.navigationBarColor = paper

        val scroll = ScrollView(this).apply { setBackgroundColor(paper) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
        }

        column.addView(TextView(this).apply {
            text = "Dini Asistan"
            textSize = 14f
            setTextColor(accent)
        })

        column.addView(TextView(this).apply {
            text = "Kur'an-ı Kerim"
            textSize = 32f
            setTextColor(ink)
            setPadding(0, dp(6), 0, dp(8))
        })

        column.addView(TextView(this).apply {
            text = "604 sayfalık Medine Mushafı • tamamen çevrimdışı"
            textSize = 16f
            setTextColor(Color.rgb(95, 105, 98))
            setPadding(0, 0, 0, dp(24))
        })

        val last = prefs.getInt("last_page", 1).coerceIn(1, pageCount)
        column.addView(actionButton("Kaldığın yerden devam et — Sayfa " + last) { showReader(last) })
        column.addView(actionButton("Mushafı 1. sayfadan aç") { showReader(1) })
        column.addView(actionButton("Sayfa numarasına git") { showPageDialog() })
        column.addView(actionButton("Sureler") { showSurahDialog() })

        column.addView(TextView(this).apply {
            text = "Bu uygulama Kur'an sayfalarını cihazın içinden açar. Çalışmak için internet bağlantısı kullanmaz."
            textSize = 13f
            setTextColor(Color.rgb(110, 116, 111))
            setPadding(0, dp(28), 0, 0)
        })

        scroll.addView(column)
        setContentView(scroll)
    }

    private fun actionButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 16f
            setTextColor(ink)
            setOnClickListener { action() }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = dp(10)
            layoutParams = params
        }
    }

    private fun showReader(page: Int) {
        isReaderOpen = true
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.navigationBarColor = paper

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(paper)
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }

        val home = Button(this).apply {
            text = "⌂"
            textSize = 20f
            isAllCaps = false
            setOnClickListener { showHome() }
        }
        topBar.addView(home, LinearLayout.LayoutParams(dp(58), dp(48)))

        val pageTitle = TextView(this).apply {
            id = View.generateViewId()
            textSize = 17f
            setTextColor(ink)
            gravity = Gravity.CENTER
        }
        topBar.addView(pageTitle, LinearLayout.LayoutParams(0, dp(48), 1f))

        val jump = Button(this).apply {
            text = "Git"
            isAllCaps = false
            setOnClickListener { showPageDialog() }
        }
        topBar.addView(jump, LinearLayout.LayoutParams(dp(64), dp(48)))
        root.addView(topBar)

        val image = ImageView(this).apply {
            id = View.generateViewId()
            setBackgroundColor(Color.WHITE)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }
        root.addView(
            image,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(8))
        }

        val previous = Button(this).apply {
            text = "‹ Önceki"
            isAllCaps = false
            setOnClickListener { openPage(currentPage - 1, image, pageTitle) }
        }
        bottomBar.addView(previous, LinearLayout.LayoutParams(0, dp(50), 1f))

        val indicator = Button(this).apply {
            text = "Sayfa"
            isAllCaps = false
            setOnClickListener { showPageDialog() }
        }
        bottomBar.addView(indicator, LinearLayout.LayoutParams(0, dp(50), 1f))

        val next = Button(this).apply {
            text = "Sonraki ›"
            isAllCaps = false
            setOnClickListener { openPage(currentPage + 1, image, pageTitle) }
        }
        bottomBar.addView(next, LinearLayout.LayoutParams(0, dp(50), 1f))
        root.addView(bottomBar)

        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y
                if (abs(dx) > dp(70) && abs(dx) > abs(dy) && abs(velocityX) > 250) {
                    if (dx < 0) {
                        openPage(currentPage + 1, image, pageTitle)
                    } else {
                        openPage(currentPage - 1, image, pageTitle)
                    }
                    return true
                }
                return false
            }
        })
        image.setOnTouchListener { _, event -> detector.onTouchEvent(event) }

        setContentView(root)
        openPage(page, image, pageTitle)
    }

    private fun openPage(requested: Int, image: ImageView, pageTitle: TextView) {
        val page = requested.coerceIn(1, pageCount)
        currentPage = page
        prefs.edit().putInt("last_page", page).apply()
        pageTitle.text = "Sayfa " + page + " / " + pageCount

        val token = ++loadToken
        Thread {
            val bitmap = try {
                assets.open("mushaf/p" + page + ".png").use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
                null
            }

            runOnUiThread {
                if (token != loadToken || !isReaderOpen) {
                    bitmap?.recycle()
                    return@runOnUiThread
                }

                if (bitmap == null) {
                    AlertDialog.Builder(this)
                        .setTitle("Sayfa açılamadı")
                        .setMessage("Mushaf sayfası bulunamadı: " + page)
                        .setPositiveButton("Tamam", null)
                        .show()
                    return@runOnUiThread
                }

                val old = currentBitmap
                currentBitmap = bitmap
                image.setImageBitmap(bitmap)
                if (old != null && old !== bitmap && !old.isRecycled) {
                    old.recycle()
                }
            }
        }.start()
    }

    private fun showPageDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "1 - 604"
            setText(currentPage.coerceIn(1, pageCount).toString())
            selectAll()
        }

        AlertDialog.Builder(this)
            .setTitle("Sayfaya git")
            .setView(input)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Git") { _, _ ->
                val page = input.text.toString().toIntOrNull()
                if (page != null && page in 1..pageCount) {
                    showReader(page)
                }
            }
            .show()
    }

    private fun showSurahDialog() {
        if (surahs.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Sureler")
                .setMessage("Sure listesi yüklenemedi.")
                .setPositiveButton("Tamam", null)
                .show()
            return
        }

        val labels = surahs.map {
            it.number.toString() + ". " + it.name + " — s. " + it.page
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Sureler")
            .setItems(labels) { _, which ->
                showReader(surahs[which].page)
            }
            .setNegativeButton("Kapat", null)
            .show()
    }
}
