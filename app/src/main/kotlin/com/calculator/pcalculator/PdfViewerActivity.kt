package com.calculator.pcalculator

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class PdfViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ASSET_NAME = "asset_name"
        const val EXTRA_TITLE      = "pdf_title"

        // GitHub raw-তেও ব্রাউজারের মতো হেডার পাঠাই (নিরাপদে থাকতে)
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    private lateinit var vpPages        : ViewPager2
    private lateinit var rvThumbnails   : RecyclerView
    private lateinit var tvTitle        : TextView
    private lateinit var tvPageChip     : TextView
    private lateinit var seekPage       : SeekBar
    private lateinit var layoutToolbar  : View
    private lateinit var layoutBottomBar: View
    private lateinit var layoutLoading  : View
    private lateinit var btnShare       : ImageView

    private var pdfRenderer    : PdfRenderer?          = null
    private var pfd            : ParcelFileDescriptor? = null
    private var pageCount      = 0
    private var uiVisible      = true
    private var thumbAdapter   : ThumbnailAdapter? = null
    private var currentSource  : String = ""   // asset filename OR full server URL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        setContentView(R.layout.activity_pdf_viewer)

        vpPages         = findViewById(R.id.vpPdfPages)
        rvThumbnails    = findViewById(R.id.rvThumbnails)
        tvTitle         = findViewById(R.id.tvPdfTitle)
        tvPageChip      = findViewById(R.id.tvPageChip)
        seekPage        = findViewById(R.id.seekPage)
        layoutToolbar   = findViewById(R.id.layoutToolbar)
        layoutBottomBar = findViewById(R.id.layoutBottomBar)
        layoutLoading   = findViewById(R.id.layoutLoading)
        btnShare        = findViewById(R.id.btnShare)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnPrev).setOnClickListener {
            if (vpPages.currentItem > 0) vpPages.currentItem -= 1
        }
        findViewById<Button>(R.id.btnNext).setOnClickListener {
            if (vpPages.currentItem < pageCount - 1) vpPages.currentItem += 1
        }

        btnShare.setOnClickListener { sharePdf(currentSource) }

        val source = intent.getStringExtra(EXTRA_ASSET_NAME) ?: run {
            Toast.makeText(this, "ফাইল পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        tvTitle.text = intent.getStringExtra(EXTRA_TITLE) ?: source
        currentSource = source
        layoutLoading.visibility = View.VISIBLE
        loadPdf(source)
    }

    // ══════════════════════════════════════════════════════════════════════
    //  নতুন: GitHub blob লিংক → raw লিংক রূপান্তর
    // ══════════════════════════════════════════════════════════════════════
    /**
     * GitHub-এর "blob" লিংক আসলে একটা HTML ওয়েব পেজ — PDF ফাইল নয়।
     * Browser-এ খুললে GitHub ভিউয়ার দেখায় বলে কাজ করে, কিন্তু অ্যাপ HTML নামায়
     * বলে PDF রেন্ডার করতে পারে না। এই ফাংশন blob লিংককে সরাসরি ফাইলের
     * (raw.githubusercontent.com) লিংকে বদলে দেয়।
     *
     *   https://github.com/user/repo/blob/main/assets/pdfs/file.pdf
     *     → https://raw.githubusercontent.com/user/repo/main/assets/pdfs/file.pdf
     */
    private fun toDirectFileUrl(url: String): String {
        val regex = Regex("""github\.com/([^/]+/[^/]+)/blob/([^/]+)/(.+)""")
        val m = regex.find(url) ?: return url
        val repo   = m.groupValues[1]          // user/repo
        val branch = m.groupValues[2]          // main
        val path   = m.groupValues[3]          // assets/pdfs/file.pdf
        return "https://raw.githubusercontent.com/$repo/$branch/$path"
    }

    // ── Load PDF: from server URL (with local disk cache) or bundled asset ──
    private fun loadPdf(source: String) {
        // blob লিংক পাঠানো হলেও নিজে থেকেই raw-তে রূপান্তর হবে
        val url = toDirectFileUrl(source.trim())
        val isUrl = url.startsWith("http://", true) || url.startsWith("https://", true)
        val fileName = url.substringAfterLast("/").replace(" ", "_")
        val localFile = File(filesDir, fileName)

        if (isUrl) {
            downloadThenOpen(url, localFile)
        } else {
            // পুরনো পদ্ধতি: APK-এর assets ফোল্ডার থেকে
            try {
                assets.open(source).use { i -> FileOutputStream(localFile).use { o -> i.copyTo(o) } }
                openRenderer(localFile)
            } catch (e: Exception) {
                layoutLoading.visibility = View.GONE
                Toast.makeText(this, "PDF লোড করতে সমস্যা হয়েছে", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    // ── আসল PDF কিনা পরীক্ষা (%PDF- magic bytes) ─────────────────────────
    private fun isPdfContent(magic: ByteArray): Boolean {
        return magic.size >= 5 &&
            magic[0] == '%'.code.toByte() &&
            magic[1] == 'P'.code.toByte() &&
            magic[2] == 'D'.code.toByte() &&
            magic[3] == 'F'.code.toByte() &&
            magic[4] == '-'.code.toByte()
    }

    private fun isPdfFile(f: File): Boolean {
        return try {
            FileInputStream(f).use { i ->
                val magic = ByteArray(5)
                var read = 0
                while (read < 5) {
                    val r = i.read(magic, read, 5 - read)
                    if (r == -1) break
                    read += r
                }
                isPdfContent(magic)
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun openInBrowser(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, "ব্রাউজার পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * সরাসরি ফাইল ডাউনলোড (GitHub raw-তে anti-bot ch্যালেঞ্জ নেই — plain GET-ই যথেষ্ট)।
     * ১) HTTP code 200 কিনা দেখি
     * ২) Content-Type "text/html" হলে সাথে সাথে বাদ দিই (blob পেজ/এরর পেজ)
     * ৩) body-র প্রথম ৫ বাইট "%PDF-" কিনা দেখি — না হলে বাদ দিই
     * সব ঠিক থাকলে filesDir-এ ক্যাশ করে PdfRenderer-এ পাঠাই।
     * ভুল হলে ডায়ালগে আসল কারণ (HTTP code / Content-Type / প্রথম বাইট) দেখাই —
     * আর নীরবে ব্যর্থ হয় না।
     */
    private fun downloadThenOpen(url: String, destFile: File) {
        Thread {
            var downloadOk = false
            var detail: String? = null
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("Accept", "application/pdf, application/x-pdf, */*")
                }
                connection.connect()
                val code        = connection.responseCode
                val contentType = connection.contentType ?: ""

                if (code in 200..299 && !contentType.contains("text/html")) {
                    connection.inputStream.use { input ->
                        val magic = ByteArray(5)
                        var read = 0
                        while (read < 5) {
                            val r = input.read(magic, read, 5 - read)
                            if (r == -1) break
                            read += r
                        }
                        if (isPdfContent(magic)) {
                            val tmp = File(cacheDir, destFile.name + ".tmp")
                            FileOutputStream(tmp).use { out ->
                                out.write(magic, 0, read)
                                input.copyTo(out)
                            }
                            if (destFile.exists()) destFile.delete()
                            tmp.renameTo(destFile)
                            downloadOk = true
                        } else {
                            detail = "ডাউনলোড করা ফাইল PDF নয়! প্রথম বাইট: " +
                                magic.take(read).joinToString(" ") { "%02x".format(it) } +
                                " (blob পেজ/HTML ডাউনলোড হয়ে গেছে)"
                        }
                    }
                } else {
                    detail = "HTTP $code, Content-Type: ${contentType.ifBlank { "unknown" }}"
                }
                connection.disconnect()
            } catch (e: Exception) {
                detail = "${e.javaClass.simpleName}: ${e.message}"
            }

            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                when {
                    downloadOk -> openRenderer(destFile)

                    // ক্যাশ করা কপি শুধু তখনই দেখাব, যখন সেটা আসলেই PDF
                    destFile.exists() && isPdfFile(destFile) -> {
                        Toast.makeText(
                            this,
                            "নেটওয়ার্ক থেকে PDF পাওয়া যায়নি — আগের সেভ করা কপি দেখানো হচ্ছে",
                            Toast.LENGTH_LONG
                        ).show()
                        openRenderer(destFile)
                    }

                    else -> showErrorDialog(url, destFile, detail)
                }
            }
        }.start()
    }

    private fun showErrorDialog(url: String, destFile: File, detail: String?) {
        if (isFinishing) return
        layoutLoading.visibility = View.GONE
        android.app.AlertDialog.Builder(this)
            .setTitle("PDF লোড করা যাচ্ছে না")
            .setMessage(
                "সার্ভার থেকে PDF পাওয়া যাচ্ছে না।\n\n" +
                "টিপস: GitHub-এ ফাইলের নাম case-sensitive — নামের বানান/বড়-ছোট হরফ হুবহু মিলতে হবে।\n" +
                "ব্রাউজারে খুলে দেখুন PDF দেখা যায় কিনা।\n\n" +
                (detail?.let { "বিস্তারিত: $it\n\n" } ?: "") +
                "URL:\n$url"
            )
            .setCancelable(false)
            .setPositiveButton("আবার চেষ্টা করুন") { _, _ ->
                layoutLoading.visibility = View.VISIBLE
                downloadThenOpen(url, destFile)
            }
            .setNeutralButton("ব্রাউজারে খুলুন") { _, _ ->
                openInBrowser(url)
                finish()
            }
            .setNegativeButton("বন্ধ") { _, _ -> finish() }
            .show()
    }

    private fun openRenderer(file: File) {
        try {
            pfd         = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(pfd!!)
            pageCount   = pdfRenderer!!.pageCount
            layoutLoading.visibility = View.GONE
            setupViewPager()
            setupThumbnails()
            setupSeekBar()
            updatePageInfo(0)
        } catch (e: Exception) {
            layoutLoading.visibility = View.GONE
            Toast.makeText(this, "PDF ওপেন করতে সমস্যা হয়েছে", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // ── Share PDF ─────────────────────────────────────────────────────────
    private fun sharePdf(source: String) {
        if (source.isBlank()) {
            Toast.makeText(this, "শেয়ার করার ফাইল পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            return
        }
        // শেয়ারের সময়ও blob লিংক raw-তে রূপান্তর করে নিই
        val url          = toDirectFileUrl(source.trim())
        val fileName     = url.substringAfterLast("/").replace(" ", "_")
        val cachedFile   = File(filesDir, fileName)

        if (cachedFile.exists() && isPdfFile(cachedFile)) {
            shareLocalFile(cachedFile)
            return
        }

        val isUrl = url.startsWith("http://", true) || url.startsWith("https://", true)
        Thread {
            var ok = false
            try {
                if (isUrl) {
                    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 30000
                        instanceFollowRedirects = true
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", USER_AGENT)
                        setRequestProperty("Accept", "application/pdf, application/x-pdf, */*")
                    }
                    connection.connect()
                    val contentType = connection.contentType ?: ""
                    if (connection.responseCode in 200..299 && !contentType.contains("text/html")) {
                        connection.inputStream.use { input ->
                            val magic = ByteArray(5)
                            var read = 0
                            while (read < 5) {
                                val r = input.read(magic, read, 5 - read)
                                if (r == -1) break
                                read += r
                            }
                            if (isPdfContent(magic)) {
                                FileOutputStream(cachedFile).use { out ->
                                    out.write(magic, 0, read)
                                    input.copyTo(out)
                                }
                                ok = true
                            }
                        }
                    }
                    connection.disconnect()
                } else {
                    assets.open(source).use { i ->
                        FileOutputStream(cachedFile).use { o -> i.copyTo(o) }
                    }
                    ok = true
                }
                runOnUiThread {
                    if (isFinishing) return@runOnUiThread
                    if (ok) shareLocalFile(cachedFile)
                    else Toast.makeText(this, "শেয়ার করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    if (!isFinishing) Toast.makeText(this, "শেয়ার করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun shareLocalFile(file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, tvTitle.text.toString())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "শেয়ার করুন"))
        } catch (e: Exception) {
            Toast.makeText(this, "শেয়ার করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupViewPager() {
        vpPages.adapter = PdfPageAdapter(pdfRenderer!!, pageCount)
        vpPages.offscreenPageLimit = 1
        vpPages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageInfo(position)
                thumbAdapter?.setSelected(position)
                rvThumbnails.smoothScrollToPosition(position)
            }
        })
    }

    private fun setupThumbnails() {
        thumbAdapter = ThumbnailAdapter(pdfRenderer!!, pageCount) { pos ->
            vpPages.currentItem = pos
        }
        rvThumbnails.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvThumbnails.adapter = thumbAdapter
    }

    private fun setupSeekBar() {
        seekPage.max = maxOf(pageCount - 1, 1)
        seekPage.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                if (fromUser) vpPages.setCurrentItem(p, false)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
    }

    private fun updatePageInfo(pos: Int) {
        tvPageChip.text   = "${pos + 1} / $pageCount"
        seekPage.progress = pos
    }

    fun toggleUi() {
        uiVisible = !uiVisible
        val alpha    = if (uiVisible) 1f else 0f
        val duration = 200L
        layoutToolbar  .animate().alpha(alpha).setDuration(duration).start()
        layoutBottomBar.animate().alpha(alpha).setDuration(duration).start()
    }

    override fun onDestroy() {
        super.onDestroy()
        pdfRenderer?.close()
        pfd?.close()
    }

    // ── PDF Page Adapter ──────────────────────────────────────────────────
    inner class PdfPageAdapter(
        private val renderer : PdfRenderer,
        private val count    : Int
    ) : RecyclerView.Adapter<PdfPageAdapter.PageHolder>() {

        inner class PageHolder(view: View) : RecyclerView.ViewHolder(view) {
            val zoomView: ZoomableImageView = view.findViewById(R.id.ivPdfPage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PageHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pdf_page, parent, false))

        override fun getItemCount() = count

        override fun onBindViewHolder(holder: PageHolder, position: Int) {
            val page     = renderer.openPage(position)
            val screenW  = resources.displayMetrics.widthPixels
            val scale    = screenW.toFloat() / page.width.toFloat() * 2f
            val bmpW     = (page.width  * scale).toInt()
            val bmpH     = (page.height * scale).toInt()
            val bitmap   = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            holder.zoomView.setImageBitmap(bitmap)
            holder.zoomView.setOnTapCallback { toggleUi() }
        }
    }

    // ── Thumbnail Adapter ─────────────────────────────────────────────────
    inner class ThumbnailAdapter(
        private val renderer : PdfRenderer,
        private val count    : Int,
        private val onClick  : (Int) -> Unit
    ) : RecyclerView.Adapter<ThumbnailAdapter.ThumbHolder>() {

        private var selectedPos = 0

        inner class ThumbHolder(view: View) : RecyclerView.ViewHolder(view) {
            val iv: ImageView = view.findViewById(R.id.ivThumb)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ThumbHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pdf_thumbnail, parent, false))

        override fun getItemCount() = count

        override fun onBindViewHolder(holder: ThumbHolder, pos: Int) {
            val page  = renderer.openPage(pos)
            val bmp   = Bitmap.createBitmap(88, 120, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(android.graphics.Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            holder.iv.setImageBitmap(bmp)
            holder.iv.background = resources.getDrawable(
                if (pos == selectedPos) R.drawable.bg_thumbnail_selected
                else R.drawable.bg_thumbnail_normal, null)
            holder.itemView.setOnClickListener { onClick(pos) }
        }

        fun setSelected(pos: Int) {
            val old = selectedPos; selectedPos = pos
            notifyItemChanged(old); notifyItemChanged(pos)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// ZoomableImageView — Matrix-based, Google Photos style
// ══════════════════════════════════════════════════════════════════════════
class ZoomableImageView @JvmOverloads constructor(
    context: android.content.Context,
    attrs  : android.util.AttributeSet? = null
) : androidx.appcompat.widget.AppCompatImageView(context, attrs) {

    private val drawMatrix   = Matrix()
    private val baseMatrix   = Matrix()
    private val suppMatrix   = Matrix()
    private val displayRect  = android.graphics.RectF()
    private val matVals      = FloatArray(9)

    private val MIN_SCALE        = 1f
    private val MAX_SCALE        = 6f
    private val DOUBLE_TAP_SCALE = 2.8f

    private var tapCallback: (() -> Unit)? = null

    private val scroller = android.widget.OverScroller(context,
        android.view.animation.DecelerateInterpolator())
    private val flingRunnable = object : Runnable {
        override fun run() {
            if (scroller.computeScrollOffset()) {
                val newX = scroller.currX.toFloat()
                val newY = scroller.currY.toFloat()
                suppMatrix.postTranslate(newX - lastFlingX, newY - lastFlingY)
                lastFlingX = newX; lastFlingY = newY
                clampAndApply()
                postOnAnimation(this)
            }
        }
    }
    private var lastFlingX = 0f
    private var lastFlingY = 0f

    private val scaleDetector = ScaleGestureDetector(context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(d: ScaleGestureDetector): Boolean {
                scroller.forceFinished(true)
                return true
            }
            override fun onScale(d: ScaleGestureDetector): Boolean {
                val factor  = d.scaleFactor
                val current = currentScale()
                val clamped = when {
                    current * factor > MAX_SCALE -> MAX_SCALE / current
                    current * factor < MIN_SCALE -> MIN_SCALE / current
                    else -> factor
                }
                suppMatrix.postScale(clamped, clamped, d.focusX, d.focusY)
                clampAndApply()
                return true
            }
            override fun onScaleEnd(d: ScaleGestureDetector) {
                if (currentScale() < MIN_SCALE) animateToBase()
            }
        })

    private val gestureDetector = android.view.GestureDetector(context,
        object : android.view.GestureDetector.SimpleOnGestureListener() {

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val target = if (currentScale() > MIN_SCALE + 0.1f) MIN_SCALE
                             else DOUBLE_TAP_SCALE
                animateScaleTo(target, e.x, e.y)
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                tapCallback?.invoke()
                return true
            }

            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent,
                dX: Float, dY: Float
            ): Boolean {
                if (e2.pointerCount == 1 && !scaleDetector.isInProgress
                    && currentScale() > MIN_SCALE + 0.05f) {
                    suppMatrix.postTranslate(-dX, -dY)
                    clampAndApply()
                }
                return true
            }

            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent,
                vX: Float, vY: Float
            ): Boolean {
                if (currentScale() <= MIN_SCALE + 0.05f) return false
                val rect = getDisplayRect()
                lastFlingX = -suppMatrix.run { getValues(matVals); matVals[Matrix.MTRANS_X] }
                lastFlingY = -suppMatrix.run { getValues(matVals); matVals[Matrix.MTRANS_Y] }
                val minX = (width - rect.width()).toInt().coerceAtMost(0)
                val minY = (height - rect.height()).toInt().coerceAtMost(0)
                scroller.fling(
                    lastFlingX.toInt(), lastFlingY.toInt(),
                    vX.toInt(), vY.toInt(),
                    minX, 0, minY, 0, 50, 50)
                lastFlingX = scroller.startX.toFloat()
                lastFlingY = scroller.startY.toFloat()
                postOnAnimation(flingRunnable)
                return true
            }
        })

    init {
        scaleType = ScaleType.MATRIX
        addOnLayoutChangeListener { _, l, t, r, b, oL, oT, oR, oB ->
            if (l != oL || t != oT || r != oR || b != oB) resetToBase()
        }
    }

    fun setOnTapCallback(cb: () -> Unit) { tapCallback = cb }

    override fun setImageBitmap(bm: Bitmap?) {
        super.setImageBitmap(bm)
        post { resetToBase() }
    }

    private fun resetToBase() {
        val d = drawable ?: return
        if (width == 0 || height == 0) return
        val bW = d.intrinsicWidth.toFloat()
        val bH = d.intrinsicHeight.toFloat()
        val scale = minOf(width / bW, height / bH)
        baseMatrix.reset()
        baseMatrix.postScale(scale, scale)
        baseMatrix.postTranslate((width  - bW * scale) / 2f,
                                  (height - bH * scale) / 2f)
        suppMatrix.reset()
        applyMatrix()
    }

    private fun animateToBase() {
        android.animation.ValueAnimator.ofFloat(currentScale(), MIN_SCALE).apply {
            duration = 250
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener {
                val s   = it.animatedValue as Float
                val cur = currentScale()
                suppMatrix.postScale(s / cur, s / cur, width / 2f, height / 2f)
                clampAndApply()
            }
            start()
        }
    }

    private fun animateScaleTo(targetScale: Float, focusX: Float, focusY: Float) {
        android.animation.ValueAnimator.ofFloat(currentScale(), targetScale).apply {
            duration = 280
            interpolator = android.view.animation.DecelerateInterpolator(1.5f)
            addUpdateListener {
                val s   = it.animatedValue as Float
                val cur = currentScale()
                if (cur > 0f) {
                    suppMatrix.postScale(s / cur, s / cur, focusX, focusY)
                    clampAndApply()
                }
            }
            start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(e)
        gestureDetector.onTouchEvent(e)
        parent?.requestDisallowInterceptTouchEvent(
            currentScale() > MIN_SCALE + 0.05f || scaleDetector.isInProgress)
        return true
    }

    private fun applyMatrix() {
        drawMatrix.set(baseMatrix)
        drawMatrix.postConcat(suppMatrix)
        imageMatrix = drawMatrix
    }

    private fun clampAndApply() {
        val rect = getDisplayRect()
        val dw = rect.width(); val dh = rect.height()
        val vw = width.toFloat(); val vh = height.toFloat()
        val dx = when {
            dw <= vw -> (vw - dw) / 2f - rect.left
            rect.left > 0f  -> -rect.left
            rect.right < vw -> vw - rect.right
            else -> 0f
        }
        val dy = when {
            dh <= vh -> (vh - dh) / 2f - rect.top
            rect.top > 0f   -> -rect.top
            rect.bottom < vh -> vh - rect.bottom
            else -> 0f
        }
        if (dx != 0f || dy != 0f) suppMatrix.postTranslate(dx, dy)
        applyMatrix()
    }

    private fun getDisplayRect(): android.graphics.RectF {
        val d = drawable ?: return android.graphics.RectF()
        displayRect.set(0f, 0f,
            d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        drawMatrix.set(baseMatrix)
        drawMatrix.postConcat(suppMatrix)
        drawMatrix.mapRect(displayRect)
        return displayRect
    }

    private fun currentScale(): Float {
        suppMatrix.getValues(matVals)
        return matVals[Matrix.MSCALE_X]
    }
}
