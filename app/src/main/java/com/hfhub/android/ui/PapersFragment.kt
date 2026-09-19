package com.hfhub.android.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hfhub.android.R
import com.hfhub.android.data.DailyPaper
import com.hfhub.android.data.HfApi
import com.hfhub.android.util.dp
import com.hfhub.android.util.vbox
import kotlinx.coroutines.launch


class PapersFragment : Fragment() {

    private val items = mutableListOf<DailyPaper>()
    private lateinit var status: TextView
    private lateinit var list: RecyclerView

    override fun onCreateView(inflater: android.view.LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        status = TextView(ctx).apply {
            text = "加载中…"
            setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0))
            gravity = android.view.Gravity.CENTER
            setPadding(0, ctx.dp(24f), 0, ctx.dp(24f))
        }
        list = RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            adapter = PaperAdapter(ctx, items, ::showPaper)
        }
        val root = vbox(ctx)
        root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val pad = ctx.dp(10f)
        root.setPadding(pad, pad, pad, 0)
        root.addView(status, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(list, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        refresh()
        return root
    }

    
    fun refresh() {
        load(status, list)
    }

    private fun load(status: TextView, list: RecyclerView) {
        status.setOnClickListener(null)
        status.text = "加载中…"
        status.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val papers = HfApi.papers()
                items.clear()
                items.addAll(papers)
                list.adapter?.notifyDataSetChanged()
                status.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                if (items.isEmpty()) status.text = "没有论文"
            } catch (e: Exception) {
                status.text = "加载失败：${e.message?.take(80) ?: e.javaClass.simpleName}\n(点击重试)"
                status.setOnClickListener { load(status, list) }
            }
        }
    }

    private fun showPaper(p: DailyPaper) {
        val ctx = requireContext()
        val paperId = p.paper?.id.orEmpty()
        val authors = p.paper?.authors?.mapNotNull { it.name }?.joinToString(", ").orEmpty()
        val msg = buildString {
            append(p.summary?.trim().orEmpty())
            if (authors.isNotBlank()) append("\n\n作者：").append(authors)
        }
        val url = HfApi.paperUrl(paperId)
        MaterialAlertDialogBuilder(ctx)
            .setTitle(p.title.orEmpty())
            .setMessage(msg)
            .setPositiveButton("复制论文链接") { _, _ ->
                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("paper", url))
                android.widget.Toast.makeText(ctx, "已复制", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("浏览器打开") { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) {}
            }
            .setNeutralButton("关闭", null)
            .show()
    }
}
