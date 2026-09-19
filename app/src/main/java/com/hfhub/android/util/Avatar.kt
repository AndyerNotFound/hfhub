package com.hfhub.android.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt





object Avatar {

    
    private val COLORS = intArrayOf(
        0xFF6750A4.toInt(), 0xFF7D5260.toInt(), 0xFF00696D.toInt(),
        0xFF7D5700.toInt(), 0xFF8C4A60.toInt(), 0xFF3E6837.toInt(),
        0xFF4A5C92.toInt(), 0xFF815600.toInt(), 0xFF6C5A9C.toInt(),
        0xFF35617A.toInt()
    )

    @ColorInt
    fun colorFor(name: String): Int {
        val h = name.hashCode().let { if (it == Int.MIN_VALUE) 0 else it }
        return COLORS[(h and 0x7FFFFFFF) % COLORS.size]
    }

    
    fun authorOf(repoId: String): String =
        repoId.substringBefore('/', repoId).trim()

    fun drawable(ctx: Context, repoId: String): Drawable =
        LetterDrawable(authorOf(repoId), colorFor(authorOf(repoId)), ctx.dp(40f))
}


class LetterDrawable(
    private val name: String,
    @ColorInt bg: Int,
    private val sizePx: Int
) : Drawable() {

    private val paintBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg }
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = sizePx * 0.42f
    }

    private val letter: String =
        name.trim().takeIf { it.isNotEmpty() }?.first()?.uppercase() ?: "?"

    override fun draw(canvas: Canvas) {
        val b = bounds
        val r = minOf(b.width(), b.height()) / 2f
        canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), r, paintBg)
        val cy = b.exactCenterY() - (paintText.descent() + paintText.ascent()) / 2f
        canvas.drawText(letter, b.exactCenterX(), cy, paintText)
    }

    override fun setAlpha(alpha: Int) { paintBg.alpha = alpha; paintText.alpha = alpha }
    override fun setColorFilter(cf: ColorFilter?) { paintBg.colorFilter = cf; paintText.colorFilter = cf }
    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun getIntrinsicWidth(): Int = sizePx
    override fun getIntrinsicHeight(): Int = sizePx
}
