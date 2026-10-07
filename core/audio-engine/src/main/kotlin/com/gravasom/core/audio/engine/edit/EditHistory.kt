package com.gravasom.core.audio.engine.edit

/** Desfazer/refazer sobre projetos imutaveis. Nao toca em disco nem no original. */
class EditHistory(initial: EditProject = EditProject()) {
    var current: EditProject = initial
        private set

    private val undoStack = ArrayDeque<EditProject>()
    private val redoStack = ArrayDeque<EditProject>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun apply(next: EditProject) {
        if (next == current) return
        undoStack.addLast(current)
        redoStack.clear()
        current = next
    }

    fun undo(): EditProject {
        if (undoStack.isEmpty()) return current
        redoStack.addLast(current)
        current = undoStack.removeLast()
        return current
    }

    fun redo(): EditProject {
        if (redoStack.isEmpty()) return current
        undoStack.addLast(current)
        current = redoStack.removeLast()
        return current
    }
}
