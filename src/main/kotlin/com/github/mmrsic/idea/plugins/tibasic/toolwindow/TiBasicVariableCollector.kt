package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.ext.childrenAfter
import com.github.mmrsic.idea.plugins.tibasic.ext.firstChildOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.lang.CallArgAccess
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicCallSubprograms
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicForStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicInputStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicLetStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicNextStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicReadStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

private const val DEFAULT_ARRAY_DIMENSION = 10
private const val DEFAULT_OPTION_BASE = 0

object TiBasicVariableCollector {

    fun collect(file: TiBasicFile): List<TiBasicVariableEntry> {
        val arrayDetailsByName = collectArrayDetailsByName(file)
        val result = mutableListOf<TiBasicVariableEntry>()
        result += collectDimDeclarations(file, arrayDetailsByName)
        result += collectUserFunctions(file)
        result += collectRegularVariables(file, arrayDetailsByName)
        return result.sortedWith(compareBy({ it.name }, { it.type.ordinal }))
    }

    fun collectCached(file: TiBasicFile): List<TiBasicVariableEntry> =
        CachedValuesManager.getCachedValue(file) {
            CachedValueProvider.Result.create(collect(file), file)
        }

    private fun collectDimDeclarations(
        file: TiBasicFile,
        arrayDetailsByName: Map<String, TiBasicArrayDetails>,
    ): List<TiBasicVariableEntry> =
        file.dimStatements().flatMap { dimStmt ->
            dimStmt.dimVariableAccesses().mapNotNull { varAccess ->
                val name = varAccess.node.firstChildNode?.text?.uppercase() ?: return@mapNotNull null
                val line = varAccess.containingTiBasicLine() ?: return@mapNotNull null
                TiBasicVariableEntry(
                    name = name,
                    type = TiBasicVariableType.DIM_DECLARATION,
                    occurrences = listOf(TiBasicVariableOccurrence(line.lineNumber(), varAccess.textOffset, AccessType.NONE)),
                    arrayDetails = arrayDetailsByName[name],
                )
            }
        }

    private fun collectUserFunctions(file: TiBasicFile): List<TiBasicVariableEntry> =
        file.defStatements().mapNotNull { defStmt ->
            val nameNode = defStmt.functionNameNode() ?: return@mapNotNull null
            val name = nameNode.text.uppercase()
            val line = defStmt.containingTiBasicLine() ?: return@mapNotNull null
            TiBasicVariableEntry(
                name = name,
                type = TiBasicVariableType.USER_FUNCTION,
                occurrences = listOf(TiBasicVariableOccurrence(line.lineNumber(), nameNode.startOffset, AccessType.NONE)),
            )
        }

    private fun collectRegularVariables(
        file: TiBasicFile,
        arrayDetailsByName: Map<String, TiBasicArrayDetails>,
    ): List<TiBasicVariableEntry> {
        val dimAccesses = file.dimStatements().flatMap { it.dimVariableAccesses() }.toSet()
        val grouped = mutableMapOf<Pair<String, TiBasicVariableType>, MutableList<TiBasicVariableOccurrence>>()

        for (varAccess in file.variableAccesses()) {
            if (varAccess in dimAccesses) continue
            val nameNode = varAccess.node.firstChildNode ?: continue
            if (nameNode.elementType == TiBasicTokenTypes.INVALID_VARIABLE_NAME) continue

            val name = nameNode.text.uppercase()
            val isArray = varAccess.hasSubscriptParens()
            val type = when {
                nameNode.elementType == TiBasicTokenTypes.STRING_VARIABLE && isArray -> TiBasicVariableType.STRING_ARRAY
                nameNode.elementType == TiBasicTokenTypes.STRING_VARIABLE -> TiBasicVariableType.STRING
                isArray -> TiBasicVariableType.NUMERIC_ARRAY
                else -> TiBasicVariableType.NUMERIC
            }

            val line = varAccess.containingTiBasicLine() ?: continue
            val accessType = determineAccessType(varAccess)
            val writtenConstant = if (accessType == AccessType.WRITE) extractLetConstant(varAccess) else null
            val occurrence = TiBasicVariableOccurrence(line.lineNumber(), nameNode.startOffset, accessType, writtenConstant)
            grouped.getOrPut(Pair(name, type)) { mutableListOf() }.add(occurrence)
        }

        return grouped.map { (key, occurrences) ->
            TiBasicVariableEntry(
                name = key.first,
                type = key.second,
                occurrences = occurrences.sortedBy { it.lineNumber },
                arrayDetails = if (key.second in ARRAY_VARIABLE_TYPES) arrayDetailsByName[key.first] else null,
            )
        }
    }

    private fun collectArrayDetailsByName(file: TiBasicFile): Map<String, TiBasicArrayDetails> {
        val dimAccesses = file.dimStatements().flatMap { it.dimVariableAccesses() }.toSet()
        val optionBase = file.optionBaseStatements()
            .firstNotNullOfOrNull { it.optionBaseValue() }
            ?: DEFAULT_OPTION_BASE
        val explicitDetailsByName = dimAccesses
            .mapNotNull { varAccess ->
                val name = varAccess.node.firstChildNode?.text?.uppercase() ?: return@mapNotNull null
                name to TiBasicArrayDetails(
                    dimensions = varAccess.subscriptExpressions().map { it.text },
                    optionBase = optionBase,
                )
            }
            .toMap()
        val implicitDetailsByName = file.variableAccesses()
            .filter { it.hasSubscriptParens() }
            .filterNot { it in dimAccesses }
            .mapNotNull { varAccess ->
                val name = varAccess.node.firstChildNode?.text?.uppercase() ?: return@mapNotNull null
                if (name in explicitDetailsByName) return@mapNotNull null
                val dimCount = varAccess.subscriptDimCount()
                if (dimCount == 0) return@mapNotNull null
                name to TiBasicArrayDetails(
                    dimensions = List(dimCount) { DEFAULT_ARRAY_DIMENSION.toString() },
                    optionBase = optionBase,
                )
            }
            .toMap()
        return explicitDetailsByName + implicitDetailsByName
    }

    internal fun determineAccessType(varAccess: TiBasicVariableAccess): AccessType =
        when (val directParent = varAccess.parent) {
            is TiBasicInputStatement -> AccessType.WRITE
            is TiBasicReadStatement -> AccessType.WRITE
            is TiBasicLetStatement -> {
                val isLhs = directParent.node.firstChildOfType(TiBasicNodeTypes.VARIABLE_ACCESS) == varAccess.node
                if (isLhs) AccessType.WRITE else AccessType.READ
            }

            is TiBasicForStatement -> {
                val isLoopVar = directParent.node.nonWhitespaceChildren
                    .firstOrNull { it.elementType == TiBasicNodeTypes.VARIABLE_ACCESS } == varAccess.node
                if (isLoopVar) AccessType.WRITE else AccessType.READ
            }

            is TiBasicExpression -> {
                val exprParent = directParent.parent
                if (exprParent is TiBasicCallStatement) {
                    val argIndex = exprParent.arguments().indexOf(directParent)
                    TiBasicCallSubprograms.byName(exprParent.subprogramName())
                        ?.takeIf { argIndex >= 0 }
                        ?.argRuleAt(argIndex)
                        ?.access
                        ?.let { access -> if (access == CallArgAccess.WRITE) AccessType.WRITE else AccessType.READ }
                        ?: AccessType.READ
                } else {
                    AccessType.READ
                }
            }

            is TiBasicNextStatement -> AccessType.READ

            else -> AccessType.READ
        }

    private fun extractLetConstant(varAccess: TiBasicVariableAccess): String? {
        val letStmt = varAccess.parent as? TiBasicLetStatement ?: return null
        val rhs = letStmt.node
            .childrenAfter(TiBasicTokenTypes.EQ_OP)
            .firstOrNull { it.elementType == TiBasicNodeTypes.EXPRESSION } ?: return null
        val kids = rhs.nonWhitespaceChildren
        if (kids.size != 1) return null
        return when (kids[0].elementType) {
            TiBasicTokenTypes.NUMERIC_LITERAL, TiBasicTokenTypes.STRING_LITERAL -> kids[0].text
            else -> null
        }
    }

    private fun PsiElement.containingTiBasicLine(): TiBasicLine? {
        var element: PsiElement? = parent
        while (element != null && element !is TiBasicFile) {
            if (element is TiBasicLine) return element
            element = element.parent
        }
        return null
    }
}

private val ARRAY_VARIABLE_TYPES: Set<TiBasicVariableType> = setOf(
    TiBasicVariableType.NUMERIC_ARRAY,
    TiBasicVariableType.STRING_ARRAY,
)
