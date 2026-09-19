package com.hfhub.android.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hfhub.android.MainActivity
import com.hfhub.android.R
import com.hfhub.android.data.HfApi
import com.hfhub.android.data.QuickSearch
import com.hfhub.android.data.RepoCard
import com.hfhub.android.util.dp
import com.hfhub.android.util.rounded
import com.hfhub.android.util.tv
import com.hfhub.android.util.vbox
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch





abstract class BaseListFragment : Fragment() {

    protected lateinit var list: RecyclerView
    protected lateinit var status: TextView
    protected lateinit var searchBox: EditText
    protected lateinit var chipGroup: ChipGroup
    private lateinit var adapter: RepoAdapter

    protected var currentSort = "downloads"
    protected val activeFilters = mutableListOf<String>()
    protected var query = ""
    private var skip = 0
    private var loading = false
    private var ended = false

    private val items = mutableListOf<RepoCard>()

    
    private var suggestJob: Job? = null
    private var suppressSuggest = false
    private var popup: ListPopupWindow? = null
    private val suggestRows = mutableListOf<SuggestRow>()
    private var suggestAdapter: ArrayAdapter<SuggestRow>? = null

    
    abstract fun kind(): String

    
    abstract fun sortOptions(): List<Pair<String, String>>

    
    abstract suspend fun fetch(skip: Int): List<RepoCard>

    
    abstract fun openDetail(id: String)

    
    protected open fun openDetail(kind: String, id: String) {
        (activity as? MainActivity)?.openSecondary(kind, id)
    }

    
    fun searchFor(q: String) {
        suppressSuggest = true
        popup?.dismiss()
        searchBox.setText(q)
        query = q.trim()
        reload()
    }

    override fun onCreateView(inflater: android.view.LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()

        searchBox = EditText(ctx).apply {
            hint = "搜索 ${label()}"
            setTextSize(14f)
            maxLines = 1
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setSingleLine(true)
            val p = ctx.dp(14f)
            setPadding(p, ctx.dp(10f), p, ctx.dp(10f))
            background = rounded(ctx.dp(24f), MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerHigh, 0))
            AppCompatResources.getDrawable(ctx, R.drawable.ic_search)?.let { icon ->
                icon.setBounds(0, 0, ctx.dp(18f), ctx.dp(18f))
                setCompoundDrawables(icon, null, null, null)
            }
            compoundDrawablePadding = ctx.dp(8f)
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    popup?.dismiss()
                    val input = text.toString().trim()
                    
                    val target = com.hfhub.android.util.Links.parse(input)
                    if (target != null) {
                        if (target.type == "user") {
                            (activity as? MainActivity)?.openUser(target.id)
                        } else {
                            openDetail(target.kind, target.id)
                        }
                        true
                    } else {
                        query = input
                        reload()
                        true
                    }
                } else false
            }
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, st: Int, cnt: Int, aft: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, before: Int, count: Int) {
                    onQueryChanged(s?.toString().orEmpty())
                }
            })
        }

        chipGroup = ChipGroup(ctx).apply {
            isSingleSelection = true
            isSingleLine = true
            chipSpacingHorizontal = ctx.dp(8f)
        }
        rebuildChips()
        val chipScroll = HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            addView(chipGroup)
        }

        list = RecyclerView(ctx).apply {
            layoutManager = LinearLayoutManager(ctx)
            adapter = RepoAdapter(ctx, items, ::openDetail)
        }
        adapter = list.adapter as RepoAdapter

        status = tv(ctx, "", 14f, MaterialColors.getColor(list, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)).apply {
            gravity = Gravity.CENTER
            setPadding(0, ctx.dp(24f), 0, ctx.dp(24f))
        }

        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val lm = rv.layoutManager as? LinearLayoutManager ?: return
                val last = lm.findLastVisibleItemPosition()
                if (last >= items.size - 5 && !loading && !ended && items.isNotEmpty()) {
                    loadMore()
                }
            }
        })

        val root = vbox(ctx)
        root.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val pad = ctx.dp(12f)
        root.setPadding(pad, ctx.dp(6f), pad, 0)
        root.addView(searchBox, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(chipScroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = ctx.dp(8f)
        })
        root.addView(status, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(list, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reload()
    }

    protected open fun label(): String = when (kind()) {
        "datasets" -> "数据集"
        "spaces" -> "Space"
        else -> "模型"
    }

    

    private class SuggestRow(
        val type: Int, val label: String, val kind: String = "", val itemId: String = ""
    ) {
        companion object {
            const val HEADER = 0
            const val ITEM = 1
            const val ALL = 2
        }
    }

    private fun onQueryChanged(text: String) {
        suggestJob?.cancel()
        if (suppressSuggest) {
            suppressSuggest = false
            return
        }
        val q = text.trim()
        if (q.length < 2) {
            popup?.dismiss()
            return
        }
        suggestJob = lifecycleScope.launch {
            delay(280)  
            val r = try {
                HfApi.quicksearch(q)
            } catch (_: Exception) {
                return@launch
            }
            
            
            if (searchBox.text.toString().trim() != q) return@launch
            buildSuggestRows(q, r)
        }
    }

    private fun buildSuggestRows(q: String, r: QuickSearch) {
        suggestRows.clear()
        fun addGroup(title: String, k: String, list: List<com.hfhub.android.data.QuickItem>, count: Long) {
            if (list.isEmpty()) return
            suggestRows.add(SuggestRow(SuggestRow.HEADER, title))
            list.forEach { suggestRows.add(SuggestRow(SuggestRow.ITEM, it.id, k, it.id)) }
            suggestRows.add(SuggestRow(SuggestRow.ALL, "查看全部 $count 条「$q」", k))
        }
        addGroup("模型", "models", r.models, r.modelsCount)
        addGroup("数据集", "datasets", r.datasets, r.datasetsCount)
        addGroup("Space", "spaces", r.spaces, r.spacesCount)
        if (suggestRows.isEmpty()) {
            popup?.dismiss()
            return
        }
        showSuggestPopup(q)
    }

    private fun showSuggestPopup(q: String) {
        val ctx = requireContext()
        if (popup == null) {
            popup = ListPopupWindow(ctx).apply {
                anchorView = searchBox
                isModal = false
                width = searchBox.width.coerceAtLeast(ctx.dp(200f))
                setBackgroundDrawable(rounded(ctx.dp(12f), MaterialColors.getColor(
                    searchBox, com.google.android.material.R.attr.colorSurfaceContainerHigh, 0)
                ).apply { setStroke(ctx.dp(1f), 0x33808080) })
            }
        }
        if (suggestAdapter == null) {
            suggestAdapter = object : ArrayAdapter<SuggestRow>(ctx, 0, suggestRows) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val row = getItem(position) ?: return View(parent.context)
                    val on = MaterialColors.getColor(parent, com.google.android.material.R.attr.colorOnSurface, 0)
                    val dim = MaterialColors.getColor(parent, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
                    val primary = MaterialColors.getColor(parent, com.google.android.material.R.attr.colorPrimary, 0)
                    val padH = parent.context.dp(14f)
                    val t = TextView(parent.context)
                    when (row.type) {
                        SuggestRow.HEADER -> {
                            t.text = row.label
                            t.setTextSize(12f)
                            t.setTextColor(dim)
                            t.setPadding(padH, parent.context.dp(10f), padH, parent.context.dp(3f))
                        }
                        SuggestRow.ALL -> {
                            t.text = "→ ${row.label}"
                            t.setTextSize(13f)
                            t.setTextColor(primary)
                            t.setPadding(padH, parent.context.dp(8f), padH, parent.context.dp(10f))
                        }
                        else -> {
                            t.text = row.label
                            t.setTextSize(14f)
                            t.setTextColor(on)
                            t.setPadding(padH, parent.context.dp(8f), padH, parent.context.dp(8f))
                        }
                    }
                    return t
                }
            }
            popup?.setAdapter(suggestAdapter)
            popup?.setOnItemClickListener { _, _, position, _ ->
                val row = suggestRows.getOrNull(position) ?: return@setOnItemClickListener
                popup?.dismiss()
                when (row.type) {
                    SuggestRow.ITEM -> openDetail(row.itemId)
                    SuggestRow.ALL -> {
                        
                        val cur = searchBox.text.toString().trim().ifBlank { q }
                        query = cur
                        searchBox.setText(cur)
                        reload()
                    }
                }
            }
        }
        suggestAdapter?.notifyDataSetChanged()
        if (popup?.isShowing != true) popup?.show()
    }

    

    private fun rebuildChips() {
        chipGroup.removeAllViews()
        val ctx = requireContext()
        for ((value, name) in sortOptions()) {
            val chip = Chip(ctx).apply {
                text = name
                isCheckable = true
                isChecked = currentSort == value
                setOnClickListener {
                    currentSort = value
                    rebuildChips()
                    reload()
                }
            }
            chipGroup.addView(chip)
        }
        
        if (com.hfhub.android.data.FilterData.categories(kind()).isNotEmpty()) {
            val n = activeFilters.size
            val fChip = Chip(ctx).apply {
                text = if (n > 0) "筛选 ($n)" else "筛选"
                isCheckable = true
                isChecked = n > 0
                chipIcon = AppCompatResources.getDrawable(ctx, R.drawable.ic_filter)
                chipIconSize = ctx.dp(16f).toFloat()
                setOnClickListener {
                    (activity as? MainActivity)?.openFilter(kind(), activeFilters)
                }
            }
            chipGroup.addView(fChip)
        }
    }

    
    fun refresh() = reload()

    
    fun onFiltersChanged(filters: List<String>) {
        activeFilters.clear()
        activeFilters.addAll(filters)
        rebuildChips()
        reload()
    }

    
    protected fun reload() {
        loading = true
        ended = false
        skip = 0
        status.text = "加载中…"
        status.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val page = fetch(0)
                loading = false
                items.clear()
                items.addAll(page)
                adapter.notifyDataSetChanged()
                skip = page.size
                ended = page.size < HfApi.PAGE_LIMIT
                status.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                if (items.isEmpty()) status.text = "没有结果"
            } catch (e: Exception) {
                loading = false
                status.visibility = View.VISIBLE
                status.text = "加载失败：${e.message?.take(80) ?: e.javaClass.simpleName}\n(点击重试)"
                status.setOnClickListener { reload() }
            }
        }
    }

    private fun loadMore() {
        loading = true
        val prevSize = items.size
        status.text = "加载中…"
        status.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val page = fetch(skip)
                loading = false
                if (page.isNotEmpty()) {
                    val oldSize = items.size
                    items.addAll(page)
                    adapter.notifyItemRangeInserted(oldSize, page.size)
                }
                skip += page.size
                ended = page.size < HfApi.PAGE_LIMIT
                status.visibility = View.GONE
            } catch (e: Exception) {
                loading = false
                status.visibility = if (items.size > prevSize) View.GONE else View.VISIBLE
                if (items.size <= prevSize) {
                    status.text = "加载失败：${e.message?.take(80) ?: e.javaClass.simpleName}\n(点击重试)"
                    status.setOnClickListener { loadMore() }
                }
            }
        }
    }
}
