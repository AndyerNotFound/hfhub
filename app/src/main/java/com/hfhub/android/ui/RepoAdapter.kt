package com.hfhub.android.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.hfhub.android.R
import com.hfhub.android.data.RepoCard
import com.hfhub.android.util.Avatar
import com.hfhub.android.util.AvatarLoader
import com.hfhub.android.util.badge
import com.hfhub.android.util.dp
import com.hfhub.android.util.fmtDate
import com.hfhub.android.util.fmtNum
import com.hfhub.android.util.hbox
import com.hfhub.android.util.tv
import com.hfhub.android.util.vbox





class RepoAdapter(
    private val ctx: Context,
    private val items: List<RepoCard>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<RepoAdapter.VH>() {

    class VH(val card: MaterialCardView) : RecyclerView.ViewHolder(card)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val card = MaterialCardView(parent.context).apply {
            radius = parent.context.dp(16f).toFloat()
            cardElevation = 0f
            strokeWidth = parent.context.dp(1f)
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = parent.context.dp(6f)
            }
        }
        return VH(card)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = items[pos]
        val c = h.card.context

        val on = MaterialColors.getColor(h.card, com.google.android.material.R.attr.colorOnSurface, 0)
        val dim = MaterialColors.getColor(h.card, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        val surface = MaterialColors.getColor(h.card, com.google.android.material.R.attr.colorSurfaceContainerLow, 0)
        val outline = MaterialColors.getColor(h.card, com.google.android.material.R.attr.colorOutlineVariant, 0)
        val p = c.dp(12f)

        h.card.setCardBackgroundColor(surface)
        h.card.strokeColor = outline

        
        val avatar = ImageView(c).apply {
            layoutParams = LinearLayout.LayoutParams(c.dp(40f), c.dp(40f)).apply {
                rightMargin = c.dp(10f)
                topMargin = c.dp(2f)
            }
            setImageDrawable(Avatar.drawable(c, item.id))   
        }
        AvatarLoader.load(Avatar.authorOf(item.id), avatar)

        
        val name = tv(c, item.id, 15f, on, true, 2)
        val desc = item.desc?.takeIf { it.isNotBlank() }?.let {
            tv(c, it.trim(), 12f, dim, false, 2).apply { setPadding(0, c.dp(2f), 0, 0) }
        }
        val metaViews = ArrayList<View>()
        item.badge?.takeIf { x -> x.isNotBlank() }?.let { b ->
            metaViews.add(badge(c, b, MaterialColors.getColor(h.card, com.google.android.material.R.attr.colorSecondaryContainer, 0),
                MaterialColors.getColor(h.card, com.google.android.material.R.attr.colorOnSecondaryContainer, 0)))
        }
        if (item.gated) {
            metaViews.add(badge(c, "🔒 需授权", 0x22FFB300, 0xFFB06000.toInt()))
        }
        if (item.downloads > 0 || item.likes > 0) {
            metaViews.add(tv(c, "⬇ ${fmtNum(item.downloads)}", 11f, dim).apply { setPadding(0, 0, c.dp(8f), 0) })
            metaViews.add(tv(c, "❤ ${fmtNum(item.likes)}", 11f, dim))
        }
        if (!item.date.isNullOrBlank()) {
            metaViews.add(tv(c, "· ${fmtDate(item.date)}", 11f, dim).apply { setPadding(c.dp(6f), 0, 0, 0) })
        }
        val metaRow = hbox(c, *metaViews.toTypedArray()).apply { setPadding(0, c.dp(6f), 0, 0) }

        val middle = vbox(c, name)
        desc?.let { middle.addView(it) }
        middle.addView(metaRow)
        middle.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

        val row = hbox(c, avatar, middle)
        row.setPadding(p, p, p, p)

        h.card.removeAllViews()
        h.card.addView(row, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        h.card.setOnClickListener { onClick(item.id) }
    }
}
