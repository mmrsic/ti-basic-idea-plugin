package com.github.mmrsic.idea.plugins.tibasic.toolwindow

data class TiBasicVariableEntry(
    val name: String,
    val type: TiBasicVariableType,
    val occurrences: List<TiBasicVariableOccurrence>,
) {
    val reads: Int get() = occurrences.count { it.accessType == AccessType.READ }
    val writes: Int get() = occurrences.count { it.accessType == AccessType.WRITE }
    val lineNumbers: List<Int> get() = occurrences.map { it.lineNumber }.distinct().sorted()
}
