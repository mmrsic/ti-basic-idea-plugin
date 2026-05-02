package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.containingTiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicFunctionCall
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicDataStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.toolwindow.TiBasicVariableCollector
import com.github.mmrsic.idea.plugins.tibasic.toolwindow.TiBasicVariableType
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil

private const val CALL_CHAR_SUBPROGRAM = "CHAR"
private const val CALL_HCHAR_SUBPROGRAM = "HCHAR"
private const val CALL_VCHAR_SUBPROGRAM = "VCHAR"
private const val CHR_FUNCTION = "CHR$"
private const val CHARACTER_GROUP_START_CODE = 32
private const val CHARACTER_GROUP_END_CODE = 159
private const val CHARACTER_GROUP_SIZE = 8
private const val FIRST_CHARACTER_GROUP = 1
private const val DELETE_CHARACTER_CODE = 127
private const val SPACE_CHARACTER_CODE = 32
private const val MAX_CHAR_PATTERN_LENGTH = 16
private val HEX_CHAR_PATTERN_REGEX = Regex("^[0-9A-Fa-f]{0,16}$")
private val DATA_HEX_PATTERN_TOKEN_TYPES = setOf(
    TiBasicTokenTypes.STRING_LITERAL,
    TiBasicTokenTypes.NUMERIC_LITERAL,
    TiBasicTokenTypes.PRINT_ARGUMENT,
)

internal sealed interface TiBasicDocumentationUsage {
    val documentationElement: PsiElement
}

internal data class TiBasicCharacterCodeUsage(
    val expression: TiBasicExpression,
    val usageDescription: String,
) : TiBasicDocumentationUsage {
    override val documentationElement: PsiElement = expression
}

internal data class TiBasicHexPatternUsage(
    override val documentationElement: PsiElement,
    val originalPattern: String,
    val normalizedPattern: String,
    val usageDescription: String,
    val title: String,
) : TiBasicDocumentationUsage

internal data class TiBasicCharacterCodeOverride(
    val lineNumber: Int,
    val pattern: String,
)

internal fun resolveDocumentationUsage(element: PsiElement?): TiBasicDocumentationUsage? =
    resolveCharacterCodeUsage(element)
        ?: resolveCallCharHexPatternUsage(element)
        ?: resolveDataHexPatternUsage(element)
        ?: resolveStringLiteralHexPatternUsage(element)

internal fun resolveCharacterCodeUsage(element: PsiElement?): TiBasicCharacterCodeUsage? {
    val expression = when (element) {
        null -> return null
        is TiBasicExpression -> element
        else -> PsiTreeUtil.getParentOfType(element, TiBasicExpression::class.java, false)
    } ?: return null
    return when (val parent = expression.parent) {
        is TiBasicCallStatement -> resolveCallCharacterCodeUsage(parent, expression)
        is TiBasicFunctionCall -> resolveFunctionCharacterCodeUsage(parent, expression)
        else -> null
    }
}

internal fun resolveConstantNumericValue(expression: TiBasicExpression?, file: TiBasicFile?): Int? {
    expression ?: return null
    val children = expression.node.nonWhitespaceChildren
    if (children.size != 1) return null
    val child = children.first()
    return when (child.elementType) {
        TiBasicTokenTypes.NUMERIC_LITERAL -> child.text.toIntOrNull()
        TiBasicNodeTypes.VARIABLE_ACCESS -> constantVariableValue(
            variableName = child.firstChildNode?.text,
            file = file,
            acceptedTypes = setOf(TiBasicVariableType.NUMERIC),
        )?.toIntOrNull()

        else -> null
    }
}

internal fun resolveConstantStringValue(expression: TiBasicExpression?, file: TiBasicFile?): String? {
    expression ?: return null
    val children = expression.node.nonWhitespaceChildren
    if (children.size != 1) return null
    val child = children.first()
    return when (child.elementType) {
        TiBasicTokenTypes.STRING_LITERAL -> child.text.removePrefix("\"").removeSuffix("\"")
        TiBasicNodeTypes.VARIABLE_ACCESS -> constantVariableValue(
            variableName = child.firstChildNode?.text,
            file = file,
            acceptedTypes = setOf(TiBasicVariableType.STRING),
        )?.removePrefix("\"")?.removeSuffix("\"")

        else -> null
    }
}

internal fun normalizeHexPattern(pattern: String?): String? {
    pattern ?: return null
    return if (pattern.length <= MAX_CHAR_PATTERN_LENGTH && HEX_CHAR_PATTERN_REGEX.matches(pattern)) {
        pattern.uppercase().padEnd(MAX_CHAR_PATTERN_LENGTH, '0')
    } else {
        null
    }
}

internal fun normalizeDataHexPattern(pattern: String?): String? {
    pattern ?: return null
    if (pattern.isEmpty()) return null
    if (pattern.all(Char::isDigit) && !isAcceptedDigitOnlyDataHexPattern(pattern)) return null
    return normalizeHexPattern(pattern)
}

private fun isAcceptedDigitOnlyDataHexPattern(pattern: String): Boolean =
    (pattern.startsWith('0') && pattern.length <= MAX_CHAR_PATTERN_LENGTH) ||
        pattern.length in 9..MAX_CHAR_PATTERN_LENGTH

internal fun tiBasicCharacterGroup(code: Int): Int? =
    if (code in CHARACTER_GROUP_START_CODE..CHARACTER_GROUP_END_CODE) {
        ((code - CHARACTER_GROUP_START_CODE) / CHARACTER_GROUP_SIZE) + FIRST_CHARACTER_GROUP
    } else {
        null
    }

internal fun asciiCharacterName(code: Int): String? =
    when (code) {
        SPACE_CHARACTER_CODE -> "SPACE"
        in (SPACE_CHARACTER_CODE + 1) until DELETE_CHARACTER_CODE -> code.toChar().toString()
        DELETE_CHARACTER_CODE -> "DEL"
        else -> null
    }

internal fun collectCallCharOverrides(file: TiBasicFile): Map<Int, List<TiBasicCharacterCodeOverride>> =
    CachedValuesManager.getCachedValue(file) {
        CachedValueProvider.Result.create(buildCallCharOverrides(file), file)
    }

internal fun buildDocumentation(usage: TiBasicDocumentationUsage): String =
    when (usage) {
        is TiBasicCharacterCodeUsage -> buildCharacterCodeDocumentation(usage)
        is TiBasicHexPatternUsage -> buildHexPatternDocumentation(usage)
    }

internal fun buildCharacterCodeDocumentation(usage: TiBasicCharacterCodeUsage): String {
    val file = usage.expression.containingTiBasicFile
    val code = resolveConstantNumericValue(usage.expression, file)
    val sections = mutableListOf(
        htmlSection("Usage", usage.usageDescription),
    )
    if (code == null) {
        sections += htmlSection("Value", "Character code is not statically determinable here")
        return htmlDocument("Character code", sections)
    }
    sections += htmlSection("Code", code.toString())
    sections += htmlSection("ASCII", asciiCharacterName(code) ?: "No ASCII character")
    sections += htmlSection(
        "TI-Basic character group",
        tiBasicCharacterGroup(code)?.toString() ?: "No TI-Basic character group",
    )
    val overrides = file?.let { collectCallCharOverrides(it)[code].orEmpty() }.orEmpty()
    sections += htmlSectionRaw(
        "CALL CHAR overrides",
        if (overrides.isEmpty()) {
            StringUtil.escapeXmlEntities("No overrides in this file")
        } else {
            overrides.joinToString("<br/>") { override ->
                "Line ${override.lineNumber}: <code>${StringUtil.escapeXmlEntities(override.pattern)}</code>"
            }
        },
    )
    return htmlDocument("Character code $code", sections)
}

private fun resolveCallCharacterCodeUsage(
    callStatement: TiBasicCallStatement,
    expression: TiBasicExpression,
): TiBasicCharacterCodeUsage? {
    val argIndex = callStatement.arguments().indexOf(expression)
    if (argIndex < 0) return null
    return when (callStatement.subprogramName()) {
        CALL_CHAR_SUBPROGRAM ->
            argIndex.takeIf { it == 0 }?.let { TiBasicCharacterCodeUsage(expression, "CALL CHAR character code") }

        CALL_HCHAR_SUBPROGRAM ->
            argIndex.takeIf { it == 2 }?.let { TiBasicCharacterCodeUsage(expression, "CALL HCHAR character code") }

        CALL_VCHAR_SUBPROGRAM ->
            argIndex.takeIf { it == 2 }?.let { TiBasicCharacterCodeUsage(expression, "CALL VCHAR character code") }

        else -> null
    }
}

private fun resolveFunctionCharacterCodeUsage(
    functionCall: TiBasicFunctionCall,
    expression: TiBasicExpression,
): TiBasicCharacterCodeUsage? {
    val argIndex = functionCall.arguments().indexOf(expression)
    if (argIndex != 0 || functionCall.functionName() != CHR_FUNCTION) return null
    return TiBasicCharacterCodeUsage(expression, "CHR$ character code")
}

private fun resolveCallCharHexPatternUsage(element: PsiElement?): TiBasicHexPatternUsage? {
    val expression = when (element) {
        null -> return null
        is TiBasicExpression -> element
        else -> PsiTreeUtil.getParentOfType(element, TiBasicExpression::class.java, false)
    } ?: return null
    val callStatement = expression.parent as? TiBasicCallStatement ?: return null
    if (callStatement.subprogramName() != CALL_CHAR_SUBPROGRAM) return null
    if (callStatement.arguments().indexOf(expression) != 1) return null
    val rawPattern = resolveConstantStringValue(expression, expression.containingTiBasicFile) ?: return null
    val normalizedPattern = normalizeHexPattern(rawPattern) ?: return null
    return TiBasicHexPatternUsage(
        documentationElement = expression,
        originalPattern = rawPattern,
        normalizedPattern = normalizedPattern,
        usageDescription = "CALL CHAR hex pattern",
        title = "CALL CHAR hex pattern",
    )
}

private fun resolveDataHexPatternUsage(element: PsiElement?): TiBasicHexPatternUsage? {
    val dataElement = element
        ?.takeIf { it.node.elementType in DATA_HEX_PATTERN_TOKEN_TYPES }
        ?.takeIf { PsiTreeUtil.getParentOfType(it, TiBasicDataStatement::class.java, false) != null }
        ?: return null
    val rawPattern = when (dataElement.node.elementType) {
        TiBasicTokenTypes.STRING_LITERAL -> dataElement.text.removePrefix("\"").removeSuffix("\"")
        else -> dataElement.text.trim()
    }
    val normalizedPattern = normalizeDataHexPattern(rawPattern) ?: return null
    return TiBasicHexPatternUsage(
        documentationElement = dataElement,
        originalPattern = rawPattern,
        normalizedPattern = normalizedPattern,
        usageDescription = "DATA hex pattern",
        title = "DATA hex pattern",
    )
}

private fun resolveStringLiteralHexPatternUsage(element: PsiElement?): TiBasicHexPatternUsage? {
    val stringLiteral = element
        ?.takeIf { it.node.elementType == TiBasicTokenTypes.STRING_LITERAL }
        ?.takeIf { PsiTreeUtil.getParentOfType(it, TiBasicDataStatement::class.java, false) == null }
        ?: return null
    val rawPattern = stringLiteral.text.removePrefix("\"").removeSuffix("\"")
    val normalizedPattern = normalizeDataHexPattern(rawPattern) ?: return null
    return TiBasicHexPatternUsage(
        documentationElement = stringLiteral,
        originalPattern = rawPattern,
        normalizedPattern = normalizedPattern,
        usageDescription = "String hex pattern",
        title = "String hex pattern",
    )
}

private fun constantVariableValue(
    variableName: String?,
    file: TiBasicFile?,
    acceptedTypes: Set<TiBasicVariableType>,
): String? {
    if (file == null || variableName == null) return null
    return TiBasicVariableCollector.collectCached(file)
        .find { entry -> entry.name == variableName.uppercase() && entry.type in acceptedTypes }
        ?.constValue
}

private fun buildCallCharOverrides(file: TiBasicFile): Map<Int, List<TiBasicCharacterCodeOverride>> =
    file.callStatements()
        .asSequence()
        .filter { it.subprogramName() == CALL_CHAR_SUBPROGRAM }
        .mapNotNull { callStatement ->
            val code = resolveConstantNumericValue(callStatement.arguments().getOrNull(0), file) ?: return@mapNotNull null
            val pattern = normalizeHexPattern(resolveConstantStringValue(callStatement.arguments().getOrNull(1), file))
                ?: return@mapNotNull null
            val lineNumber = PsiTreeUtil.getParentOfType(callStatement, TiBasicLine::class.java)?.lineNumber()
                ?: return@mapNotNull null
            code to TiBasicCharacterCodeOverride(lineNumber, pattern)
        }
        .groupBy(
            keySelector = { it.first },
            valueTransform = { it.second },
        )
        .mapValues { (_, overrides) -> overrides.sortedBy { it.lineNumber } }

private fun buildHexPatternDocumentation(usage: TiBasicHexPatternUsage): String {
    val lineNumber = PsiTreeUtil.getParentOfType(usage.documentationElement, TiBasicLine::class.java)?.lineNumber()
    val sections = mutableListOf(
        htmlSection("Usage", usage.usageDescription),
    )
    if (lineNumber != null) {
        sections += htmlSection("Line", lineNumber.toString())
    }
    sections += htmlSectionRaw("Pattern", htmlCode(usage.originalPattern))
    sections += htmlSectionRaw("Normalized pattern", htmlCode(usage.normalizedPattern))
    sections += htmlSectionRaw("Preview", htmlCharPatternPreview(usage.normalizedPattern))
    return htmlDocument(usage.title, sections)
}

private fun htmlDocument(title: String, sections: List<String>): String =
    buildString {
        append("<html><body><b>")
        append(StringUtil.escapeXmlEntities(title))
        append("</b>")
        sections.forEach(::append)
        append("</body></html>")
    }

private fun htmlSection(title: String, value: String): String =
    "<p><b>${StringUtil.escapeXmlEntities(title)}:</b> ${StringUtil.escapeXmlEntities(value)}</p>"

private fun htmlSectionRaw(title: String, value: String): String =
    "<p><b>${StringUtil.escapeXmlEntities(title)}:</b> $value</p>"

private fun htmlCode(value: String): String =
    "<code>${StringUtil.escapeXmlEntities(value)}</code>"

private fun htmlCharPatternPreview(normalizedPattern: String): String {
    val bitmap = TiBasicCharPatternBitmap.fromHexPattern(normalizedPattern)
    return buildString {
        append("<table cellspacing='0' cellpadding='0' style='border-collapse:collapse;border:1px solid #666;'>")
        repeat(TI_BASIC_CHAR_PATTERN_GRID_SIZE) { row ->
            append("<tr>")
            repeat(TI_BASIC_CHAR_PATTERN_GRID_SIZE) { col ->
                val color = if (bitmap.bitAt(row, col)) "#000000" else "#FFFFFF"
                append("<td style='width:10px;height:10px;background:")
                append(color)
                append(";border:1px solid #666;'></td>")
            }
            append("</tr>")
        }
        append("</table>")
    }
}
