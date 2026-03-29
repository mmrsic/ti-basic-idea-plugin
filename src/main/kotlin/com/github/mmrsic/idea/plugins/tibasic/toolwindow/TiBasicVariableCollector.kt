package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.ext.firstChildOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.*
import com.intellij.psi.PsiElement

object TiBasicVariableCollector {

    fun collect(file: TiBasicFile): List<TiBasicVariableEntry> {
        val result = mutableListOf<TiBasicVariableEntry>()
        result += collectDimDeclarations(file)
        result += collectUserFunctions(file)
        result += collectRegularVariables(file)
        return result.sortedWith(compareBy({ it.name }, { it.type.ordinal }))
    }

    private fun collectDimDeclarations(file: TiBasicFile): List<TiBasicVariableEntry> =
        file.dimStatements().flatMap { dimStmt ->
            dimStmt.dimVariableAccesses().mapNotNull { varAccess ->
                val name = varAccess.node.firstChildNode?.text?.uppercase() ?: return@mapNotNull null
                val line = varAccess.containingTiBasicLine() ?: return@mapNotNull null
                TiBasicVariableEntry(
                    name = name,
                    type = TiBasicVariableType.DIM_DECLARATION,
                    occurrences = listOf(TiBasicVariableOccurrence(line.lineNumber(), varAccess.textOffset, AccessType.NONE)),
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

    private fun collectRegularVariables(file: TiBasicFile): List<TiBasicVariableEntry> {
        val dimAccesses = file.dimStatements().flatMap { it.dimVariableAccesses() }.toSet()
        val grouped = mutableMapOf<Pair<String, TiBasicVariableType>, MutableList<TiBasicVariableOccurrence>>()

        for (varAccess in file.variableAccesses()) {
            if (varAccess in dimAccesses) continue
            if (varAccess.parent is TiBasicNextStatement) continue
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
            val occurrence = TiBasicVariableOccurrence(line.lineNumber(), nameNode.startOffset, determineAccessType(varAccess))
            grouped.getOrPut(Pair(name, type)) { mutableListOf() }.add(occurrence)
        }

        return grouped.map { (key, occurrences) ->
            TiBasicVariableEntry(key.first, key.second, occurrences.sortedBy { it.lineNumber })
        }
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
                    when (exprParent.subprogramName()) {
                        "GCHAR" -> if (argIndex == 2) AccessType.WRITE else AccessType.READ
                        "KEY" -> if (argIndex in 1..2) AccessType.WRITE else AccessType.READ
                        "JOYST" -> if (argIndex in 1..2) AccessType.WRITE else AccessType.READ
                        else -> AccessType.READ
                    }
                } else {
                    AccessType.READ
                }
            }

            else -> AccessType.READ
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
