package com.hfhub.android.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.hfhub.android.MainActivity
import com.hfhub.android.R
import com.hfhub.android.data.HfApi
import com.hfhub.android.data.OwnerInfo
import com.hfhub.android.util.Avatar
import com.hfhub.android.util.AvatarLoader
import com.hfhub.android.util.badge
import com.hfhub.android.util.dp
import com.hfhub.android.util.fmtNum
import com.hfhub.android.util.rounded
import com.hfhub.android.util.tv
import com.hfhub.android.util.vbox
import kotlinx.coroutines.launch





class UserFragment : Fragment() {

    companion object {
        private const val ARG_NAME = "name"
        fun newInstance(name: String): UserFragment = UserFragment().apply {
            arguments = bundleOf(ARG_NAME to name)
        }
    }

    private val userName get() = requireArguments().getString(ARG_NAME) ?: ""

    override fun onCreateView(inflater: android.view.LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val ctx = requireContext()
        val on = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, 0)

        val toolbar = MaterialToolbar(ctx).apply {
            setBackgroundColor(MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurface, 0))
            setNavigationIcon(R.drawable.ic_arrow_back)
            title = userName
            setNavigationOnClickListener { (activity as? MainActivity)?.closeSecondary() }
        }

        val content = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val pad = ctx.dp(18f)
        content.setPadding(pad, ctx.dp(6f), pad, ctx.dp(24f))

        val status = tv(ctx, "加载中…", 14f, MaterialColors.getColor(
            ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)).apply {
            gravity = android.view.Gravity.CENTER
            setPadding(0, ctx.dp(30f), 0, 0)
        }
        content.addView(status)

        val scroll = ScrollView(ctx)
        scroll.addView(content, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root.setBackgroundColor(MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSurface, 0))
        root.addView(toolbar, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        load(content, status)
        return root
    }

    private fun load(content: LinearLayout, status: android.widget.TextView) {
        val ctx = requireContext()
        lifecycleScope.launch {
            try {
                val info = HfApi.owner(userName)
                val models = try { HfApi.listModels("", "downloads", 0, author = userName) } catch (_: Exception) { emptyList() }
                val datasets = try { HfApi.listDatasets("", "downloads", 0, author = userName) } catch (_: Exception) { emptyList() }
                val spaces = try { HfApi.listSpaces("", "likes", 0, author = userName) } catch (_: Exception) { emptyList() }
                status.visibility = View.GONE
                content.addView(buildHeader(info, models.size, datasets.size, spaces.size))
                if (models.isNotEmpty()) content.addView(buildSection("模型", "models", models.map { it.id to "⬇ ${fmtNum(it.downloads)} · ❤ ${fmtNum(it.likes)}" }))
                if (datasets.isNotEmpty()) content.addView(buildSection("数据集", "datasets", datasets.map { it.id to "⬇ ${fmtNum(it.downloads)} · ❤ ${fmtNum(it.likes)}" }))
                if (spaces.isNotEmpty()) content.addView(buildSection("Space", "spaces", spaces.map { it.id to "❤ ${fmtNum(it.likes)}" }))
                if (models.isEmpty() && datasets.isEmpty() && spaces.isEmpty()) {
                    content.addView(tv(ctx, "该用户没有公开内容", 13f,
                        MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)))
                }
            } catch (e: Exception) {
                status.text = "加载失败：${e.message?.take(80) ?: e.javaClass.simpleName}\n(点击重试)"
                status.setOnClickListener { load(content, status) }
            }
        }
    }

    private fun buildHeader(info: OwnerInfo?, nM: Int, nD: Int, nS: Int): View {
        val ctx = requireContext()
        val on = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, 0)
        val dim = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)

        val card = MaterialCardView(ctx).apply {
            radius = ctx.dp(16f).toFloat()
            cardElevation = 0f
            strokeWidth = ctx.dp(1f)
            setCardBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerLow, 0))
            strokeColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant, 0)
        }

        val avatar = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(ctx.dp(72f), ctx.dp(72f)).apply { rightMargin = ctx.dp(14f) }
            setImageDrawable(Avatar.drawable(ctx, userName))
        }
        
        info?.avatarUrl?.takeIf { it.isNotBlank() }?.let { url ->
            com.hfhub.android.util.ImageLoader.load(url, avatar, circular = true)
        } ?: AvatarLoader.load(userName, avatar)

        val name = tv(ctx, info?.fullname?.takeIf { it.isNotBlank() } ?: userName, 20f, on, true, 2)
        val typeRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, ctx.dp(4f), 0, 0) }
        val typeLabel = when {
            info == null -> "用户 / 组织"
            info.numUsers > 0 -> "Team"
            !info.plan.isNullOrBlank() -> info.plan.replaceFirstChar { it.uppercase() }
            else -> "User"
        }
        typeRow.addView(badge(ctx, typeLabel,
            MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorSecondaryContainer, 0),
            MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSecondaryContainer, 0)))
        if (info != null && info.numFollowers > 0) {
            typeRow.addView(tv(ctx, "${fmtNum(info.numFollowers)} 关注", 12f, dim).apply {
                setPadding(ctx.dp(8f), 0, 0, 0)
            })
        }
        val stats = tv(ctx, "模型 $nM · 数据集 $nD · Space $nS", 13f, dim).apply {
            setPadding(0, ctx.dp(6f), 0, 0)
        }

        val right = vbox(ctx, name, typeRow, stats)
        right.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        val head = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(avatar)
            addView(right)
            setPadding(ctx.dp(14f), ctx.dp(14f), ctx.dp(14f), ctx.dp(14f))
        }
        card.addView(head, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return card
    }

    private fun buildSection(title: String, kind: String, rows: List<Pair<String, String>>): View {
        val ctx = requireContext()
        val on = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurface, 0)
        val dim = MaterialColors.getColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        box.addView(tv(ctx, title, 16f, on, true).apply {
            setPadding(0, ctx.dp(18f), 0, ctx.dp(6f))
        })
        rows.take(20).forEach { (id, meta) ->
            val nameTv = tv(ctx, id, 14f, on, false, 1)
            nameTv.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            val metaTv = tv(ctx, meta, 11f, dim)
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(ctx.dp(12f), ctx.dp(11f), ctx.dp(12f), ctx.dp(11f))
                background = rounded(ctx.dp(10f), MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainerLow, 0))
                addView(nameTv)
                addView(metaTv)
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = ctx.dp(5f)
            row.setOnClickListener {
                (activity as? MainActivity)?.openSecondary(kind, id, addToStack = true)
            }
            box.addView(row, lp)
        }
        return box
    }
}
