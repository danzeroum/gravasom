package com.gravasom.core.audio.engine.edit

/**
 * Serializacao estavel e versionada do projeto (guardada na Room como texto):
 *
 *     v1
 *     bypass=false
 *     intensity=1.0
 *     step|HIGH_PASS|true|80.0
 */
object EditCodec {
    const val VERSION = 1
    private const val HEADER = "v$VERSION"

    fun encode(project: EditProject): String = buildString {
        appendLine(HEADER)
        appendLine("bypass=${project.bypass}")
        appendLine("intensity=${project.intensity}")
        for (s in project.steps) {
            appendLine("step|${s.type.name}|${s.enabled}|${s.params.joinToString(",")}")
        }
    }

    fun decode(text: String): EditProject {
        val lines = text.lines().filter { it.isNotBlank() }
        require(lines.firstOrNull() == HEADER) { "versao de projeto nao suportada: ${lines.firstOrNull()}" }
        var bypass = false
        var intensity = 1f
        val steps = mutableListOf<EditStep>()
        for (line in lines.drop(1)) {
            when {
                line.startsWith("bypass=") -> bypass = line.substringAfter('=').toBooleanStrict()
                line.startsWith("intensity=") -> intensity = line.substringAfter('=').toFloat()
                line.startsWith("step|") -> steps += decodeStep(line)
                else -> throw IllegalArgumentException("linha invalida: $line")
            }
        }
        return EditProject(steps, bypass, intensity)
    }

    private fun decodeStep(line: String): EditStep {
        val parts = line.split('|')
        require(parts.size == STEP_FIELDS) { "passo invalido: $line" }
        val type = requireNotNull(StepType.entries.firstOrNull { it.name == parts[1] }) { "tipo desconhecido: ${parts[1]}" }
        val params = parts[3].split(',').filter { it.isNotBlank() }.map { it.toFloat() }
        return EditStep(type, params, parts[2].toBooleanStrict())
    }

    private const val STEP_FIELDS = 4
}
