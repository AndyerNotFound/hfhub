package com.hfhub.android.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.hfhub.android.R
import com.hfhub.android.data.DailyPaper
import com.hfhub.android.util.ImageLoader
import com.hfhub.android.util.dp
import com.hfhub.android.util.fmtDate
import com.hfhub.android.util.fmtNum
import com.hfhub.android.util.hbox
import com.hfhub.android.util.tv
import com.hfhub.android.util.vbox


class PaperAdapter(
    private val ctx: Context,
    private val items: List<DailyPaper>,
    private val onClick: (DailyPaper) -> Unit
) : RecyclerView.Adapter<PaperAdapter.VH>() {

    class VH(val card: MaterialCardView) : RecyclerView.ViewHolder(card)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val card = MaterialCardView(parent.context).apply {
            radius = parent.context.dp(14f).toFloat()
            cardElevation = 0f
            useCompatPadding = true
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = parent.context.dp(8f)
            }
        }
        return VH(card)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = items[pos]
        val c = h.card.context
        val on = MaterialColors.getColor(h.card, com.google.android.material.R.attr.colorOnSurface, 0)
        val dim = MaterialColors.getColor(h.card, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        val pad = c.dp(12f)

        val thumb = ImageView(c).apply {
            layoutParams = LinearLayout.LayoutParams(c.dp(72f), c.dp(72f)).apply {
                rightMargin = c.dp(10f)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
        }

        val authors = p.paper?.authors?.take(3)?.mapNotNull { it.name }?.joinToString(", ").orEmpty()
        val metaLine = hbox(c,
            tv(c, "📄 ${fmtDate(p.publishedAt)}", 11f, dim),
            tv(c, "💬 ${p.numComments}", 11f, dim).apply { setPadding(c.dp(10f), 0, 0, 0) }
        )

        val right = vbox(c,
            tv(c, p.title.orEmpty(), 14f, on, true, 2),
            tv(c, p.summary?.trim().orEmpty(), 12f, dim, false, 3).apply {
                setPadding(0, c.dp(3f), 0, 0)
            },
            metaLine.apply { setPadding(0, c.dp(5f), 0, 0) }
        )

        val body = hbox(c, thumb, right)
        body.setPadding(pad, pad, pad, pad)

        h.card.removeAllViews()
        h.card.addView(body, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        h.card.setOnClickListener { onClick(p) }

        val url = p.thumbnail
        if (url.isNullOrBlank()) {
            thumb.setImageDrawable(null)
        } else {
            ImageLoader.load(url, thumb)
        }
    }
}
