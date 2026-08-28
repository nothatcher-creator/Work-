package com.github.sevagh.demucs_android

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.ceil
import kotlin.math.max

class BassTabView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    companion object {
        const val SYSTEM_SECONDS = 10.0
        private const val SYSTEM_H = 184f
        private const val TOP_PAD = 26f
        private const val LEFT = 54f
        private const val RIGHT = 18f
    }

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(126, 136, 151); strokeWidth = 2f }
    private val faint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(58, 66, 78); strokeWidth = 1f }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 26f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(186, 197, 214); textSize = 18f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val timeText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(132, 145, 165); textSize = 14f }
    private val tech = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(230, 235, 243); textSize = 17f; textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val noteBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(19, 24, 31) }
    private val green = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(50, 220, 117); strokeWidth = 4f }
    private val bg = Paint().apply { color = Color.rgb(11, 15, 21) }

    private var notes: List<TabNote> = emptyList()
    private var durationSec = 0.0
    private var playheadSec = 0.0

    fun setTab(notes: List<TabNote>, duration: Double) {
        this.notes = notes
        durationSec = max(duration, notes.maxOfOrNull { it.endSec } ?: 0.0)
        requestLayout(); invalidate()
    }

    fun setPlayhead(seconds: Double) {
        playheadSec = seconds.coerceAtLeast(0.0)
        invalidate()
    }

    fun playheadY(): Int {
        val system = (playheadSec / SYSTEM_SECONDS).toInt()
        return (TOP_PAD + system * SYSTEM_H + SYSTEM_H / 2f).toInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec).coerceAtLeast(320)
        val systems = max(1, ceil(max(1.0, durationSec) / SYSTEM_SECONDS).toInt())
        val h = (TOP_PAD * 2 + systems * SYSTEM_H).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
        val usable = width - LEFT - RIGHT
        val systems = max(1, ceil(max(1.0, durationSec) / SYSTEM_SECONDS).toInt())
        val names = arrayOf("G", "D", "A", "E")

        for (sys in 0 until systems) {
            val y0 = TOP_PAD + sys * SYSTEM_H
            val stringTop = y0 + 46f
            val gap = 27f
            val startT = sys * SYSTEM_SECONDS
            canvas.drawText(formatTime(startT), LEFT, y0 + 19f, timeText)

            for (r in 0..3) {
                val y = stringTop + r * gap
                canvas.drawLine(LEFT, y, width - RIGHT, y, line)
                canvas.drawText(names[r], 25f, y + 7f, label)
            }
            for (beat in 0..5) {
                val x = LEFT + usable * beat / 5f
                canvas.drawLine(x, stringTop - 12f, x, stringTop + 3 * gap + 12f, faint)
            }

            val endT = startT + SYSTEM_SECONDS
            for (n in notes) {
                if (n.startSec < startT || n.startSec >= endT) continue
                val x = LEFT + ((n.startSec - startT) / SYSTEM_SECONDS * usable).toFloat()
                val row = 3 - n.stringIndex
                val y = stringTop + row * gap
                val s = n.fret.toString()
                val tw = text.measureText(s)
                val box = RectF(x - tw / 2f - 7f, y - 19f, x + tw / 2f + 7f, y + 9f)
                canvas.drawRoundRect(box, 7f, 7f, noteBg)
                canvas.drawText(s, x, y + 8f, text)
                if (n.technique.isNotEmpty()) canvas.drawText(n.technique, x - 17f, y - 16f, tech)
            }

            if (playheadSec >= startT && playheadSec < endT) {
                val x = LEFT + ((playheadSec - startT) / SYSTEM_SECONDS * usable).toFloat()
                canvas.drawLine(x, stringTop - 19f, x, stringTop + 3 * gap + 19f, green)
            }
        }
    }

    private fun formatTime(sec: Double): String {
        val s = sec.toInt()
        return "%d:%02d".format(s / 60, s % 60)
    }
}
