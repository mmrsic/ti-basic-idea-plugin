package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicExpression

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

    data class ForLoopRange(
        val startExpression: TiBasicExpression?,
        val endExpression: TiBasicExpression?,
        val stepExpression: TiBasicExpression?,
    ) : TiBasicWrittenValue
}
