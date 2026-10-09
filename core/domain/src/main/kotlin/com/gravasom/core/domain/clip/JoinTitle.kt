package com.gravasom.core.domain.clip

/** Nome da juncao de gravacoes: duas vira "A e B"; tres ou mais, "Junção de N gravações" (en: "Merge of N recordings"). */
object JoinTitle {

    fun build(titles: List<String>, pt: Boolean): String {
        if (titles.size < COUNT_MIN) return first(titles, pt)
        if (titles.size == COUNT_MIN) {
            val a = titles[0].take(HALF_MAX).trimEnd()
            val b = titles[1].take(HALF_MAX).trimEnd()
            return "$a ${if (pt) "e" else "and"} $b"
        }
        val prefix = if (pt) "Junção de " else "Merge of "
        return prefix + titles.size + (if (pt) " gravações" else " recordings")
    }

    private fun first(titles: List<String>, pt: Boolean) =
        titles.firstOrNull()?.take(ClipTitle.MAX_LENGTH)?.trimEnd() ?: (if (pt) "Junção" else "Merge")

    private const val COUNT_MIN = 2
    private const val HALF_MAX = ClipTitle.MAX_LENGTH / 2
}
