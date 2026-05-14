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
) {
    val reads: Int get() = occurrences.count { it.accessType == AccessType.READ }
    val writes: Int get() = occurrences.count { it.accessType == AccessType.WRITE }
    val lineNumbers: List<Int> get() = occurrences.map { it.lineNumber }.distinct().sorted()
    val dimensions: String? get() = arrayDetails?.dimensionDisplay
    val optionBase: String? get() = arrayDetails?.optionBase?.toString()
    val dimOccurrences: List<TiBasicVariableOccurrence> get() = occurrences.takeIf { type == TiBasicVariableType.DIM_DECLARATION } ?: emptyList()
    val dimLine: String? get() = lineNumbers.singleOrNull()?.toString()?.takeIf { type == TiBasicVariableType.DIM_DECLARATION }

    val constValue: String?
        get() {
            if (type !in setOf(TiBasicVariableType.NUMERIC, TiBasicVariableType.STRING)) return null
            if (writes == 0) return if (type == TiBasicVariableType.NUMERIC) "0" else "\"\""
            val constants = occurrences.filter { it.accessType == AccessType.WRITE }.map { it.writtenConstant }
            val first = constants[0] ?: return null
            return if (constants.all { it == first }) first else null
        }
}
