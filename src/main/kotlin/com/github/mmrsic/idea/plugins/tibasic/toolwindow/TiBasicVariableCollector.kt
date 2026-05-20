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
        val arrayMetadataByKey = collectArrayMetadataByKey(file)
        val result = mutableListOf<TiBasicVariableEntry>()
        result += collectUserFunctions(file)
        result += collectRegularVariables(file, arrayMetadataByKey)
        return resolveValueRanges(result)
            .sortedWith(compareBy({ it.name }, { it.type.ordinal }))
    }

    fun collectCached(file: TiBasicFile): List<TiBasicVariableEntry> =
        CachedValuesManager.getCachedValue(file) {
            CachedValueProvider.Result.create(collect(file), file)
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
        arrayMetadataByKey: Map<VariableEntryKey, TiBasicArrayMetadata>,
    ): List<TiBasicVariableEntry> {
        val dimAccesses = file.dimStatements().flatMap { it.dimVariableAccesses() }.toSet()
        val grouped = mutableMapOf<VariableEntryKey, MutableList<TiBasicVariableOccurrence>>()

        for (varAccess in file.variableAccesses()) {
            if (varAccess in dimAccesses) continue
            val nameNode = varAccess.node.firstChildNode ?: continue
            if (nameNode.elementType == TiBasicTokenTypes.INVALID_VARIABLE_NAME) continue

            val name = nameNode.text.uppercase()
            val isArray = varAccess.hasSubscriptParens()
            val type = variableTypeOf(nameNode.elementType, isArray)

            val line = varAccess.containingTiBasicLine() ?: continue
            val accessType = determineAccessType(varAccess)
            val writtenValue = if (accessType == AccessType.WRITE) extractWrittenValue(varAccess) else null
            val occurrence = TiBasicVariableOccurrence(line.lineNumber(), nameNode.startOffset, accessType, writtenValue)
            grouped.getOrPut(VariableEntryKey(name, type)) { mutableListOf() }.add(occurrence)
        }

        val entries = grouped.map { (key, occurrences) ->
            createVariableEntry(
                key = key,
                occurrences = occurrences,
                arrayMetadata = arrayMetadataByKey[key],
            )
        }
        val declaredOnlyEntries = arrayMetadataByKey.keys
            .filterNot(grouped::containsKey)
            .mapNotNull { key ->
                key.takeIf { it.type in ARRAY_VARIABLE_TYPES }?.let {
                    createVariableEntry(
                        key = key,
                        occurrences = emptyList(),
                        arrayMetadata = arrayMetadataByKey[key],
                    )
                }
            }
        return entries + declaredOnlyEntries
    }

    private fun createVariableEntry(
        key: VariableEntryKey,
        occurrences: List<TiBasicVariableOccurrence>,
        arrayMetadata: TiBasicArrayMetadata?,
    ): TiBasicVariableEntry =
        TiBasicVariableEntry(
            name = key.name,
            type = key.type,
            occurrences = occurrences.sortedBy { it.lineNumber },
            arrayDetails = arrayMetadata?.details,
            dimOccurrences = arrayMetadata?.dimOccurrences.orEmpty(),
        )

    private fun collectArrayMetadataByKey(file: TiBasicFile): Map<VariableEntryKey, TiBasicArrayMetadata> {
        val dimAccesses = file.dimStatements().flatMap { it.dimVariableAccesses() }.toSet()
        val optionBase = file.optionBaseStatements()
            .firstNotNullOfOrNull { it.optionBaseValue() }
            ?: DEFAULT_OPTION_BASE
        val explicitMetadataByKey = dimAccesses
            .mapNotNull { varAccess ->
                val key = variableEntryKey(varAccess) ?: return@mapNotNull null
                val line = varAccess.containingTiBasicLine() ?: return@mapNotNull null
                key to TiBasicArrayMetadata(
                    details = TiBasicArrayDetails(
                        dimensions = varAccess.subscriptExpressions().map { it.text },
                        optionBase = optionBase,
                    ),
                    dimOccurrences = listOf(
                        TiBasicVariableOccurrence(line.lineNumber(), varAccess.textOffset, AccessType.NONE),
                    ),
                )
            }
            .entriesByKey()
        val implicitMetadataByKey = file.variableAccesses()
            .filter { it.hasSubscriptParens() }
            .filterNot { it in dimAccesses }
            .mapNotNull { varAccess ->
                val key = variableEntryKey(varAccess) ?: return@mapNotNull null
                if (key in explicitMetadataByKey) return@mapNotNull null
                val dimCount = varAccess.subscriptDimCount()
                if (dimCount == 0) return@mapNotNull null
                key to TiBasicArrayMetadata(
                    details = TiBasicArrayDetails(
                        dimensions = List(dimCount) { DEFAULT_ARRAY_DIMENSION.toString() },
                        optionBase = optionBase,
                    ),
                )
            }
            .toMap()
        return explicitMetadataByKey + implicitMetadataByKey
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

    private fun resolveValueRanges(entries: List<TiBasicVariableEntry>): List<TiBasicVariableEntry> {
        val entryByKey = entries.associateBy { VariableEntryKey(it.name, it.type) }
        val resolvedRanges = mutableMapOf<VariableEntryKey, List<String>?>()
        return entries.map { entry ->
            val key = VariableEntryKey(entry.name, entry.type)
            entry.copy(
                    resolvedValueRange = resolvedRanges.getOrPut(key) {
                        resolveValueRange(key, entryByKey, resolvedRanges, emptySet())
                    },
            )
        }
    }

    private fun resolveValueRange(
        key: VariableEntryKey,
        entryByKey: Map<VariableEntryKey, TiBasicVariableEntry>,
        resolvedRanges: MutableMap<VariableEntryKey, List<String>?>,
        visitedKeys: Set<VariableEntryKey>,
    ): List<String>? {
        if (key in visitedKeys) return null
        val entry = entryByKey[key] ?: return null
        if (entry.type !in SCALAR_VARIABLE_TYPES) return null
        if (entry.writes == 0) return defaultValueRange(entry.type)
        val possibleValues = mutableListOf<String>()
        for (occurrence in entry.occurrences.filter { it.accessType == AccessType.WRITE }) {
            val writtenRange = when (val writtenValue = occurrence.writtenValue) {
                    is TiBasicWrittenValue.Constant -> listOf(writtenValue.value)
                    is TiBasicWrittenValue.VariableReference -> {
                        val referencedKey = VariableEntryKey(writtenValue.name, writtenValue.type)
                        resolvedRanges[referencedKey]
                            ?: resolveValueRange(referencedKey, entryByKey, resolvedRanges, visitedKeys + key)
                                ?.also { resolvedRanges[referencedKey] = it }
                    }

                    null -> null
            }
            possibleValues += writtenRange ?: return null
        }
        return possibleValues.distinct()
    }

    private fun defaultValueRange(type: TiBasicVariableType): List<String> =
        listOf(if (type == TiBasicVariableType.NUMERIC) DEFAULT_NUMERIC_CONST_VALUE else DEFAULT_STRING_CONST_VALUE)

    private fun extractWrittenValue(varAccess: TiBasicVariableAccess): TiBasicWrittenValue? {
        val letStmt = varAccess.parent as? TiBasicLetStatement ?: return null
        val rhs = letStmt.node
            .childrenAfter(TiBasicTokenTypes.EQ_OP)
            .firstOrNull { it.elementType == TiBasicNodeTypes.EXPRESSION } ?: return null
        val kids = rhs.nonWhitespaceChildren
        if (kids.size != 1) return null
        val child = kids[0]
        return when (child.elementType) {
            TiBasicTokenTypes.NUMERIC_LITERAL, TiBasicTokenTypes.STRING_LITERAL -> TiBasicWrittenValue.Constant(child.text)
            TiBasicNodeTypes.VARIABLE_ACCESS -> {
                val referencedVariable = child.psi as? TiBasicVariableAccess ?: return null
                val nameNode = referencedVariable.node.firstChildNode ?: return null
                TiBasicWrittenValue.VariableReference(
                    name = nameNode.text.uppercase(),
                    type = variableTypeOf(nameNode.elementType, referencedVariable.hasSubscriptParens()),
                )
            }

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

    private fun variableEntryKey(varAccess: TiBasicVariableAccess): VariableEntryKey? {
        val nameNode = varAccess.node.firstChildNode ?: return null
        return VariableEntryKey(
            name = nameNode.text.uppercase(),
            type = variableTypeOf(nameNode.elementType, isArray = true),
        )
    }

    private fun variableTypeOf(
        elementType: Any,
        isArray: Boolean,
    ): TiBasicVariableType =
        when {
            elementType == TiBasicTokenTypes.STRING_VARIABLE && isArray -> TiBasicVariableType.STRING_ARRAY
            elementType == TiBasicTokenTypes.STRING_VARIABLE -> TiBasicVariableType.STRING
            isArray -> TiBasicVariableType.NUMERIC_ARRAY
            else -> TiBasicVariableType.NUMERIC
        }
}

private val ARRAY_VARIABLE_TYPES: Set<TiBasicVariableType> = setOf(
    TiBasicVariableType.NUMERIC_ARRAY,
    TiBasicVariableType.STRING_ARRAY,
)
private val SCALAR_VARIABLE_TYPES: Set<TiBasicVariableType> = setOf(
    TiBasicVariableType.NUMERIC,
    TiBasicVariableType.STRING,
)
private const val DEFAULT_NUMERIC_CONST_VALUE = "0"
private const val DEFAULT_STRING_CONST_VALUE = "\"\""

private data class VariableEntryKey(
    val name: String,
    val type: TiBasicVariableType,
)

private data class TiBasicArrayMetadata(
    val details: TiBasicArrayDetails,
    val dimOccurrences: List<TiBasicVariableOccurrence> = emptyList(),
)

private fun List<Pair<VariableEntryKey, TiBasicArrayMetadata>>.entriesByKey(): Map<VariableEntryKey, TiBasicArrayMetadata> =
    groupBy({ it.first }, { it.second })
        .mapValues { (_, metadata) ->
            TiBasicArrayMetadata(
                details = metadata.last().details,
                dimOccurrences = metadata.flatMap { it.dimOccurrences }.sortedBy { it.lineNumber },
            )
        }
