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
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        val p = (20 * d).toInt()

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

        root.addView(label("NEXA SHIFT", 32f, 0xFF22D3EE.toInt()))
        root.addView(label("Type Beyond Ordinary.", 16f, 0xFFE6F7FF.toInt(), 24))
        root.addView(label("Step 1: enable the keyboard in Android settings.", 15f, 0xFFE6F7FF.toInt()))
        root.addView(Button(this).apply {
            text = "1. Enable keyboard"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        root.addView(label("Step 2: choose NEXA SHIFT as your current keyboard.", 15f, 0xFFE6F7FF.toInt()))
        root.addView(Button(this).apply {
            text = "2. Select keyboard"
            setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        })
        root.addView(label("Step 3: test typing here (English, Bengali, Hindi).", 15f, 0xFFE6F7FF.toInt()))
        root.addView(EditText(this).apply {
            hint = "Type here..."
            setHintTextColor(0xFF7A8AA8.toInt())
            setTextColor(0xFFE6F7FF.toInt())
            minLines = 3
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }
}
