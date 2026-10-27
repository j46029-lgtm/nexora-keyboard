package com.nexashift.keyboard

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo

class NexaImeService : InputMethodService(), KeyboardView.Listener {

    private var kv: KeyboardView? = null
    private val languages = listOf("en", "bn1", "hi1")
    private var langIndex = 0
    private var current = "en"

    override fun onCreateInputView(): View {
        val v = KeyboardView(this)
        v.listener = this
        v.setLayout(Layouts.get(current))
        kv = v
        return v
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val cls = (info?.inputType ?: 0) and InputType.TYPE_MASK_CLASS
        val numeric = cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE
        val target = if (numeric) "sym" else if (current == "sym") languages[langIndex] else current
        if (target != current) show(target)
        kv?.shiftState = 0
        kv?.enterLabel = enterLabelFor(info)
    }

    private fun show(id: String) {
        current = id
        kv?.setLayout(Layouts.get(id))
    }

    private fun enterLabelFor(info: EditorInfo?): String {
        if (info == null) return "\u23CE"
        val multiline = (info.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        if (multiline || (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) return "\u23CE"
        return when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_GO -> "Go"
            EditorInfo.IME_ACTION_SEARCH -> "\uD83D\uDD0D"
            EditorInfo.IME_ACTION_SEND -> "Send"
            EditorInfo.IME_ACTION_NEXT -> "Next"
            EditorInfo.IME_ACTION_DONE -> "Done"
            else -> "\u23CE"
        }
    }

    // ---- KeyboardView.Listener (nothing typed is stored or logged) ----

    override fun onText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun onBackspace() {
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
    }

    override fun onEnter() {
        val ei = currentInputEditorInfo
        val ic = currentInputConnection ?: return
        if (ei == null) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
            return
        }
        val action = ei.imeOptions and EditorInfo.IME_MASK_ACTION
        val multiline = (ei.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        val noAction = (ei.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0
        if (multiline || noAction ||
            action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED
        ) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
        } else {
            ic.performEditorAction(action)
        }
    }

    override fun onGlobe() {
        langIndex = (langIndex + 1) % languages.size
        show(languages[langIndex])
    }

    override fun onSwitch(target: String) {
        show(if (target == "back") languages[langIndex] else target)
    }
}
