package com.github.mmrsic.idea.plugins.tibasic.toolwindow

data class TiBasicArrayDetails(
    val dimensions: List<String>,
    val optionBase: Int,
) {
    val dimensionDisplay: String get() = dimensions.joinToString(",")
    fun declarationDisplay(name: String): String =
        dimensions.joinToString(
            separator = ",",
            prefix = "DIM $name(",
            postfix = ")",
        ) { dimension -> "$optionBase$RANGE_DISPLAY_SEPARATOR$dimension" }
}

data class TiBasicVariableDimensionsDisplay(
    val occurrences: List<TiBasicVariableOccurrence>,
    val text: String,
)

data class TiBasicVariableEntry(
    val name: String,
    val type: TiBasicVariableType,
    val occurrences: List<TiBasicVariableOccurrence>,
    val arrayDetails: TiBasicArrayDetails? = null,
    val dimOccurrences: List<TiBasicVariableOccurrence> = emptyList(),
    private val resolvedValueRange: List<String>? = null,
    private val resolvedArrayElementRanges: Map<List<Int>, List<String>> = emptyMap(),
) {
    val reads: Int get() = occurrences.count { it.accessType == AccessType.READ }
    val writes: Int get() = occurrences.count { it.accessType == AccessType.WRITE }
    val lineNumbers: List<Int> get() = occurrences.map { it.lineNumber }.distinct().sorted()
    val dimensions: String? get() = arrayDetails?.dimensionDisplay
    val optionBase: String? get() = arrayDetails?.optionBase?.toString()
    val dimLine: String? get() = dimOccurrences.map { it.lineNumber }.distinct().singleOrNull()?.toString()
    val dimensionsDisplay: TiBasicVariableDimensionsDisplay?
        get() = arrayDetails?.let { details ->
            TiBasicVariableDimensionsDisplay(
                occurrences = dimOccurrences.distinctBy(TiBasicVariableOccurrence::lineNumber).sortedBy(TiBasicVariableOccurrence::lineNumber),
                text = details.declarationDisplay(name),
            )
        }

    val valueRange: List<String>? get() = resolvedValueRange?.sortedRangeValues()
    val rangeDisplay: String? get() = valueRange?.toRangeDisplayIfWithin(displayItemLimit, keyStatusInterval())
    val constValue: String? get() = valueRange?.singleOrNull()
    val arrayElementConstantsDisplay: String?
        get() = resolvedArrayElementRanges
            .takeIf(Map<List<Int>, List<String>>::isNotEmpty)
            ?.entries
            ?.sortedWith(arrayElementEntryComparator)
            ?.toCompactArrayElementDisplays()
            ?.takeIf { it.size <= displayItemLimit }
            ?.joinToString(ARRAY_ELEMENT_SEPARATOR)

    fun rangeDisplay(showArrayElementConstants: Boolean): String? =
        rangeDisplay ?: arrayElementConstantsDisplay.takeIf { showArrayElementConstants }

    private fun keyStatusInterval(): IntRange? =
        occurrences
            .mapNotNull(TiBasicVariableOccurrence::writtenValue)
            .filterIsInstance<TiBasicWrittenValue.FixedRange>()
            .firstOrNull { it.values == KEY_STATUS_INTERVAL_VALUES }
            ?.let { KEY_STATUS_INTERVAL_START..KEY_STATUS_INTERVAL_END }

    private val displayItemLimit: Int
        get() = when (type) {
            TiBasicVariableType.NUMERIC, TiBasicVariableType.NUMERIC_ARRAY -> MAX_NUMERIC_DISPLAY_ITEMS
            TiBasicVariableType.STRING, TiBasicVariableType.STRING_ARRAY -> MAX_STRING_DISPLAY_ITEMS
            TiBasicVariableType.USER_FUNCTION -> 0
        }

    fun arrayElementValueRange(subscripts: List<Int>): List<String>? =
        resolvedArrayElementRanges[subscripts]?.sortedRangeValues()

    fun arrayElementConstValue(subscripts: List<Int>): String? =
        arrayElementValueRange(subscripts)?.singleOrNull()
}

internal fun List<String>.sortedRangeValues(): List<String> =
    sortedWith(compareBy({ it.toIntOrNull() == null }, { it.toIntOrNull() ?: 0 }, { it }))

internal fun List<String>.toRangeDisplay(): String =
    sortedRangeValues().toDisplayString()

private fun List<String>.toRangeDisplayIfWithin(
    maxDisplayItems: Int,
    preferredInterval: IntRange?,
): String? =
    sortedRangeValues()
        .let { sortedValues ->
            sortedValues.toDisplayString(preferredInterval)
                .takeIf { sortedValues.displayItemCount(preferredInterval) <= maxDisplayItems }
        }

private fun List<String>.toDisplayString(preferredInterval: IntRange? = null): String =
    when {
        preferredInterval != null -> toDisplaySegments(preferredInterval).joinToString(RANGE_VALUE_SEPARATOR)
        this == KEY_STATUS_INTERVAL_VALUES -> "$RANGE_PREFIX${first()}$INTERVAL_VALUE_SEPARATOR${last()}$RANGE_POSTFIX"
        else -> asDisplaySegments().joinToString(RANGE_VALUE_SEPARATOR)
    }

private fun List<String>.displayItemCount(preferredInterval: IntRange? = null): Int =
    when {
        preferredInterval != null -> toDisplaySegments(preferredInterval).size
        this == KEY_STATUS_INTERVAL_VALUES -> 1
        else -> asDisplaySegments().size
    }

private fun List<String>.toDisplaySegments(preferredInterval: IntRange): List<String> {
    val numericValues = mapNotNull(String::toIntOrNull)
    if (numericValues.size != size) return asDisplaySegments()

    var intervalStart = preferredInterval.first
    var intervalEnd = preferredInterval.last
    val remainingLower = mutableListOf<String>()
    val remainingUpper = mutableListOf<String>()

    numericValues.forEach { value ->
        when {
            value < intervalStart - 1 -> remainingLower += value.toString()
            value == intervalStart - 1 -> intervalStart = value
            value in intervalStart..intervalEnd -> Unit
            value == intervalEnd + 1 -> intervalEnd = value
            else -> remainingUpper += value.toString()
        }
    }

    return buildList {
        addAll(remainingLower.sortedRangeValues().asDisplaySegments())
        add("$RANGE_PREFIX$intervalStart$INTERVAL_VALUE_SEPARATOR$intervalEnd$RANGE_POSTFIX")
        addAll(remainingUpper.sortedRangeValues().asDisplaySegments())
    }
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
    if (size >= 3) {
        "$RANGE_PREFIX${first()}$INTERVAL_VALUE_SEPARATOR${last()}$RANGE_POSTFIX"
    } else {
        joinToString(RANGE_VALUE_SEPARATOR)
    }

private fun List<Int>.asSubscriptDisplay(): String = "(${joinToString(",")})"

private fun List<Map.Entry<List<Int>, List<String>>>.toCompactArrayElementDisplays(): List<String> {
    val displays = mutableListOf<String>()
    var currentRunStartEntryIndex: Int? = null
    var currentRunEndEntryIndex: Int? = null
    var currentRunStartSubscript: Int? = null
    var currentRunEndSubscript: Int? = null
    var currentRunRange: List<String>? = null

    fun flushRun() {
        val runStartEntryIndex = currentRunStartEntryIndex ?: return
        val runEndEntryIndex = currentRunEndEntryIndex ?: return
        val runStartSubscript = currentRunStartSubscript ?: return
        val runEndSubscript = currentRunEndSubscript ?: return
        val range = currentRunRange ?: return
        if (runEndSubscript - runStartSubscript + 1 >= MIN_ARRAY_ELEMENT_RANGE_LENGTH) {
            displays += "(${runStartSubscript}$RANGE_DISPLAY_SEPARATOR$runEndSubscript)=${range.toRangeDisplay()}"
        } else {
            subList(runStartEntryIndex, runEndEntryIndex + 1).forEach { (subscripts, elementRange) ->
                displays += "${subscripts.asSubscriptDisplay()}=${elementRange.toRangeDisplay()}"
            }
        }
        currentRunStartEntryIndex = null
        currentRunEndEntryIndex = null
        currentRunStartSubscript = null
        currentRunEndSubscript = null
        currentRunRange = null
    }

    forEachIndexed { entryIndex, (subscripts, range) ->
        val subscript = subscripts.singleOrNull()
        if (subscript == null) {
            flushRun()
            displays += "${subscripts.asSubscriptDisplay()}=${range.toRangeDisplay()}"
            return@forEachIndexed
        }

        when {
            currentRunStartEntryIndex == null -> {
                currentRunStartEntryIndex = entryIndex
                currentRunEndEntryIndex = entryIndex
                currentRunStartSubscript = subscript
                currentRunEndSubscript = subscript
                currentRunRange = range
            }

            currentRunRange == range && subscript == currentRunEndSubscript?.plus(1) -> {
                currentRunEndEntryIndex = entryIndex
                currentRunEndSubscript = subscript
            }

            else -> {
                flushRun()
                currentRunStartEntryIndex = entryIndex
                currentRunEndEntryIndex = entryIndex
                currentRunStartSubscript = subscript
                currentRunEndSubscript = subscript
                currentRunRange = range
            }
        }
    }
    flushRun()
    return displays
}

private val arraySubscriptComparator = Comparator<List<Int>> { left, right ->
    val sharedSize = minOf(left.size, right.size)
    for (index in 0 until sharedSize) {
        val comparison = left[index].compareTo(right[index])
        if (comparison != 0) return@Comparator comparison
    }
    left.size.compareTo(right.size)
}

private val arrayElementEntryComparator = Comparator<Map.Entry<List<Int>, List<String>>> { left, right ->
    arraySubscriptComparator.compare(left.key, right.key)
}

private const val RANGE_VALUE_SEPARATOR = ", "
private const val RANGE_DISPLAY_SEPARATOR = "-"
private const val INTERVAL_VALUE_SEPARATOR = "; "
private const val ARRAY_ELEMENT_SEPARATOR = "; "
private const val MIN_ARRAY_ELEMENT_RANGE_LENGTH = 3
private const val RANGE_PREFIX = "["
private const val RANGE_POSTFIX = "]"
private const val MAX_NUMERIC_DISPLAY_ITEMS = 20
private const val MAX_STRING_DISPLAY_ITEMS = 10
private val KEY_STATUS_INTERVAL_VALUES = listOf("-1", "1")
private const val KEY_STATUS_INTERVAL_START = -1
private const val KEY_STATUS_INTERVAL_END = 1
