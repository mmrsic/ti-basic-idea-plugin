package com.github.mmrsic.idea.plugins.tibasic.toolwindow

data class TiBasicVariableEntry(
    val name: String,
    val type: TiBasicVariableType,
    val occurrences: List<TiBasicVariableOccurrence>,
) {
    val reads: Int get() = occurrences.count { it.accessType == AccessType.READ }
    val writes: Int get() = occurrences.count { it.accessType == AccessType.WRITE }
    val lineNumbers: List<Int> get() = occurrences.map { it.lineNumber }.distinct().sorted()

    val constValue: String? get() {
        if (type !in setOf(TiBasicVariableType.NUMERIC, TiBasicVariableType.STRING)) return null
        if (writes == 0) return if (type == TiBasicVariableType.NUMERIC) "0" else "\"\""
        val constants = occurrences.filter { it.accessType == AccessType.WRITE }.map { it.writtenConstant }
        val first = constants[0] ?: return null
        return if (constants.all { it == first }) first else null
    }
}
