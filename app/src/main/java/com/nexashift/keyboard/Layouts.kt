package com.nexashift.keyboard

enum class KeyType { CHAR, SHIFT, BACKSPACE, ENTER, SPACE, SWITCH, GLOBE }

data class Key(
    val label: String,
    val type: KeyType = KeyType.CHAR,
    val weight: Float = 1f,
    val target: String? = null,
    val output: String = label
)

/** All keyboard layouts. Every layout has exactly 5 rows so the keyboard height never jumps. */
object Layouts {
    const val SIGN_BASE = "\u25CC" // dotted circle shown under combining signs

    private fun letters(s: String) = s.split(" ").map { Key(it) }
    private fun signs(s: String) = s.split(" ").map { Key(SIGN_BASE + it, output = it) }
    private fun chars(s: String) = s.map { Key(it.toString()) }
    private fun backspace(w: Float = 1.5f) = Key("\u232B", KeyType.BACKSPACE, w)

    private fun bottom(switchLabel: String, target: String, punct: String) = listOf(
        Key(switchLabel, KeyType.SWITCH, 1.5f, target),
        Key("\uD83C\uDF10", KeyType.GLOBE, 1.2f),
        Key("space", KeyType.SPACE, 4f),
        Key(punct, KeyType.CHAR, 1f),
        Key("\u23CE", KeyType.ENTER, 1.5f)
    )

    private val en = listOf(
        chars("1234567890"),
        chars("qwertyuiop"),
        chars("asdfghjkl"),
        listOf(Key("\u21E7", KeyType.SHIFT, 1.5f)) + chars("zxcvbnm") + backspace(),
        bottom("?123", "sym", ".")
    )

    private val sym = listOf(
        chars("1234567890"),
        chars("@#\$%&-+()/"),
        letters("\u09F3 \u20B9 \u20AC \u00A3 * \" ' : ; !"),
        letters("? , . _ = < >") + backspace(3f),
        bottom("ABC", "back", ".")
    )

    private val bn1 = listOf(
        letters("ক খ গ ঘ ঙ চ ছ জ ঝ ঞ"),
        letters("ট ঠ ড ঢ ণ ত থ দ ধ ন"),
        letters("প ফ ব ভ ম য র ল শ ষ"),
        letters("স হ \u09DC \u09DD \u09DF \u09CE") + signs("\u0982 \u0983 \u0981") + backspace(1f),
        bottom("অ আ", "bn2", "\u0964")
    )

    private val bn2 = listOf(
        letters("অ আ ই ঈ উ ঊ ঋ এ ঐ ও"),
        letters("ঔ") + signs("\u09BE \u09BF \u09C0 \u09C1 \u09C2 \u09C3 \u09C7 \u09C8 \u09CB"),
        signs("\u09CC \u09CD \u09BC \u09D7") + letters("- , ? !") + backspace(2f),
        chars("০১২৩৪৫৬৭৮৯"),
        bottom("ক খ", "bn1", "\u0964")
    )

    private val hi1 = listOf(
        letters("क ख ग घ ङ च छ ज झ ञ"),
        letters("ट ठ ड ढ ण त थ द ध न"),
        letters("प फ ब भ म य र ल व श"),
        letters("ष स ह \u0915\u094D\u0937 \u0924\u094D\u0930 \u091C\u094D\u091E \u0933 \u095C \u095D") + backspace(1f),
        bottom("अ आ", "hi2", "\u0964")
    )

    private val hi2 = listOf(
        letters("अ आ इ ई उ ऊ ऋ ए ऐ ओ"),
        letters("औ") + signs("\u093E \u093F \u0940 \u0941 \u0942 \u0943 \u0947 \u0948 \u094B"),
        signs("\u094C \u0902 \u0903 \u0901 \u093C \u094D \u0949") + backspace(3f),
        chars("०१२३४५६७८९"),
        bottom("क ख", "hi1", "\u0964")
    )

    val all: Map<String, List<List<Key>>> = mapOf(
        "en" to en, "sym" to sym, "bn1" to bn1, "bn2" to bn2, "hi1" to hi1, "hi2" to hi2
    )

    fun get(id: String): List<List<Key>> = all[id] ?: en
}
