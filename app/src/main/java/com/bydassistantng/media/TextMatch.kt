package com.bydassistantng.media

import java.text.Normalizer
import java.util.Locale

/** Comparing what a person said with what an app shows: names, search phrases. */
internal object TextMatch {
    /**
     * A Polish letter as the plain Latin one it is written with when the accents are left off. NFKC keeps these letters
     * whole ("ł" has no decomposition at all), so "Łódź" and "lodz" would not match without it. Any other letter is
     * returned as it is.
     */
    fun foldPolish(c: Char): Char = when (c) {
        'ą' -> 'a'
        'ć' -> 'c'
        'ę' -> 'e'
        'ł' -> 'l'
        'ń' -> 'n'
        'ó' -> 'o'
        'ś' -> 's'
        'ź', 'ż' -> 'z'
        else -> c
    }

    /**
     * A Spanish letter as the plain Latin one it is written with when the accents are left off ("cámara" and "camara",
     * "año" and "ano"). NFKC keeps these letters whole too, so they need the same fold as the Polish ones. "ó" is already
     * folded by [foldPolish]. Any other letter is returned as it is.
     */
    fun foldSpanish(c: Char): Char = when (c) {
        'á' -> 'a'
        'é' -> 'e'
        'í' -> 'i'
        'ú', 'ü' -> 'u'
        'ñ' -> 'n'
        else -> c
    }

    /** The plain Latin letter for a Polish or Spanish accented one, so a name matches with or without its accents. */
    fun foldLatin(c: Char): Char = foldSpanish(foldPolish(c))

    /** Lower case, letters and digits only, one space between words; marks that sit on letters (Arabic) are dropped. */
    fun normalize(text: String): String {
        val out = StringBuilder()
        var pendingSpace = false
        for (ch in Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT)) {
            when {
                Character.getType(ch) == Character.NON_SPACING_MARK.toInt() -> Unit
                Character.isLetterOrDigit(ch) -> {
                    if (pendingSpace && out.isNotEmpty()) out.append(' ')
                    pendingSpace = false
                    out.append(foldLatin(ch))
                }
                else -> pendingSpace = true
            }
        }
        return out.toString()
    }

    /** Whether the two say the same thing: equal once normalized, or one holds the other. */
    fun same(a: String, b: String): Boolean {
        val x = normalize(a)
        val y = normalize(b)
        return x.isNotEmpty() && y.isNotEmpty() && (x == y || x.contains(y) || y.contains(x))
    }

    /** How well [title] fits what was [wanted] (both normalized): 0 for not at all, 100 for exactly. */
    fun score(title: String, wanted: String): Int {
        if (title.isEmpty() || wanted.isEmpty()) return 0
        if (title == wanted) return 100
        if (title.startsWith(wanted) || wanted.startsWith(title)) return 80
        if (title.contains(wanted) || wanted.contains(title)) return 60
        val titleWords = title.split(' ').toSet()
        val wantedWords = wanted.split(' ').filter { it.isNotEmpty() }
        val hits = wantedWords.count { it in titleWords }
        return when {
            wantedWords.isEmpty() || hits == 0 -> 0
            hits == wantedWords.size -> 50
            hits * 2 >= wantedWords.size -> 30
            else -> 0
        }
    }
}
