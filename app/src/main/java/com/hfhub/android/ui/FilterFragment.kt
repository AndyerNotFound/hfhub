package com.hfhub.android.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.hfhub.android.MainActivity
import com.hfhub.android.R
import com.hfhub.android.data.FilterData
import com.hfhub.android.util.dp
import com.hfhub.android.util.rounded






class FilterFragment : Fragment() {

    companion object {
        const val RESULT_KEY = "hf_filter"
        private const val ARG_KIND = "kind"
        private const val ARG_CURRENT = "current"

        fun newInstance(kind: String, current: List<String>): FilterFragment = FilterFragment().apply {
            arguments = bundleOf(
                ARG_KIND to kind,
                ARG_CURRENT to ArrayList(current)
            )
        }
    }

    private val kind get() = requireArguments().getString(ARG_KIND) ?: "models"
    private val categories get() = FilterData.categories(kind)

    
    private val selection = LinkedHashMap<String, String>()
    private var currentCat = ""
    private var searchText = ""

    private lateinit var catGroup: ChipGroup
    private lateinit var optionGroup: ChipGroup
    private lateinit var searchBox: TextInputEditText

    override fun onCreateView(inflater: android.view.LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        val on = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, 0)

        
        (requireArguments().getStringArrayList(ARG_CURRENT) ?: arrayListOf()).forEach { f ->
            parseFilter(f)?.let { (cat, value) -> selection[cat] = value }
        }
        currentCat = categories.firstOrNull() ?: FilterData.CAT_TASK

        
        val toolbar = MaterialToolbar(ctx).apply {
            setBackgroundColor(MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurface, 0))
            setNavigationIcon(R.drawable.ic_close)
            title = "筛选"
            setNavigationOnClickListener { (activity as? MainActivity)?.closeSecondary() }
            menu.add(0, 1, 0, "重置")
            setOnMenuItemClickListener { item ->
                if (item.itemId == 1) {
                    selection.clear()
                    rebuildOptions()
                    true
                } else false
            }
        }

        
        catGroup = ChipGroup(ctx).apply {
            isSingleSelection = true
            isSingleLine = true
            chipSpacingHorizontal = ctx.dp(8f)
        }
        rebuildCategories()
        val catScroll = HorizontalScrollView(ctx).apply {
            isHorizontalScrollBarEnabled = false
            addView(catGroup)
        }

        
        searchBox = TextInputEditText(ctx).apply {
            hint = "搜索选项"
            setSingleLine(true)
            setOnFocusChangeListener { _, _ -> }
        }
        val searchWrap = TextInputLayout(ctx).apply {
            addView(searchBox, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        searchBox.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                searchText = s?.toString().orEmpty().trim().lowercase()
                rebuildOptions()
            }
        })

        
        optionGroup = ChipGroup(ctx).apply {
            isSingleSelection = false
            chipSpacingHorizontal = ctx.dp(8f)
            chipSpacingVertical = ctx.dp(2f)
            setPadding(0, ctx.dp(4f), 0, ctx.dp(4f))
        }
        rebuildOptions()
        val optionScroll = ScrollView(ctx).apply {
            addView(optionGroup, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        
        val applyBtn = MaterialButton(ctx).apply {
            text = "✓ 应用筛选"
            setOnClickListener { apply() }
        }

        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root.setBackgroundColor(MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurface, 0))
        val p = ctx.dp(16f)
        root.setPadding(p, ctx.dp(4f), p, ctx.dp(16f))
        root.addView(toolbar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(catScroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(searchWrap, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = ctx.dp(8f)
        })
        root.addView(optionScroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f).apply {
            topMargin = ctx.dp(6f)
        })
        root.addView(applyBtn, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return root
    }

    private fun rebuildCategories() {
        catGroup.removeAllViews()
        val ctx = requireContext()
        val primaryContainer = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorPrimaryContainer, 0)
        val surface = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurfaceContainerHigh, 0)
        categories.forEach { cat ->
            val chip = Chip(ctx).apply {
                text = FilterData.categoryLabel(cat)
                isCheckable = false        
                isClickable = true
                setEnsureMinTouchTargetSize(false)
                styleChip(this, cat == currentCat)
                setOnClickListener {
                    currentCat = cat
                    rebuildCategories()
                    rebuildOptions()
                }
            }
            catGroup.addView(chip)
        }
    }

    private fun rebuildOptions() {
        optionGroup.removeAllViews()
        val ctx = requireContext()
        var values = FilterData.valuesFor(kind, currentCat)
        if (searchText.isNotEmpty()) {
            values = values.filter { it.lowercase().contains(searchText) }
        }
        val total = values.size
        values.take(80).forEach { v ->
            val chip = Chip(ctx).apply {
                text = v
                isCheckable = false        
                isClickable = true
                setEnsureMinTouchTargetSize(false)
                styleChip(this, selection[currentCat] == v)
                setOnClickListener {
                    if (selection[currentCat] == v) selection.remove(currentCat)
                    else selection[currentCat] = v
                    android.util.Log.i("HfFilter", "click $currentCat=$v -> selection=$selection")
                    rebuildOptions()
                }
            }
            optionGroup.addView(chip)
        }
        if (total > 80) {
            val more = Chip(ctx).apply {
                text = "… 共 $total 项, 用搜索框查找"
                isClickable = false
                isCheckable = false
                setEnsureMinTouchTargetSize(false)
            }
            optionGroup.addView(more)
        }
        if (values.isEmpty()) {
            optionGroup.addView(android.widget.TextView(ctx).apply {
                text = "没有匹配的选项"
                setPadding(ctx.dp(4f), ctx.dp(12f), 0, 0)
            })
        }
    }

    
    private fun parseFilter(f: String): Pair<String, String>? = FilterData.parseFilter(kind, f)

    
    private fun styleChip(chip: Chip, selected: Boolean) {
        val c = chip.context
        val bgSel = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorPrimaryContainer, 0)
        val fgSel = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorOnPrimaryContainer, 0)
        val bgUn = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorSurface, 0)
        val fgUn = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        val outline = MaterialColors.getColor(chip, com.google.android.material.R.attr.colorOutline, 0)
        chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(if (selected) bgSel else bgUn))
        chip.setTextColor(if (selected) fgSel else fgUn)
        if (selected) {
            chip.chipStrokeWidth = 0f
        } else {
            chip.chipStrokeWidth = c.dp(1f).toFloat()
            chip.chipStrokeColor = android.content.res.ColorStateList.valueOf(outline)
        }
    }

    private fun apply() {
        val filters = selection.map { (cat, value) ->
            FilterData.toFilter(kind, cat, value)
        }
        android.util.Log.i("HfFilter", "apply kind=$kind filters=$filters")
        val act = activity as? MainActivity
        if (filters.isEmpty()) {
            
            android.widget.Toast.makeText(requireContext(), "已清除筛选，显示全部", android.widget.Toast.LENGTH_SHORT).show()
            act?.applyFilters(kind, emptyList())
            return
        }
        android.widget.Toast.makeText(requireContext(), "已应用 ${filters.size} 项筛选", android.widget.Toast.LENGTH_SHORT).show()
        act?.applyFilters(kind, filters)
    }
}
