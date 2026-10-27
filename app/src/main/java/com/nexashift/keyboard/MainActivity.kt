 package com.nexashift.keyboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        val p = (20 * d).toInt()
        val white = 0xFFE6F7FF.toInt()
        val prefs = getSharedPreferences("nexa_prefs", MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(p, p * 2, p, p)
            setBackgroundColor(0xFF0B1020.toInt())
        }

        fun label(s: String, size: Float, color: Int, bottom: Int = 8) =
            TextView(this).apply {
                this.text = s
                textSize = size
                setTextColor(color)
                setPadding(0, 0, 0, (bottom * d).toInt())
            }

        fun fmt(ms: Int) = "${ms / 1000}.${(ms % 1000) / 100} s"

        root.addView(label("NEXA SHIFT", 32f, 0xFF22D3EE.toInt()))
        root.addView(label("Type Beyond Ordinary.", 16f, white, 24))
        root.addView(label("Step 1: enable the keyboard in Android settings.", 15f, white))
        root.addView(Button(this).apply {
            text = "1. Enable keyboard"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        root.addView(label("Step 2: choose NEXA SHIFT as your current keyboard.", 15f, white))
        root.addView(Button(this).apply {
            text = "2. Select keyboard"
            setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        })

        // ---- Vanish effect settings ----
        root.addView(label("Vanish effect", 18f, 0xFF22D3EE.toInt()))
        root.addView(Switch(this).apply {
            text = "Keys vanish after a press"
            setTextColor(white)
            isChecked = prefs.getBoolean("vanish_enabled", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("vanish_enabled", checked).apply()
            }
        })
        val startMs = prefs.getInt("vanish_ms", 1000).coerceIn(100, 5000)
        val timeLabel = label("Hidden for: " + fmt(startMs), 15f, white)
        root.addView(timeLabel)
        root.addView(SeekBar(this).apply {
            max = 49
            progress = (startMs - 100) / 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                    val ms = 100 + value * 100
                    timeLabel.text = "Hidden for: " + fmt(ms)
                    if (fromUser) prefs.edit().putInt("vanish_ms", ms).apply()
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        })

        root.addView(label("Step 3: test typing here.", 15f, white))
        root.addView(EditText(this).apply {
            hint = "Type here..."
            setHintTextColor(0xFF7A8AA8.toInt())
            setTextColor(white)
            minLines = 3
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }
}
