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

/** Custom-drawn keyboard. Single touch only. Keys can vanish briefly after a press. */
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

    private class Slot(val key: Key, val rect: RectF) {
        var hideStart = 0L
        var holdMs = 0L
    }

    private var rows: List<List<Key>> = emptyList()
    private var slots: List<Slot> = emptyList()
    private var pressed: Slot? = null
    private var lastShiftTap = 0L

    private val prefs = context.getSharedPreferences("nexa_prefs", Context.MODE_PRIVATE)
    private val outMs = 150L
    private val inMs = 150L

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

    /** 1 = fully visible, 0 = fully vanished */
    private fun visibility(s: Slot, now: Long): Float {
        if (s.hideStart == 0L) return 1f
        val t = now - s.hideStart
        return when {
            t < 0 -> 1f
            t < outMs -> 1f - t / outMs.toFloat()
            t < outMs + s.holdMs -> 0f
            t < outMs + s.holdMs + inMs -> (t - outMs - s.holdMs) / inMs.toFloat()
            else -> { s.hideStart = 0L; 1f }
        }
    }

    private fun isBusy(s: Slot, now: Long): Boolean =
        s.hideStart != 0L && now - s.hideStart < outMs + s.holdMs + inMs

    private fun startVanish(s: Slot) {
        if (!prefs.getBoolean("vanish_enabled", true)) return
        s.holdMs = prefs.getInt("vanish_ms", 1000).toLong()
        s.hideStart = SystemClock.uptimeMillis()
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        val now = SystemClock.uptimeMillis()
        var animating = false
        for (s in slots) {
            val v = visibility(s, now)
            if (s.hideStart != 0L) animating = true
            if (v <= 0f) continue

            val isPressed = s === pressed
            keyPaint.color = when {
                isPressed -> cPressed
                s.key.type == KeyType.SHIFT && shiftState > 0 -> cShift
                s.key.type == KeyType.CHAR || s.key.type == KeyType.SPACE -> cKey
                else -> cSpecial
            }
            keyPaint.alpha = (255 * v).toInt()

            canvas.save()
            canvas.scale(v, v, s.rect.centerX(), s.rect.centerY())
            canvas.drawRoundRect(s.rect, radius, radius, keyPaint)

            val label = labelFor(s.key)
            textPaint.color = if (isPressed) cTextDark else cText
            textPaint.alpha = (255 * v).toInt()
            textPaint.textSize = (if (s.key.type == KeyType.CHAR) 22f else 15f) * d
            val maxW = s.rect.width() - 8f * d
            val tw = textPaint.measureText(label)
            if (tw > maxW) textPaint.textSize *= maxW / tw
            val y = s.rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(label, s.rect.centerX(), y, textPaint)
            canvas.restore()
        }
        if (animating) postInvalidateOnAnimation()
    }

    private fun hit(x: Float, y: Float): Slot? {
        val g = gap / 2f
        val now = SystemClock.uptimeMillis()
        return slots.firstOrNull {
            !isBusy(it, now) &&
                x >= it.rect.left - g && x <= it.rect.right + g &&
                y >= it.rect.top - g && y <= it.rect.bottom + g
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
                if (s != null) {
                    if (s.key.type != KeyType.BACKSPACE) perform(s.key)
                    startVanish(s)
                }
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
