package com.hfhub.android.ui

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hfhub.android.MainActivity
import com.hfhub.android.R
import com.hfhub.android.data.FileEntry
import com.hfhub.android.data.HfApi
import com.hfhub.android.data.RepoDetail
import com.hfhub.android.util.Avatar
import com.hfhub.android.util.AvatarLoader
import com.hfhub.android.util.badge
import com.hfhub.android.util.ImageLoader
import com.hfhub.android.util.Markdown
import com.hfhub.android.util.dp
import com.hfhub.android.util.fmtBytes
import com.hfhub.android.util.fmtDate
import com.hfhub.android.util.fmtNum
import com.hfhub.android.util.fmtParams
import com.hfhub.android.util.hbox
import com.hfhub.android.util.rounded
import com.hfhub.android.util.tv
import com.hfhub.android.util.vbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder


class DetailFragment : Fragment() {

    companion object {
        private const val ARG_KIND = "kind"
        private const val ARG_ID = "id"
        fun newInstance(kind: String, repoId: String): DetailFragment = DetailFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_KIND, kind)
                putString(ARG_ID, repoId)
            }
        }
    }

    private val kind get() = requireArguments().getString(ARG_KIND) ?: "models"
    private val repoId get() = requireArguments().getString(ARG_ID) ?: ""

    private var currentDir = ""            
    private lateinit var overviewBox: LinearLayout
    private lateinit var readmeBox: LinearLayout
    private lateinit var filesBox: LinearLayout
    private lateinit var tabChipIntro: Chip
    private lateinit var tabChipFiles: Chip
    private lateinit var introCard: MaterialCardView
    private lateinit var filesCard: MaterialCardView
    private lateinit var breadcrumb: TextView
    private lateinit var filesTitle: TextView
    private lateinit var status: TextView
    private lateinit var scroll: ScrollView
    private var rootFiles: List<FileEntry> = emptyList()

    
    fun handleBack(): Boolean {
        if (currentDir.isNotEmpty()) {
            currentDir = currentDir.substringBeforeLast('/', "")
            if (currentDir.isEmpty()) {
                renderFiles(rootFiles, loading = false)
            } else {
                renderFiles(emptyList(), loading = true)
                loadTree(currentDir)
            }
            return true
        }
        return false
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        val on = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, 0xFF1C1B1F.toInt())
        val dim = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF49454F.toInt())

        status = tv(ctx, "加载中…", 14f, dim).apply {
            gravity = Gravity.CENTER
            setPadding(0, ctx.dp(20f), 0, ctx.dp(20f))
        }

        
        val toolbar = com.google.android.material.appbar.MaterialToolbar(ctx).apply {
            setBackgroundColor(MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurface, 0))
            setNavigationIcon(R.drawable.ic_arrow_back)
            title = repoId
            inflateMenu(R.menu.detail_menu)
            setNavigationOnClickListener { if (!handleBack()) (activity as? MainActivity)?.closeSecondary() }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_copy -> { copyText(HfApi.webUrl(kind, repoId)); true }
                    R.id.action_web -> {
                        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(HfApi.webUrl(kind, repoId)))) }
                        catch (_: Exception) {}
                        true
                    }
                    else -> false
                }
            }
        }

        overviewBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        filesTitle = tv(ctx, "文件", 15f, on, true)
        breadcrumb = tv(ctx, "", 12f, dim, false, 2)
        filesBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        readmeBox = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        fun mkCard(): MaterialCardView = MaterialCardView(ctx).apply {
            radius = ctx.dp(16f).toFloat()
            cardElevation = 0f
            strokeWidth = ctx.dp(1f)
            setCardBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerLow, 0))
            strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant, 0)
        }

        
        tabChipIntro = Chip(ctx).apply { text = "简介"; isCheckable = true }
        tabChipFiles = Chip(ctx).apply { text = "文件"; isCheckable = true }
        val tabRow = ChipGroup(ctx).apply {
            isSingleSelection = true
            chipSpacingHorizontal = ctx.dp(8f)
            addView(tabChipIntro)
            addView(tabChipFiles)
        }
        tabChipIntro.setOnClickListener { showTab(intro = true) }
        tabChipFiles.setOnClickListener { showTab(intro = false) }
        val tabWrap = LinearLayout(ctx).apply { setPadding(0, ctx.dp(10f), 0, ctx.dp(10f)) }
        tabWrap.addView(tabRow)

        
        introCard = mkCard()
        val introInner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(14f), ctx.dp(12f), ctx.dp(14f), ctx.dp(14f))
        }
        introInner.addView(readmeBox)
        introCard.addView(introInner, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        introCard.visibility = View.GONE

        
        filesCard = mkCard()
        val filesInner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ctx.dp(14f), ctx.dp(12f), ctx.dp(14f), ctx.dp(12f))
        }
        filesInner.addView(filesTitle)
        filesInner.addView(breadcrumb)
        filesBox.setPadding(0, ctx.dp(2f), 0, 0)
        filesInner.addView(filesBox)
        filesCard.addView(filesInner, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        
        val content = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val pad = ctx.dp(18f)
        content.setPadding(pad, ctx.dp(4f), pad, ctx.dp(20f))
        content.addView(status, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        content.addView(overviewBox, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        content.addView(tabWrap, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        content.addView(filesCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        content.addView(introCard, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        showTab(intro = false)

        scroll = ScrollView(ctx)
        scroll.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root.setBackgroundColor(MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF.toInt()))
        root.addView(toolbar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        loadAll()
        return root
    }

    private fun loadAll() {
        status.visibility = View.VISIBLE
        status.text = "加载中…"
        status.setOnClickListener(null)
        lifecycleScope.launch {
            
            val d = HfApi.detailOrNull(kind, repoId)
            val readme = HfApi.readme(kind, repoId)
            
            var files = try { HfApi.tree(kind, repoId, "") } catch (e: Exception) { emptyList() }
            if (files.isEmpty()) {
                files = d?.siblings ?: emptyList()
            }
            rootFiles = files
            status.visibility = View.GONE
            renderOverview(d)
            renderFiles(files, loading = false)
            renderReadme(readme)
            if (d == null && files.isEmpty() && readme.isBlank()) {
                status.visibility = View.VISIBLE
                status.text = "此仓库加载失败（数据源可能不支持）\n可点右上角 🌐 在浏览器打开"
            }
        }
    }

    private fun loadTree(dir: String) {
        status.visibility = View.VISIBLE
        status.text = "加载目录…"
        lifecycleScope.launch {
            try {
                val entries = HfApi.tree(kind, repoId, dir)
                status.visibility = View.GONE
                renderFiles(entries, loading = false)
            } catch (e: Exception) {
                status.visibility = View.VISIBLE
                status.text = "目录加载失败：${e.message?.take(80)}\n(点击重试)"
                status.setOnClickListener { loadTree(dir) }
            }
        }
    }

    private fun renderOverview(d: RepoDetail?) {
        val ctx = requireContext()
        val on = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, 0)
        val dim = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        val primary = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorPrimary, 0)
        overviewBox.removeAllViews()

        if (d == null) {
            overviewBox.addView(tv(ctx, "ℹ️ 该数据源不支持详情接口，仅显示文件与 README", 13f, dim, false, 3).apply {
                setPadding(0, ctx.dp(8f), 0, ctx.dp(4f))
            })
            return
        }

        
        val card = MaterialCardView(ctx).apply {
            radius = ctx.dp(16f).toFloat()
            cardElevation = 0f
            strokeWidth = ctx.dp(1f)
            setCardBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerLow, 0))
            strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant, 0)
        }

        val avatar = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ctx.dp(44f), ctx.dp(44f)).apply { rightMargin = ctx.dp(10f) }
            setImageDrawable(Avatar.drawable(ctx, d.id.ifBlank { repoId }))
        }
        AvatarLoader.load(Avatar.authorOf(d.id.ifBlank { repoId }), avatar)
        val name = tv(ctx, repoId, 16f, on, true, 2)
        val metaBits = ArrayList<String>()
        metaBits.add("${fmtNum(d.downloads)} 下载")
        metaBits.add("${fmtNum(d.likes)} 喜欢")
        if (!d.lastModified.isNullOrBlank()) metaBits.add("更新 ${fmtDate(d.lastModified)}")
        if (d.usedStorage > 0) metaBits.add(fmtBytes(d.usedStorage))
        val params = d.safetensors?.total ?: 0
        if (params > 0) metaBits.add("${fmtParams(params)} 参数")
        metaBits.add("${d.siblings.size} 文件")
        val meta = tv(ctx, metaBits.joinToString("  ·  "), 12f, dim, false, 2).apply {
            setPadding(0, ctx.dp(3f), 0, 0)
        }

        val likeBtn = tv(ctx, "❤ ${fmtNum(d.likes)}", 12f, primary, true).apply {
            val p = ctx.dp(12f)
            setPadding(p, ctx.dp(7f), p, ctx.dp(7f))
            background = rounded(ctx.dp(18f), MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer, 0))
            setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(HfApi.webUrl(kind, repoId) + "?like=1")))
                } catch (_: Exception) {}
            }
        }

        val nameCol = vbox(ctx, name, meta)
        nameCol.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val headRow = hbox(ctx, avatar, nameCol, likeBtn)

        val badgeRow = hbox(ctx)
        badgeRow.setPadding(0, ctx.dp(8f), 0, 0)
        if (d.gated) badgeRow.addView(badge(ctx, "🔒 需授权", 0x22FFB300, 0xFFB06000.toInt()))
        else badgeRow.addView(badge(ctx, "公开", MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSecondaryContainer, 0),
            MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSecondaryContainer, 0)))
        if (!d.pipeline_tag.isNullOrBlank()) badgeRow.addView(badge(ctx, d.pipeline_tag,
            MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorTertiaryContainer, 0),
            MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnTertiaryContainer, 0)))
        if (!d.library_name.isNullOrBlank()) badgeRow.addView(badge(ctx, d.library_name,
            MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurfaceContainerHigh, 0), dim))
        if (!d.sdk.isNullOrBlank()) badgeRow.addView(badge(ctx, "SDK " + d.sdk,
            MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurfaceContainerHigh, 0), dim))

        val tagGroup = ChipGroup(ctx).apply {
            chipSpacingHorizontal = ctx.dp(6f)
            chipSpacingVertical = ctx.dp(4f)
            setPadding(0, ctx.dp(8f), 0, 0)
        }
        d.tags.take(16).forEach { tag ->
            tagGroup.addView(Chip(ctx).apply {
                text = tag
                isClickable = true
                isCheckable = false
                setEnsureMinTouchTargetSize(false)
                setOnClickListener { (activity as? MainActivity)?.searchModels(tag) }
            })
        }

        val inner = vbox(ctx, headRow, badgeRow, tagGroup)
        val cardPad = ctx.dp(14f)
        inner.setPadding(cardPad, cardPad, cardPad, cardPad)
        card.addView(inner, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        overviewBox.addView(card, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }


    private fun renderFiles(entries: List<FileEntry>, loading: Boolean) {
        val ctx = requireContext()
        val on = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, 0)
        val dim = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        filesBox.removeAllViews()
        if (currentDir.isEmpty()) {
            filesTitle.text = "文件 (${entries.size})"
            filesTitle.setLeftIcon(R.drawable.ic_folder, 18, on, 6)
        } else {
            filesTitle.text = "${currentDir.substringAfterLast('/')} (${entries.size})"
            filesTitle.setLeftIcon(R.drawable.ic_folder, 18, on, 6)
        }
        breadcrumb.text = if (currentDir.isEmpty()) "" else "路径：$repoId / $currentDir"
        if (loading) {
            filesBox.addView(tv(ctx, "加载目录…", 13f, dim).apply { setPadding(0, ctx.dp(6f), 0, ctx.dp(6f)) })
            return
        }
        if (entries.isEmpty()) {
            filesBox.addView(tv(ctx, "（无文件）", 13f, dim).apply { setPadding(0, ctx.dp(6f), 0, ctx.dp(6f)) })
            return
        }
        entries.take(100).forEach { f ->
            val isDir = f.type == "directory"
            val ic = iconView(if (isDir) R.drawable.ic_folder else R.drawable.ic_file, 17, dim)
            ic.layoutParams = LinearLayout.LayoutParams(ctx.dp(22f), ctx.dp(22f)).apply { rightMargin = ctx.dp(10f) }
            val nameTv = tv(ctx, f.path.substringAfterLast('/'), 14f, on, false, 1)
            nameTv.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            val sizeTv = tv(ctx, if (isDir) "›" else fmtBytes(f.size), 12f, dim)
            val row = hbox(ctx, ic, nameTv, sizeTv).apply {
                setPadding(ctx.dp(14f), ctx.dp(13f), ctx.dp(14f), ctx.dp(13f))
                background = rounded(ctx.dp(10f), MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerLow, 0))
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = ctx.dp(6f)
            row.setOnClickListener {
                if (isDir) {
                    currentDir = if (currentDir.isEmpty()) f.path else "$currentDir/${f.path}"
                    renderFiles(emptyList(), loading = true)
                    loadTree(currentDir)
                } else {
                    confirmDownload(f)
                }
            }
            filesBox.addView(row, lp)
        }
        if (entries.size > 100) {
            filesBox.addView(tv(ctx, "… 还有 ${entries.size - 100} 项未显示", 12f, dim).apply {
                setPadding(0, ctx.dp(8f), 0, 0)
            })
        }
    }

    
    private fun showTab(intro: Boolean) {
        introCard.visibility = if (intro) View.VISIBLE else View.GONE
        filesCard.visibility = if (intro) View.GONE else View.VISIBLE
        tabChipIntro.isChecked = intro
        tabChipFiles.isChecked = !intro
    }

    private suspend fun renderReadme(readme: String) {
        readmeBox.removeAllViews()
        val ctx = requireContext()
        val on = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, 0)
        val dim = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        if (readme.isBlank()) {
            readmeBox.addView(tv(ctx, "（该仓库没有 README）", 13f, dim))
            return
        }
        val res = Markdown.render(readme)

        
        
        val imgUrls = Regex("<img src=\"([^\"]+)\"").findAll(res.html)
            .map { it.groupValues[1] }
            .filter { it.startsWith("http") }
            .distinct()
            .sortedBy { if (it.contains("hf-mirror.com")) 0 else 1 }
            .take(10).toList()
        val bitmaps: Map<String, Bitmap> = if (imgUrls.isEmpty()) emptyMap() else
            (kotlinx.coroutines.withTimeoutOrNull(6000) {
                withContext(Dispatchers.IO) {
                    imgUrls.mapNotNull { u -> ImageLoader.getSync(u)?.let { u to it } }.toMap()
                }
            } ?: emptyMap())
        val maxW = resources.displayMetrics.widthPixels - ctx.dp(48f)
        val getter = android.text.Html.ImageGetter { source ->
            val bmp = bitmaps[source]
            if (bmp == null || bmp.width <= 0) null
            else {
                val w = minOf(bmp.width, maxW)
                val h = (bmp.height.toFloat() / bmp.width * w).toInt().coerceAtLeast(1)
                android.graphics.drawable.BitmapDrawable(resources, bmp).apply { setBounds(0, 0, w, h) }
            }
        }

        
        val html2 = Regex("<a href=\"([^\"]+)\"").replace(res.html) { m ->
            "<a href=\"hf://copy?u=" + URLEncoder.encode(m.groupValues[1], "UTF-8") + "\""
        }

        
        var last = 0
        Regex("\u0001T(\\d+)\u0001").findAll(html2).forEach { m ->
            val seg = html2.substring(last, m.range.first)
            if (seg.isNotBlank()) readmeBox.addView(makeRichText(seg, getter, on))
            val idx = m.groupValues[1].toIntOrNull() ?: -1
            res.tables.getOrNull(idx)?.let { readmeBox.addView(makeTable(it, dim)) }
            last = m.range.last + 1
        }
        val tail = html2.substring(last)
        if (tail.isNotBlank()) readmeBox.addView(makeRichText(tail, getter, on))
    }

    private fun makeRichText(html: String, getter: android.text.Html.ImageGetter, on: Int): TextView {
        val ctx = requireContext()
        return tv(ctx, "", 14f, on).apply {
            try {
                text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY, getter, null)
            } catch (e: Exception) {
                text = html.replace(Regex("<[^>]+>"), "").take(4000)
            }
            movementMethod = CopyLinkMovementMethod()
            setLineSpacing(ctx.dp(4f).toFloat(), 1f)
        }
    }

    
    private fun makeTable(t: Markdown.Table, dim: Int): View {
        val ctx = requireContext()
        val on = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, 0)
        val headerBg = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurfaceContainerHigh, 0)
        val colW = ctx.dp(140f)

        fun cell(text: String, bold: Boolean, bg: Int): TextView {
            val v = tv(ctx, text, 12f, if (bold) on else dim, bold, 6)
            v.setPadding(ctx.dp(9f), ctx.dp(7f), ctx.dp(9f), ctx.dp(7f))
            v.layoutParams = LinearLayout.LayoutParams(colW, ViewGroup.LayoutParams.WRAP_CONTENT)
            if (bg != 0) v.background = rounded(0, bg)
            return v
        }

        fun rowOf(cells: List<String>, bold: Boolean): LinearLayout {
            val r = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            cells.forEach { r.addView(cell(it, bold, if (bold) headerBg else 0)) }
            return r
        }

        val grid = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        grid.addView(rowOf(t.header, true))
        t.rows.forEach { grid.addView(rowOf(it, false)) }
        val scrollH = HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            addView(grid)
        }
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val p = ctx.dp(8f)
            setPadding(p, p, p, p)
            background = rounded(ctx.dp(12f),
                MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerLow, 0))
        }
        card.addView(scrollH, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = ctx.dp(6f)
        })
        return card
    }

    private fun confirmDownload(f: FileEntry) {
        val ctx = requireContext()
        val name = f.path.substringAfterLast('/')
        MaterialAlertDialogBuilder(ctx)
            .setTitle("下载文件？")
            .setMessage("$name\n${fmtBytes(f.size)}\n\n将保存到 Download/HF/$repoId/")
            .setPositiveButton("下载") { _, _ ->
                val url = HfApi.resolveUrl(kind, repoId, f.path)
                try {
                    val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val req = DownloadManager.Request(Uri.parse(url)).apply {
                        setTitle(name)
                        setDescription(repoId)
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "HF/$repoId/$name")
                        addRequestHeader("User-Agent", HfApi.UA)
                    }
                    dm.enqueue(req)
                    android.widget.Toast.makeText(ctx, "已加入下载队列", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(ctx, "下载失败：${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    
    private fun iconView(resId: Int, sizeDp: Int, tint: Int): ImageView {
        val ctx = requireContext()
        return ImageView(ctx).apply {
            val d = androidx.appcompat.content.res.AppCompatResources.getDrawable(ctx, resId)
            d?.setBounds(0, 0, ctx.dp(sizeDp), ctx.dp(sizeDp))
            d?.setTint(tint)
            setImageDrawable(d)
        }
    }

    
    private fun TextView.setLeftIcon(resId: Int, sizeDp: Int, tint: Int, gapDp: Int) {
        val ctx = context
        val d = androidx.appcompat.content.res.AppCompatResources.getDrawable(ctx, resId)
        d?.setBounds(0, 0, ctx.dp(sizeDp), ctx.dp(sizeDp))
        d?.setTint(tint)
        setCompoundDrawables(d, null, null, null)
        compoundDrawablePadding = ctx.dp(gapDp)
    }

    private fun copyText(text: String) {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("link", text))
        android.widget.Toast.makeText(requireContext(), "已复制链接", android.widget.Toast.LENGTH_SHORT).show()
    }

    



    private inner class CopyLinkMovementMethod : LinkMovementMethod() {
        override fun onTouchEvent(widget: TextView, buffer: android.text.Spannable, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                val layout = widget.layout ?: return false
                val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
                val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
                val line = layout.getLineForVertical(y)
                val off = layout.getOffsetForHorizontal(line, x.toFloat())
                if (buffer.getSpans(off, off, URLSpan::class.java).isEmpty()) return false
            }
            if (event.action == MotionEvent.ACTION_UP) {
                val layout = widget.layout ?: return true
                val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
                val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
                val line = layout.getLineForVertical(y)
                val off = layout.getOffsetForHorizontal(line, x.toFloat())
                val spans = buffer.getSpans(off, off, URLSpan::class.java)
                for (span in spans) {
                    val u = span.url
                    if (u.startsWith("hf://copy?u=")) {
                        val decoded = try { java.net.URLDecoder.decode(u.removePrefix("hf://copy?u="), "UTF-8") } catch (_: Exception) { u }
                        copyText(decoded)
                        return true
                    }
                }
            }
            return super.onTouchEvent(widget, buffer, event)
        }
    }
}
