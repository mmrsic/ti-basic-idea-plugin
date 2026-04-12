package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.ext.lineNumberReferenceNodes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.common.VALID_LINE_NUMBER_RANGE
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicLine
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

data class TiBasicInboundLineReference(
    val sourceLine: TiBasicLine,
    val sourceOffset: Int,
) {
    val sourceLineNumber: Int
        get() = sourceLine.lineNumber()
}

object TiBasicInboundLineReferenceCollector {

    fun collect(file: TiBasicFile): Map<Int, List<TiBasicInboundLineReference>> {
        val referencesByTarget = mutableMapOf<Int, MutableList<TiBasicInboundLineReference>>()
        file.lines().forEach { line ->
            line.children
                .flatMap { it.lineNumberReferenceNodes() }
                .mapNotNull { node ->
                    node.text.toIntOrNull()
                        ?.takeIf { it in VALID_LINE_NUMBER_RANGE }
                        ?.let { targetLineNumber -> targetLineNumber to TiBasicInboundLineReference(line, node.startOffset) }
                }
                .forEach { (targetLineNumber, reference) ->
                    referencesByTarget.getOrPut(targetLineNumber) { mutableListOf() }.add(reference)
                }
        }
        return referencesByTarget.mapValues { (_, references) ->
            references.sortedWith(compareBy({ it.sourceLineNumber }, { it.sourceOffset }))
        }
    }

    fun collectCached(file: TiBasicFile): Map<Int, List<TiBasicInboundLineReference>> =
        CachedValuesManager.getCachedValue(file) {
            CachedValueProvider.Result.create(collect(file), file)
        }
}

internal fun referencedByTooltip(references: Collection<TiBasicInboundLineReference>): String {
    val referringLineNumbers = references
        .map { it.sourceLineNumber }
        .distinct()
        .sorted()
    if (referringLineNumbers.isEmpty()) return "No inbound line references"
    val previewLineNumbers = referringLineNumbers.take(MAX_TOOLTIP_LINE_NUMBERS)
        .joinToString(", ")
    return if (referringLineNumbers.size <= MAX_TOOLTIP_LINE_NUMBERS) {
        "Referenced by lines $previewLineNumbers"
    } else {
        "Referenced by ${referringLineNumbers.size} lines: $previewLineNumbers, ..."
    }
}

private const val MAX_TOOLTIP_LINE_NUMBERS = 5
