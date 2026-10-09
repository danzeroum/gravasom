package com.gravasom.core.domain.clip

/** Nomes das duas metades de uma gravacao dividida: "<original> · parte 1" e "<original> · parte 2". */
object SplitTitle {

    fun build(original: String, part: Int, pt: Boolean): String {
        val suffix = " · ${if (pt) "parte" else "part"} $part"
        return original.take(ClipTitle.MAX_LENGTH - suffix.length).trimEnd() + suffix
    }
}
