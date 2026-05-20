package com.github.mmrsic.idea.plugins.tibasic.toolwindow

data class TiBasicArrayDetails(
    val dimensions: List<String>,
    val optionBase: Int,
) {
    val dimensionDisplay: String get() = dimensions.joinToString(",")
}

data class TiBasicVariableEntry(
    val name: String,
    val type: TiBasicVariableType,
    val occurrences: List<TiBasicVariableOccurrence>,
    val arrayDetails: TiBasicArrayDetails? = null,
    val dimOccurrences: List<TiBasicVariableOccurrence> = emptyList(),
    private val resolvedConstValue: String? = null,
) {
    val reads: Int get() = occurrences.count { it.accessType == AccessType.READ }
    val writes: Int get() = occurrences.count { it.accessType == AccessType.WRITE }
    val lineNumbers: List<Int> get() = occurrences.map { it.lineNumber }.distinct().sorted()
    val dimensions: String? get() = arrayDetails?.dimensionDisplay
    val optionBase: String? get() = arrayDetails?.optionBase?.toString()
    val dimLine: String? get() = dimOccurrences.map { it.lineNumber }.distinct().singleOrNull()?.toString()

    val constValue: String? get() = resolvedConstValue
}
