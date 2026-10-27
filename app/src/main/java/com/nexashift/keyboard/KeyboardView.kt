package com.nexashift.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

/** Custom-drawn keyboard. Single touch only (stage 1). */
class KeyboardView(context: Context) : View(context) {

    interface Listener {
        fun onText(text: String)
        fun onBackspace()
        fun onEnter()
        fun onGlobe()
        fun onSwitch(target: String)
    }

    var listener: Listener? = null

    /** 0 = off, 1 = next letter only, 2 = caps lock */
    var shiftState = 0
        set(value) { field = value; invalidate() }

    var enterLabel = "\u23CE"
        set(value) { field = value; invalidate() }

    private class Slot(val key: Key, val rect: RectF)

    private var rows: List<List<Key>> = emptyList()
    private var slots: List<Slot> = emptyList()
    private var pressed: Slot? = null
    private var lastShiftTap = 0L

    private val d = resources.displayMetrics.density
    private val keyH = 50f * d
    private val gap = 4f * d
    private val pad = 6f * d
    private val radius = 10f * d

    private val cBg = 0xFF0B1020.toInt()
    private val cKey = 0xFF16203A.toInt()
    private val cSpecial = 0xFF222E52.toInt()
    private val cPressed = 0xFF22D3EE.toInt()
    private val cShift = 0xFF7C3AED.toInt()
    private val cText = 0xFFE6F7FF.toInt()
    private val cTextDark = 0xFF04101A.toInt()

    private val bgPaint = Paint().apply { color = cBg }
    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    private val handler = Handler(Looper.getMainLooper())
    private val repeatBackspace = object : Runnable {
        override fun run() {
            listener?.onBackspace()
            handler.postDelayed(this, 50)
        }
    }

    fun setLayout(newRows: List<List<Key>>) {
        rows = newRows
        pressed = null
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (pad * 2 + rows.size * keyH + (rows.size - 1).coerceAtLeast(0) * gap).toInt()
        setMeasuredDimension(w, h)
        buildSlots(w)
    }

    private fun buildSlots(width: Int) {
        val out = ArrayList<Slot>()
        rows.forEachIndexed { i, row ->
            val total = row.sumOf { it.weight.toDouble() }.toFloat()
            val usable = width - 2 * pad - gap * (row.size - 1)
            var x = pad
            val top = pad + i * (keyH + gap)
            for (key in row) {
                val w = usable * key.weight / total
                out.add(Slot(key, RectF(x, top, x + w, top + keyH)))
                x += w + gap
            }
        }
        slots = out
    }

    private fun labelFor(key: Key): String = when (key.type) {
        KeyType.SHIFT -> if (shiftState == 2) "\u21EA" else "\u21E7"
        KeyType.ENTER -> enterLabel
        KeyType.CHAR ->
            if (shiftState > 0 && key.label.length == 1 && key.label[0] in 'a'..'z') key.label.uppercase()
            else key.label
        else -> key.label
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        for (s in slots) {
            val isPressed = s === pressed
            keyPaint.color = when {
                isPressed -> cPressed
                s.key.type == KeyType.SHIFT && shiftState > 0 -> cShift
                s.key.type == KeyType.CHAR || s.key.type == KeyType.SPACE -> cKey
                else -> cSpecial
            }
            canvas.drawRoundRect(s.rect, radius, radius, keyPaint)

            val label = labelFor(s.key)
            textPaint.color = if (isPressed) cTextDark else cText
            textPaint.textSize = (if (s.key.type == KeyType.CHAR) 22f else 15f) * d
            val maxW = s.rect.width() - 8f * d
            val tw = textPaint.measureText(label)
            if (tw > maxW) textPaint.textSize *= maxW / tw
            val y = s.rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label, s.rect.centerX(), y, textPaint)
        }
    }

    private fun hit(x: Float, y: Float): Slot? {
        val g = gap / 2f
        return slots.firstOrNull {
            x >= it.rect.left - g && x <= it.rect.right + g && y >= it.rect.top - g && y <= it.rect.bottom + g
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressed = hit(e.x, e.y)
                pressed?.let {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    if (it.key.type == KeyType.BACKSPACE) {
                        listener?.onBackspace()
                        handler.postDelayed(repeatBackspace, 400)
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                val h = hit(e.x, e.y)
                if (h !== pressed) {
                    handler.removeCallbacks(repeatBackspace)
                    pressed = h
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(repeatBackspace)
                val s = pressed
                pressed = null
                if (s != null && s.key.type != KeyType.BACKSPACE) perform(s.key)
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(repeatBackspace)
                pressed = null
                invalidate()
            }
        }
        return true
    }

    private fun perform(key: Key) {
        when (key.type) {
            KeyType.CHAR -> {
                val out = if (shiftState > 0 && key.output.length == 1 && key.output[0] in 'a'..'z')
                    key.output.uppercase() else key.output
                listener?.onText(out)
                if (shiftState == 1) shiftState = 0
            }
            KeyType.SPACE -> listener?.onText(" ")
            KeyType.ENTER -> listener?.onEnter()
            KeyType.GLOBE -> listener?.onGlobe()
            KeyType.SWITCH -> key.target?.let { listener?.onSwitch(it) }
            KeyType.SHIFT -> {
                val now = SystemClock.uptimeMillis()
                shiftState = when {
                    shiftState == 2 -> 0
                    shiftState == 1 && now - lastShiftTap < 400 -> 2
                    shiftState == 1 -> 0
                    else -> 1
                }
                lastShiftTap = now
            }
            KeyType.BACKSPACE -> Unit
        }
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(repeatBackspace)
        super.onDetachedFromWindow()
    }
}
