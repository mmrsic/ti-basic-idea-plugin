package com.github.mmrsic.idea.plugins.tibasic.language.analysis.variables

import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicExpression

enum class AccessType { READ, WRITE, NONE }

data class TiBasicVariableOccurrence @JvmOverloads constructor(
    val lineNumber: Int,
    val offset: Int,
    val accessType: AccessType,
    val writtenValue: TiBasicWrittenValue? = null,
    val subscriptExpressions: List<TiBasicExpression> = emptyList(),
) 

sealed interface TiBasicWrittenValue {
    data class Constant(
        val value: String,
    ) : TiBasicWrittenValue

    data class VariableReference @JvmOverloads constructor(
        val name: String,
        val type: TiBasicVariableType,
        val subscriptExpressions: List<TiBasicExpression> = emptyList(),
    ) : TiBasicWrittenValue

    data class ForLoopRange(
        val startExpression: TiBasicExpression?,
        val endExpression: TiBasicExpression?,
        val stepExpression: TiBasicExpression?,
    ) : TiBasicWrittenValue

    data class FixedRange(
        val values: List<String>,
    ) : TiBasicWrittenValue
}
