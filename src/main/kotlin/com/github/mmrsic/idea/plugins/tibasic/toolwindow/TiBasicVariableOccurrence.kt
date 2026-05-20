package com.github.mmrsic.idea.plugins.tibasic.toolwindow

enum class AccessType { READ, WRITE, NONE }

data class TiBasicVariableOccurrence(
    val lineNumber: Int,
    val offset: Int,
    val accessType: AccessType,
    val writtenValue: TiBasicWrittenValue? = null,
)

sealed interface TiBasicWrittenValue {
    data class Constant(
        val value: String,
    ) : TiBasicWrittenValue

    data class VariableReference(
        val name: String,
        val type: TiBasicVariableType,
    ) : TiBasicWrittenValue
}
