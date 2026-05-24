package com.github.mmrsic.idea.plugins.tibasic.language.analysis.variables

import com.github.mmrsic.idea.plugins.tibasic.language.analysis.calls.collectStaticallyTraceableStatementSnapshots
import com.github.mmrsic.idea.plugins.tibasic.common.ext.childrenAfter
import com.github.mmrsic.idea.plugins.tibasic.common.ext.firstChildOfType
import com.github.mmrsic.idea.plugins.tibasic.common.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.resolveNumericExpressionValue
import com.github.mmrsic.idea.plugins.tibasic.language.model.CallArgAccess
import com.github.mmrsic.idea.plugins.tibasic.language.model.TiBasicCallSubprograms
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicForStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicInputStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLetStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicNextStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicReadStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

private const val DEFAULT_ARRAY_DIMENSION = 10
private const val DEFAULT_OPTION_BASE = 0
private const val DEFAULT_FOR_STEP = 1

object TiBasicVariableCollector {

    fun collect(file: TiBasicFile): List<TiBasicVariableEntry> {
        val arrayMetadataByKey = collectArrayMetadataByKey(file)
        val (staticallyTraceableScalarWriteRanges, staticallyTraceableArrayRanges) = withoutCollectedConstantFallback {
            collectStaticallyTraceableRanges(file)
        }
        val result = mutableListOf<TiBasicVariableEntry>()
        result += collectUserFunctions(file)
        result += collectRegularVariables(file, arrayMetadataByKey)
        return resolveValueRanges(result, staticallyTraceableScalarWriteRanges, staticallyTraceableArrayRanges)
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
            val occurrence = TiBasicVariableOccurrence(
                line.lineNumber(),
                nameNode.startOffset,
                accessType,
                writtenValue,
                varAccess.subscriptExpressions(),
            )
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

    internal fun constantValueOf(variableAccess: TiBasicVariableAccess, file: TiBasicFile): String? =
        constantRangeOf(variableAccess, file)?.singleOrNull()

    internal fun collectedConstantFallbackSuppressed(): Boolean =
        collectedConstantFallbackSuppressed.get()

    internal fun constantRangeOf(variableAccess: TiBasicVariableAccess, file: TiBasicFile): List<String>? {
        if (collectedConstantFallbackSuppressed.get()) return null
        val nameNode = variableAccess.node.firstChildNode ?: return null
        val variableType = variableTypeOf(nameNode.elementType, variableAccess.hasSubscriptParens())
        val entry = collectCached(file)
            .firstOrNull { candidate ->
                candidate.name == variableAccess.name && candidate.type == variableType
            } ?: return null
        if (!variableAccess.hasSubscriptParens()) {
            return entry.valueRange
        }
        val subscripts = variableAccess.subscriptExpressions()
            .map { expression ->
                resolveNumericExpressionValue(expression) { referencedVariable ->
                    constantValueOf(referencedVariable, file)?.toIntOrNull()
                }
            }
        if (subscripts.any { it == null }) return null
        return entry.arrayElementValueRange(subscripts.filterNotNull())
    }

    private fun resolveValueRanges(
        entries: List<TiBasicVariableEntry>,
        staticallyTraceableScalarWriteRanges: Map<ScalarWriteKey, List<String>>,
        staticallyTraceableArrayRanges: Map<VariableEntryKey, Map<List<Int>, List<String>>>,
    ): List<TiBasicVariableEntry> {
        val entryByKey = entries.associateBy { VariableEntryKey(it.name, it.type) }
        val resolvedScalarRanges = mutableMapOf<VariableEntryKey, List<String>?>()
        val resolvedArrayElementRanges = mutableMapOf<ArrayElementKey, List<String>?>()
        return entries.map { entry ->
            val key = VariableEntryKey(entry.name, entry.type)
            entry.copy(
                resolvedValueRange = resolvedScalarRanges.getOrPut(key) {
                    resolveValueRange(
                        key,
                        entryByKey,
                        resolvedScalarRanges,
                        resolvedArrayElementRanges,
                        staticallyTraceableScalarWriteRanges,
                        emptySet(),
                    )
                },
                resolvedArrayElementRanges = if (entry.type in ARRAY_VARIABLE_TYPES) {
                    mergeArrayElementRanges(
                        resolveArrayElementRanges(
                            key = key,
                            entryByKey = entryByKey,
                            resolvedScalarRanges = resolvedScalarRanges,
                            resolvedArrayElementRanges = resolvedArrayElementRanges,
                        ),
                        staticallyTraceableArrayRanges[key].orEmpty(),
                    )
                } else {
                    emptyMap()
                },
            )
        }
    }

    private fun resolveValueRange(
        key: VariableEntryKey,
        entryByKey: Map<VariableEntryKey, TiBasicVariableEntry>,
        resolvedScalarRanges: MutableMap<VariableEntryKey, List<String>?>,
        resolvedArrayElementRanges: MutableMap<ArrayElementKey, List<String>?>,
        staticallyTraceableScalarWriteRanges: Map<ScalarWriteKey, List<String>>,
        visitedKeys: Set<VariableEntryKey>,
    ): List<String>? {
        if (key in visitedKeys) return null
        val entry = entryByKey[key] ?: return null
        if (entry.type !in SCALAR_VARIABLE_TYPES) return null
        if (entry.writes == 0) return defaultValueRange(entry.type)
        val possibleValues = mutableListOf<String>()
        for (occurrence in entry.occurrences.filter { it.accessType == AccessType.WRITE }) {
            val writtenRange = resolveWrittenValueRange(
                occurrence.writtenValue,
                entryByKey,
                resolvedScalarRanges,
                resolvedArrayElementRanges,
                staticallyTraceableScalarWriteRanges,
                visitedKeys + key,
                emptySet(),
            )
            possibleValues += writtenRange ?: staticallyTraceableScalarWriteRanges[ScalarWriteKey(key, occurrence.offset)] ?: return null
        }
        return possibleValues
            .distinct()
            .sortedRangeValues()
    }

    private fun defaultValueRange(type: TiBasicVariableType): List<String> =
        listOf(if (type == TiBasicVariableType.NUMERIC) DEFAULT_NUMERIC_CONST_VALUE else DEFAULT_STRING_CONST_VALUE)

    private fun extractWrittenValue(varAccess: TiBasicVariableAccess): TiBasicWrittenValue? {
        val forStmt = varAccess.parent as? TiBasicForStatement
        if (forStmt != null) {
            return TiBasicWrittenValue.ForLoopRange(
                startExpression = forStmt.startExpression(),
                endExpression = forStmt.endExpression(),
                stepExpression = forStmt.stepExpression(),
            )
        }
        val callExpression = varAccess.parent as? TiBasicExpression
        val callStatement = callExpression?.parent as? TiBasicCallStatement
        if (callStatement?.subprogramName()?.uppercase() == KEY_SUBPROGRAM_NAME) {
            val argIndex = callStatement.arguments().indexOf(callExpression)
            if (argIndex == KEY_STATUS_ARG_INDEX) {
                return TiBasicWrittenValue.FixedRange(KEY_STATUS_RANGE_VALUES)
            }
        }
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
                    subscriptExpressions = referencedVariable.subscriptExpressions(),
                )
            }

            else -> null
        }
    }

    private fun resolveWrittenValueRange(
        writtenValue: TiBasicWrittenValue?,
        entryByKey: Map<VariableEntryKey, TiBasicVariableEntry>,
        resolvedScalarRanges: MutableMap<VariableEntryKey, List<String>?>,
        resolvedArrayElementRanges: MutableMap<ArrayElementKey, List<String>?>,
        staticallyTraceableScalarWriteRanges: Map<ScalarWriteKey, List<String>>,
        visitedKeys: Set<VariableEntryKey>,
        visitedArrayKeys: Set<ArrayElementKey>,
    ): List<String>? =
        when (writtenValue) {
            is TiBasicWrittenValue.Constant -> listOf(writtenValue.value)
            is TiBasicWrittenValue.FixedRange -> writtenValue.values
            is TiBasicWrittenValue.VariableReference -> {
                when (writtenValue.type) {
                    in SCALAR_VARIABLE_TYPES -> {
                        val referencedKey = VariableEntryKey(writtenValue.name, writtenValue.type)
                        resolvedScalarRanges[referencedKey]
                            ?: resolveValueRange(
                                referencedKey,
                                entryByKey,
                                resolvedScalarRanges,
                                resolvedArrayElementRanges,
                                staticallyTraceableScalarWriteRanges,
                                visitedKeys,
                            )?.also { resolvedScalarRanges[referencedKey] = it }
                    }

                    in ARRAY_VARIABLE_TYPES -> {
                        val referencedKey = VariableEntryKey(writtenValue.name, writtenValue.type)
                        val referencedSubscripts = resolveSubscripts(
                            writtenValue.subscriptExpressions,
                            entryByKey,
                            resolvedScalarRanges,
                            resolvedArrayElementRanges,
                            staticallyTraceableScalarWriteRanges,
                            visitedKeys,
                            visitedArrayKeys,
                        ) ?: return null
                        val arrayElementKey = ArrayElementKey(referencedKey, referencedSubscripts)
                        resolvedArrayElementRanges[arrayElementKey]
                            ?: resolveArrayElementValueRange(
                                arrayElementKey,
                                entryByKey,
                                resolvedScalarRanges,
                                resolvedArrayElementRanges,
                                staticallyTraceableScalarWriteRanges,
                                visitedKeys,
                                visitedArrayKeys,
                            )?.also { resolvedArrayElementRanges[arrayElementKey] = it }
                    }

                    else -> null
                }
            }

            is TiBasicWrittenValue.ForLoopRange -> resolveForLoopRange(
                writtenValue,
                entryByKey,
                resolvedScalarRanges,
                resolvedArrayElementRanges,
                staticallyTraceableScalarWriteRanges,
                visitedKeys,
                visitedArrayKeys,
            )

            null -> null
        }

    private fun resolveForLoopRange(
        writtenValue: TiBasicWrittenValue.ForLoopRange,
        entryByKey: Map<VariableEntryKey, TiBasicVariableEntry>,
        resolvedScalarRanges: MutableMap<VariableEntryKey, List<String>?>,
        resolvedArrayElementRanges: MutableMap<ArrayElementKey, List<String>?>,
        staticallyTraceableScalarWriteRanges: Map<ScalarWriteKey, List<String>>,
        visitedKeys: Set<VariableEntryKey>,
        visitedArrayKeys: Set<ArrayElementKey>,
    ): List<String>? {
        val start = resolveNumericExpressionToInt(
            writtenValue.startExpression,
            entryByKey,
            resolvedScalarRanges,
            resolvedArrayElementRanges,
            staticallyTraceableScalarWriteRanges,
            visitedKeys,
            visitedArrayKeys,
        ) ?: return null
        val end = resolveNumericExpressionToInt(
            writtenValue.endExpression,
            entryByKey,
            resolvedScalarRanges,
            resolvedArrayElementRanges,
            staticallyTraceableScalarWriteRanges,
            visitedKeys,
            visitedArrayKeys,
        ) ?: return null
        val step = writtenValue.stepExpression?.let { stepExpression ->
            resolveNumericExpressionToInt(
                stepExpression,
                entryByKey,
                resolvedScalarRanges,
                resolvedArrayElementRanges,
                staticallyTraceableScalarWriteRanges,
                visitedKeys,
                visitedArrayKeys,
            )
        } ?: DEFAULT_FOR_STEP
        if (step == 0) return null
        return loopIterationValues(start, end, step).map(Int::toString)
    }

    private fun resolveNumericExpressionToInt(
        expression: TiBasicExpression?,
        entryByKey: Map<VariableEntryKey, TiBasicVariableEntry>,
        resolvedScalarRanges: MutableMap<VariableEntryKey, List<String>?>,
        resolvedArrayElementRanges: MutableMap<ArrayElementKey, List<String>?>,
        staticallyTraceableScalarWriteRanges: Map<ScalarWriteKey, List<String>>,
        visitedKeys: Set<VariableEntryKey>,
        visitedArrayKeys: Set<ArrayElementKey>,
    ): Int? =
        resolveNumericExpressionValue(expression) { variableAccess ->
            resolveNumericVariableAccessToInt(
                variableAccess,
                entryByKey,
                resolvedScalarRanges,
                resolvedArrayElementRanges,
                staticallyTraceableScalarWriteRanges,
                visitedKeys,
                visitedArrayKeys,
            )
        }

    private fun resolveNumericVariableAccessToInt(
        variableAccess: TiBasicVariableAccess,
        entryByKey: Map<VariableEntryKey, TiBasicVariableEntry>,
        resolvedScalarRanges: MutableMap<VariableEntryKey, List<String>?>,
        resolvedArrayElementRanges: MutableMap<ArrayElementKey, List<String>?>,
        staticallyTraceableScalarWriteRanges: Map<ScalarWriteKey, List<String>>,
        visitedKeys: Set<VariableEntryKey>,
        visitedArrayKeys: Set<ArrayElementKey>,
    ): Int? {
        val name = variableAccess.name ?: return null
        return if (variableAccess.hasSubscriptParens()) {
            val arrayKey = VariableEntryKey(name, TiBasicVariableType.NUMERIC_ARRAY)
            val subscripts = resolveSubscripts(
                variableAccess.subscriptExpressions(),
                entryByKey,
                resolvedScalarRanges,
                resolvedArrayElementRanges,
                staticallyTraceableScalarWriteRanges,
                visitedKeys,
                visitedArrayKeys,
            ) ?: return null
            val elementKey = ArrayElementKey(arrayKey, subscripts)
            val referencedRange = resolvedArrayElementRanges[elementKey]
                ?: resolveArrayElementValueRange(
                    elementKey,
                    entryByKey,
                    resolvedScalarRanges,
                    resolvedArrayElementRanges,
                    staticallyTraceableScalarWriteRanges,
                    visitedKeys,
                    visitedArrayKeys,
                )?.also { resolvedArrayElementRanges[elementKey] = it }
            referencedRange?.singleOrNull()?.toIntOrNull()
        } else {
            val key = VariableEntryKey(name, TiBasicVariableType.NUMERIC)
            val referencedRange = resolvedScalarRanges[key]
                ?: resolveValueRange(
                    key,
                    entryByKey,
                    resolvedScalarRanges,
                    resolvedArrayElementRanges,
                    staticallyTraceableScalarWriteRanges,
                    visitedKeys,
                )?.also { resolvedScalarRanges[key] = it }
            referencedRange?.singleOrNull()?.toIntOrNull()
        }
    }

    private fun resolveArrayElementRanges(
        key: VariableEntryKey,
        entryByKey: Map<VariableEntryKey, TiBasicVariableEntry>,
        resolvedScalarRanges: MutableMap<VariableEntryKey, List<String>?>,
        resolvedArrayElementRanges: MutableMap<ArrayElementKey, List<String>?>,
    ): Map<List<Int>, List<String>> {
        val entry = entryByKey[key] ?: return emptyMap()
        val invalidatedElements = mutableSetOf<List<Int>>()
        val resolvedRangesBySubscript = mutableMapOf<List<Int>, MutableList<String>>()
        for (occurrence in entry.occurrences.filter { it.accessType == AccessType.WRITE }) {
            val subscripts = resolveSubscripts(
                occurrence.subscriptExpressions,
                entryByKey,
                resolvedScalarRanges,
                resolvedArrayElementRanges,
                emptyMap(),
                emptySet(),
                emptySet(),
            ) ?: return emptyMap()
            val range = resolveWrittenValueRange(
                occurrence.writtenValue,
                entryByKey,
                resolvedScalarRanges,
                resolvedArrayElementRanges,
                emptyMap(),
                emptySet(),
                setOf(ArrayElementKey(key, subscripts)),
            )
            if (range == null) {
                invalidatedElements += subscripts
                resolvedRangesBySubscript.remove(subscripts)
                continue
            }
            if (subscripts !in invalidatedElements) {
                resolvedRangesBySubscript.getOrPut(subscripts) { mutableListOf() } += range
            }
        }
        return resolvedRangesBySubscript.mapValues { (_, ranges) -> ranges.distinct().sortedRangeValues() }
    }

    private fun resolveArrayElementValueRange(
        key: ArrayElementKey,
        entryByKey: Map<VariableEntryKey, TiBasicVariableEntry>,
        resolvedScalarRanges: MutableMap<VariableEntryKey, List<String>?>,
        resolvedArrayElementRanges: MutableMap<ArrayElementKey, List<String>?>,
        staticallyTraceableScalarWriteRanges: Map<ScalarWriteKey, List<String>>,
        visitedKeys: Set<VariableEntryKey>,
        visitedArrayKeys: Set<ArrayElementKey>,
    ): List<String>? {
        if (key in visitedArrayKeys) return null
        val entry = entryByKey[key.variableKey] ?: return null
        val matchingWrites = entry.occurrences
            .filter { it.accessType == AccessType.WRITE }
            .filter { occurrence ->
                resolveSubscripts(
                    occurrence.subscriptExpressions,
                    entryByKey,
                    resolvedScalarRanges,
                    resolvedArrayElementRanges,
                    staticallyTraceableScalarWriteRanges,
                    visitedKeys,
                    visitedArrayKeys + key,
                ) == key.subscripts
            }
        if (matchingWrites.isEmpty()) return null
        val possibleValues = mutableListOf<String>()
        for (occurrence in matchingWrites) {
            val writtenRange = resolveWrittenValueRange(
                occurrence.writtenValue,
                entryByKey,
                resolvedScalarRanges,
                resolvedArrayElementRanges,
                staticallyTraceableScalarWriteRanges,
                visitedKeys,
                visitedArrayKeys + key,
            ) ?: return null
            possibleValues += writtenRange
        }
        return possibleValues.distinct().sortedRangeValues()
    }

    private fun resolveSubscripts(
        expressions: List<TiBasicExpression>,
        entryByKey: Map<VariableEntryKey, TiBasicVariableEntry>,
        resolvedScalarRanges: MutableMap<VariableEntryKey, List<String>?>,
        resolvedArrayElementRanges: MutableMap<ArrayElementKey, List<String>?>,
        staticallyTraceableScalarWriteRanges: Map<ScalarWriteKey, List<String>>,
        visitedKeys: Set<VariableEntryKey>,
        visitedArrayKeys: Set<ArrayElementKey>,
    ): List<Int>? {
        val resolved = expressions.map { expression ->
            resolveNumericExpressionToInt(
                expression,
                entryByKey,
                resolvedScalarRanges,
                resolvedArrayElementRanges,
                staticallyTraceableScalarWriteRanges,
                visitedKeys,
                visitedArrayKeys,
            )
        }
        return if (resolved.all { it != null }) resolved.filterNotNull() else null
    }

    private fun mergeArrayElementRanges(
        resolvedRanges: Map<List<Int>, List<String>>,
        staticallyTraceableRanges: Map<List<Int>, List<String>>,
    ): Map<List<Int>, List<String>> =
        (resolvedRanges.keys + staticallyTraceableRanges.keys)
            .associateWith { subscripts ->
                resolvedRanges[subscripts].orEmpty()
                    .plus(staticallyTraceableRanges[subscripts].orEmpty())
                    .distinct()
                    .sortedRangeValues()
            }
            .filterValues(List<String>::isNotEmpty)

    private fun loopIterationValues(
        start: Int,
        end: Int,
        step: Int,
    ): List<Int> {
        if (step > 0 && start > end) return listOf(start)
        if (step < 0 && start < end) return listOf(start)
        val values = mutableListOf<Int>()
        var current = start
        while ((step > 0 && current <= end) || (step < 0 && current >= end)) {
            values += current
            current += step
        }
        return values.ifEmpty { listOf(start) }
    }

    private fun PsiElement.containingTiBasicLine(): TiBasicLine? {
        var element: PsiElement? = parent
        while (element != null && element !is TiBasicFile) {
            if (element is TiBasicLine) return element
            element = element.parent
        }
        return null
    }

    private fun variableEntryKey(
        varAccess: TiBasicVariableAccess,
        isArray: Boolean = true,
    ): VariableEntryKey? {
        val nameNode = varAccess.node.firstChildNode ?: return null
        return VariableEntryKey(
            name = nameNode.text.uppercase(),
            type = variableTypeOf(nameNode.elementType, isArray = isArray),
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

    private fun collectStaticallyTraceableRanges(
        file: TiBasicFile,
    ): Pair<Map<ScalarWriteKey, List<String>>, Map<VariableEntryKey, Map<List<Int>, List<String>>>> =
        collectStaticallyTraceableStatementSnapshots(file)
            .fold(
                TraceableRanges(
                    scalarWriteRanges = mutableMapOf(),
                    arrayRanges = mutableMapOf(),
                ),
            ) { groupedRanges, snapshot ->
                PsiTreeUtil.findChildrenOfType(snapshot.statement, TiBasicVariableAccess::class.java)
                    .filter { determineAccessType(it) == AccessType.WRITE }
                    .forEach { variableAccess ->
                        if (variableAccess.hasSubscriptParens()) return@forEach
                        val variableKey = variableEntryKey(variableAccess, isArray = false) ?: return@forEach
                        val value = when (variableKey.type) {
                            TiBasicVariableType.NUMERIC -> snapshot.staticValues.numericVariables[variableKey.name]?.toString()
                            TiBasicVariableType.STRING -> snapshot.staticValues.stringVariables[variableKey.name]?.let { "\"$it\"" }
                            else -> null
                        } ?: return@forEach
                        groupedRanges.scalarWriteRanges
                            .getOrPut(ScalarWriteKey(variableKey, variableAccess.textOffset)) { mutableListOf() }
                            .add(value)
                    }
                snapshot.staticValues.numericArrays.forEach { (key, value) ->
                    groupedRanges.arrayRanges.addArrayValue(
                        variableKey = VariableEntryKey(key.name, TiBasicVariableType.NUMERIC_ARRAY),
                        subscripts = key.subscripts,
                        value = value.toString(),
                    )
                }
                snapshot.staticValues.stringArrays.forEach { (key, value) ->
                    groupedRanges.arrayRanges.addArrayValue(
                        variableKey = VariableEntryKey(key.name, TiBasicVariableType.STRING_ARRAY),
                        subscripts = key.subscripts,
                        value = "\"$value\"",
                    )
                }
                groupedRanges
            }
            .let { groupedRanges ->
                groupedRanges.scalarWriteRanges.mapValues { (_, values) -> values.distinct().sortedRangeValues() } to
                    groupedRanges.arrayRanges.mapValues { (_, valuesBySubscript) ->
                        valuesBySubscript.mapValues { (_, values) -> values.distinct().sortedRangeValues() }
                    }
            }

    private fun <T> withoutCollectedConstantFallback(action: () -> T): T {
        val previousValue = collectedConstantFallbackSuppressed.get()
        collectedConstantFallbackSuppressed.set(true)
        return try {
            action()
        } finally {
            collectedConstantFallbackSuppressed.set(previousValue)
        }
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
private const val KEY_SUBPROGRAM_NAME = "KEY"
private const val KEY_STATUS_ARG_INDEX = 2
private val KEY_STATUS_RANGE_VALUES = listOf("-1", "1")

private data class VariableEntryKey(
    val name: String,
    val type: TiBasicVariableType,
)

private data class ScalarWriteKey(
    val variableKey: VariableEntryKey,
    val offset: Int,
)

private data class ArrayElementKey(
    val variableKey: VariableEntryKey,
    val subscripts: List<Int>,
)

private data class TiBasicArrayMetadata(
    val details: TiBasicArrayDetails,
    val dimOccurrences: List<TiBasicVariableOccurrence> = emptyList(),
)

private data class TraceableRanges(
    val scalarWriteRanges: MutableMap<ScalarWriteKey, MutableList<String>>,
    val arrayRanges: MutableMap<VariableEntryKey, MutableMap<List<Int>, MutableList<String>>>,
)

private val collectedConstantFallbackSuppressed = ThreadLocal.withInitial { false }

private fun MutableMap<VariableEntryKey, MutableMap<List<Int>, MutableList<String>>>.addArrayValue(
    variableKey: VariableEntryKey,
    subscripts: List<Int>,
    value: String,
) {
    getOrPut(variableKey) { mutableMapOf() }
        .getOrPut(subscripts) { mutableListOf() }
        .add(value)
}

private fun List<Pair<VariableEntryKey, TiBasicArrayMetadata>>.entriesByKey(): Map<VariableEntryKey, TiBasicArrayMetadata> =
    groupBy({ it.first }, { it.second })
        .mapValues { (_, metadata) ->
            TiBasicArrayMetadata(
                details = metadata.last().details,
                dimOccurrences = metadata.flatMap { it.dimOccurrences }.sortedBy { it.lineNumber },
            )
        }
