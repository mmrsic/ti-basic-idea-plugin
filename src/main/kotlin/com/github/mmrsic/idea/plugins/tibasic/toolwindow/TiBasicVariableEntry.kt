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
    private val resolvedValueRange: List<String>? = null,
) {
    val reads: Int get() = occurrences.count { it.accessType == AccessType.READ }
    val writes: Int get() = occurrences.count { it.accessType == AccessType.WRITE }
    val lineNumbers: List<Int> get() = occurrences.map { it.lineNumber }.distinct().sorted()
    val dimensions: String? get() = arrayDetails?.dimensionDisplay
    val optionBase: String? get() = arrayDetails?.optionBase?.toString()
    val dimLine: String? get() = dimOccurrences.map { it.lineNumber }.distinct().singleOrNull()?.toString()

    val valueRange: List<String>? get() = resolvedValueRange
    val rangeDisplay: String? get() = valueRange?.asDisplaySegments()?.joinToString(RANGE_VALUE_SEPARATOR)
    val constValue: String? get() = valueRange?.singleOrNull()
}

private fun List<String>.asDisplaySegments(): List<String> {
    val displaySegments = mutableListOf<String>()
    var numericRunStart: Int? = null
    var previousNumericValue: Int? = null

    fun flushNumericRun() {
        val runStartIndex = numericRunStart ?: return
        val runValues = subList(runStartIndex, previousNumericValueIndex(runStartIndex, previousNumericValue))
        displaySegments += runValues.toDisplaySegment()
        numericRunStart = null
        previousNumericValue = null
    }

    forEachIndexed { index, value ->
        val numericValue = value.toIntOrNull()
        when {
            numericValue == null -> {
                flushNumericRun()
                displaySegments += value
            }

            numericRunStart == null -> {
                numericRunStart = index
                previousNumericValue = numericValue
            }

            numericValue == previousNumericValue?.plus(1) -> previousNumericValue = numericValue
            else -> {
                flushNumericRun()
                numericRunStart = index
                previousNumericValue = numericValue
            }
        }
    }
    flushNumericRun()
    return displaySegments
}

private fun List<String>.previousNumericValueIndex(
    runStartIndex: Int,
    previousNumericValue: Int?,
): Int {
    if (previousNumericValue == null) return runStartIndex
    val runLength = previousNumericValue - this[runStartIndex].toInt() + 1
    return runStartIndex + runLength
}

private fun List<String>.toDisplaySegment(): String =
    if (size >= 3) "${first()}$RANGE_DISPLAY_SEPARATOR${last()}" else joinToString(RANGE_VALUE_SEPARATOR)

private const val RANGE_VALUE_SEPARATOR = ", "
private const val RANGE_DISPLAY_SEPARATOR = "-"
