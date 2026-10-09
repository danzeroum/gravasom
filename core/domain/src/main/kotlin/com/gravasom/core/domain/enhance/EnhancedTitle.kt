package com.gravasom.core.domain.enhance

/** Nome da copia melhorada: "Aula de fisica (melhorada)" / "Lecture (enhanced)". Sem duplicar se ja for uma copia melhorada. */
object EnhancedTitle {
    private const val PT = "melhorada"
    private const val EN = "enhanced"

    fun build(original: String, pt: Boolean): String {
        val base = original.trim().removeSuffix(" ($PT)").removeSuffix(" ($EN)")
        return "$base (${if (pt) PT else EN})"
    }
}
