package com.hfhub.android

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hfhub.android.data.HfApi
import com.hfhub.android.data.SettingsStore
import com.hfhub.android.ui.BaseListFragment
import com.hfhub.android.ui.DatasetsFragment
import com.hfhub.android.ui.DetailFragment
import com.hfhub.android.ui.ModelsFragment
import com.hfhub.android.ui.PapersFragment
import com.hfhub.android.ui.SpacesFragment


class MainActivity : AppCompatActivity() {

    private data class Tab(val menuId: Int, val create: () -> Fragment)

    private val tabs = listOf(
        Tab(R.id.nav_models) { ModelsFragment() },
        Tab(R.id.nav_datasets) { DatasetsFragment() },
        Tab(R.id.nav_spaces) { SpacesFragment() },
        Tab(R.id.nav_papers) { PapersFragment() }
    )

    private lateinit var pager: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var mainContent: View
    private lateinit var secondaryContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.root)
        pager = findViewById(R.id.pager)
        bottomNav = findViewById(R.id.bottomNav)
        mainContent = findViewById(R.id.mainContent)
        secondaryContainer = findViewById(R.id.secondaryContainer)

        
        root.setBackgroundColor(MaterialColors.getColor(root, com.google.android.material.R.attr.colorSurface, 0))
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            v.setPadding(0, top, 0, 0)
            insets
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.toolbar_main)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_source) {
                showBaseDialog()
                true
            } else false
        }

        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = tabs.size
            override fun createFragment(position: Int): Fragment = tabs[position].create()
        }
        pager.offscreenPageLimit = 2
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val id = tabs[position].menuId
                if (bottomNav.selectedItemId != id) bottomNav.selectedItemId = id
            }
        })

        bottomNav.setOnItemSelectedListener { item ->
            val index = tabs.indexOfFirst { it.menuId == item.itemId }
            if (index >= 0) {
                pager.setCurrentItem(index, true)
                true
            } else false
        }

        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val f = supportFragmentManager.findFragmentById(R.id.secondaryContainer) as? DetailFragment
                if (f != null && f.handleBack()) return
                if (secondaryContainer.visibility == View.VISIBLE) {
                    closeSecondary()
                    return
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })
    }

    
    fun openSecondary(kind: String, repoId: String, addToStack: Boolean = false) {
        val tx = supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_right)
            .replace(R.id.secondaryContainer, DetailFragment.newInstance(kind, repoId))
        if (addToStack) tx.addToBackStack(null)
        tx.commitAllowingStateLoss()
        mainContent.visibility = View.GONE
        secondaryContainer.visibility = View.VISIBLE
    }

    
    fun openUser(name: String) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_right)
            .replace(R.id.secondaryContainer, com.hfhub.android.ui.UserFragment.newInstance(name))
            .commitAllowingStateLoss()
        mainContent.visibility = View.GONE
        secondaryContainer.visibility = View.VISIBLE
    }

    fun closeSecondary() {
        
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            return
        }
        mainContent.visibility = View.VISIBLE
        secondaryContainer.visibility = View.GONE
        val f = supportFragmentManager.findFragmentById(R.id.secondaryContainer)
        if (f != null) {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(0, R.anim.slide_out_right)
                .remove(f)
                .commitAllowingStateLoss()
        }
    }

    
    fun openFilter(kind: String, current: List<String>) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_right)
            .replace(R.id.secondaryContainer, com.hfhub.android.ui.FilterFragment.newInstance(kind, current))
            .commitAllowingStateLoss()
        mainContent.visibility = View.GONE
        secondaryContainer.visibility = View.VISIBLE
    }

    
    fun applyFilters(kind: String, filters: List<String>) {
        supportFragmentManager.fragments.forEach { f ->
            if (f is BaseListFragment && f.kind() == kind) {
                f.onFiltersChanged(filters)
            }
        }
        closeSecondary()
    }

    
    fun searchModels(query: String) {
        closeSecondary()
        pager.setCurrentItem(0, false)
        trySearch(query, 0)
    }

    private fun trySearch(query: String, attempt: Int) {
        val f = supportFragmentManager.fragments.firstOrNull { it is ModelsFragment } as? ModelsFragment
        if (f != null) {
            f.searchFor(query)
        } else if (attempt < 6) {
            pager.postDelayed({ trySearch(query, attempt + 1) }, 200)
        }
    }

    private fun showBaseDialog() {
        val ctx = this
        val input = com.google.android.material.textfield.TextInputEditText(ctx).apply {
            setText(SettingsStore.baseUrl)
            setSingleLine(true)
        }
        val inputBox = com.google.android.material.textfield.TextInputLayout(ctx).apply {
            hint = "Base URL"
            addView(input, android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        val padView = android.widget.FrameLayout(ctx).apply {
            val pad = (resources.displayMetrics.density * 20f).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(inputBox)
        }
        MaterialAlertDialogBuilder(ctx)
            .setTitle("数据源（HF 镜像）")
            .setMessage("国内网络直连 huggingface.co 通常超时，默认走 hf-mirror.com。")
            .setView(padView)
            .setNeutralButton("官方源") { _, _ -> switchBase(HfApi.OFFICIAL_BASE) }
            .setNegativeButton("默认镜像") { _, _ -> switchBase(HfApi.DEFAULT_BASE) }
            .setPositiveButton("使用自定义") { _, _ ->
                val v = input.text.toString().trim()
                if (v.isNotBlank()) switchBase(v)
            }
            .show()
    }

    private fun switchBase(url: String) {
        SettingsStore.baseUrl = url
        android.widget.Toast.makeText(this, "已切换：${SettingsStore.baseUrl}\n正在刷新…", android.widget.Toast.LENGTH_SHORT).show()
        supportFragmentManager.fragments.forEach { f ->
            when (f) {
                is BaseListFragment -> f.refresh()
                is PapersFragment -> f.refresh()
            }
        }
    }
}
