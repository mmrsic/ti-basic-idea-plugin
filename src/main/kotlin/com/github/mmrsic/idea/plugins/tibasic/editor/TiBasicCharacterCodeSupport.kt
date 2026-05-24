package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.common.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.UNARY_EXPRESSION_OPERATOR_TYPES
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.calls.TiBasicCallCharDefinition
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.calls.collectCallCharDefinitions
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.resolveNumericExpressionValue
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.containingTiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicFunctionCall
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicDataStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.variables.TiBasicVariableCollector
import com.github.mmrsic.idea.plugins.tibasic.language.analysis.variables.TiBasicVariableType
import com.github.mmrsic.idea.plugins.tibasic.language.values.parseTiBasicDecimalLiteral
import com.github.mmrsic.idea.plugins.tibasic.language.values.tiBasicDecimalString
import com.github.mmrsic.idea.plugins.tibasic.language.values.tiBasicRadix100Number
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import java.math.BigDecimal

internal const val CALL_CHAR_SUBPROGRAM = "CHAR"
private const val CALL_COLOR_SUBPROGRAM = "COLOR"
private const val CALL_HCHAR_SUBPROGRAM = "HCHAR"
private const val CALL_VCHAR_SUBPROGRAM = "VCHAR"
private const val CHR_FUNCTION = "CHR$"
private const val CHARACTER_GROUP_START_CODE = 32
private const val CHARACTER_GROUP_END_CODE = 159
private const val CHARACTER_GROUP_SIZE = 8
private const val FIRST_CHARACTER_GROUP = 1
private const val LAST_CODE_OFFSET_IN_CHARACTER_GROUP = CHARACTER_GROUP_SIZE - 1
private const val DELETE_CHARACTER_CODE = 127
private const val SPACE_CHARACTER_CODE = 32
private const val MAX_CHAR_PATTERN_LENGTH = 16
private const val CALL_COLOR_SET_ARG_INDEX = 0
private const val CALL_COLOR_FOREGROUND_ARG_INDEX = 1
private const val CALL_COLOR_BACKGROUND_ARG_INDEX = 2
private const val CALL_COLOR_SET_MEANING = "Character set whose characters receive the specified foreground and background colors"
private const val CALL_COLOR_FOREGROUND_MEANING = "Foreground color applied to the selected character set"
private const val CALL_COLOR_BACKGROUND_MEANING = "Background color applied to the selected character set"
private const val VALUE_SECTION = "Value"
private const val MEANING_SECTION = "Meaning"
private const val CHARACTER_CODE_RANGE_SECTION = "Character code range"
private const val ASCII_CHARACTERS_SECTION = "ASCII characters"
private const val TI_COLOR_NAME_SECTION = "TI color name"
private const val NON_CONSTANT_VALUE_MESSAGE = "Value is not statically determinable here"
private const val TI_VALUE_SECTION = "TI value"
private const val TI_RADIX_100_SECTION = "TI radix-100"
private const val TI_BYTES_HEX_SECTION = "Bytes (hex)"
private const val TI_BYTES_DECIMAL_SECTION = "Bytes (decimal)"
private const val EXACTNESS_SECTION = "Exactness"
private const val STORED_EXACTLY_MESSAGE = "Stored exactly in TI radix-100 format"
private const val ROUNDED_TO_TI_MESSAGE = "Rounded to the nearest TI radix-100 value"
private const val OUT_OF_RANGE_MESSAGE = "Not representable in the TI radix-100 range"
private const val LAST_CHARACTER_GROUP =
    ((CHARACTER_GROUP_END_CODE - CHARACTER_GROUP_START_CODE) / CHARACTER_GROUP_SIZE) + FIRST_CHARACTER_GROUP
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

internal enum class TiBasicCallColorArgument(
    val usageDescription: String,
    val title: String,
    val meaning: String,
) {
    CHARACTER_SET(
        "CALL COLOR character set",
        "CALL COLOR character set",
        CALL_COLOR_SET_MEANING,
    ),
    FOREGROUND_COLOR(
        "CALL COLOR foreground color",
        "CALL COLOR foreground color",
        CALL_COLOR_FOREGROUND_MEANING,
    ),
    BACKGROUND_COLOR(
        "CALL COLOR background color",
        "CALL COLOR background color",
        CALL_COLOR_BACKGROUND_MEANING,
    ),
}

internal data class TiBasicCallColorUsage(
    val expression: TiBasicExpression,
    val argument: TiBasicCallColorArgument,
) : TiBasicDocumentationUsage {
    override val documentationElement: PsiElement = expression
}

internal data class TiBasicNumericLiteralUsage(
    override val documentationElement: PsiElement,
    val sourceText: String,
    val sourceValue: BigDecimal,
) : TiBasicDocumentationUsage

internal data class TiBasicCharacterCodeOverride(
    val lineNumber: Int,
    val pattern: String,
)

internal fun resolveDocumentationUsage(element: PsiElement?): TiBasicDocumentationUsage? =
    resolveCharacterCodeUsage(element)
        ?: resolveCallColorUsage(element)
        ?: resolveCallCharHexPatternUsage(element)
        ?: resolveDataHexPatternUsage(element)
        ?: resolveStringLiteralHexPatternUsage(element)
        ?: resolveNumericLiteralUsage(element)

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
    file ?: return null
    return resolveNumericExpressionValue(expression) { variableAccess ->
        TiBasicVariableCollector.constantValueOf(variableAccess, file)?.toIntOrNull()
    }
}

internal fun resolveConstantStringValue(expression: TiBasicExpression?, file: TiBasicFile?): String? {
    expression ?: return null
    val children = expression.node.nonWhitespaceChildren
    if (children.size != 1) return null
    val child = children.first()
    return when (child.elementType) {
        TiBasicTokenTypes.STRING_LITERAL -> child.text.removePrefix("\"").removeSuffix("\"")
        TiBasicNodeTypes.VARIABLE_ACCESS -> (child.psi as? com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicVariableAccess)
            ?.takeIf { variableAccess -> variableAccess.name?.endsWith('$') == true }
            ?.let { variableAccess -> file?.let { currentFile -> TiBasicVariableCollector.constantValueOf(variableAccess, currentFile) } }
            ?.removePrefix("\"")
            ?.removeSuffix("\"")

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

internal fun callColorCharacterSetRange(set: Int): IntRange? =
    if (set in FIRST_CHARACTER_GROUP..LAST_CHARACTER_GROUP) {
        val firstCode = CHARACTER_GROUP_START_CODE + ((set - FIRST_CHARACTER_GROUP) * CHARACTER_GROUP_SIZE)
        firstCode..(firstCode + LAST_CODE_OFFSET_IN_CHARACTER_GROUP)
    } else {
        null
    }

internal fun collectCallCharOverrides(file: TiBasicFile): Map<Int, List<TiBasicCharacterCodeOverride>> =
    collectCallCharDefinitions(file)
        .groupBy(
            keySelector = TiBasicCallCharDefinition::code,
            valueTransform = { definition ->
                TiBasicCharacterCodeOverride(
                    lineNumber = definition.lineNumber,
                    pattern = definition.pattern,
                )
            },
        )
        .mapValues { (_, overrides) -> overrides.sortedBy(TiBasicCharacterCodeOverride::lineNumber) }

internal fun buildDocumentation(usage: TiBasicDocumentationUsage): String =
    when (usage) {
        is TiBasicCallColorUsage -> buildCallColorDocumentation(usage)
        is TiBasicCharacterCodeUsage -> buildCharacterCodeDocumentation(usage)
        is TiBasicHexPatternUsage -> buildHexPatternDocumentation(usage)
        is TiBasicNumericLiteralUsage -> buildNumericLiteralDocumentation(usage)
    }

internal fun buildCallColorDocumentation(usage: TiBasicCallColorUsage): String {
    val value = resolveConstantNumericValue(usage.expression, usage.expression.containingTiBasicFile)
    val sections = mutableListOf(
        htmlSection("Usage", usage.argument.usageDescription),
        htmlSection(MEANING_SECTION, usage.argument.meaning),
    )
    if (value == null) {
        sections += htmlSection(VALUE_SECTION, NON_CONSTANT_VALUE_MESSAGE)
        return htmlDocument(usage.argument.title, sections)
    }
    sections += htmlSection(VALUE_SECTION, value.toString())
    sections += tiRadix100Sections(BigDecimal.valueOf(value.toLong()))
    when (usage.argument) {
        TiBasicCallColorArgument.CHARACTER_SET -> {
            val characterSetRange = callColorCharacterSetRange(value)
            sections += htmlSectionRaw(
                CHARACTER_CODE_RANGE_SECTION,
                characterSetRange?.let { htmlCode("${it.first}-${it.last}") }
                    ?: StringUtil.escapeXmlEntities("No CALL COLOR character set for value $value"),
            )
            sections += htmlSectionRaw(
                ASCII_CHARACTERS_SECTION,
                characterSetRange?.let(::htmlAsciiCharactersForRange)
                    ?: StringUtil.escapeXmlEntities("No ASCII characters can be derived for this value"),
            )
        }

        TiBasicCallColorArgument.FOREGROUND_COLOR,
        TiBasicCallColorArgument.BACKGROUND_COLOR,
        -> sections += htmlSection(
            TI_COLOR_NAME_SECTION,
            tiColorAt(value)?.name ?: "No TI color for value $value",
        )
    }
    return htmlDocument(documentationTitle(usage.argument.title, value), sections)
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
    sections += tiRadix100Sections(BigDecimal.valueOf(code.toLong()))
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

private fun resolveNumericLiteralUsage(element: PsiElement?): TiBasicNumericLiteralUsage? {
    val expression = when (element) {
        null -> null
        is TiBasicExpression -> element
        else -> PsiTreeUtil.getParentOfType(element, TiBasicExpression::class.java, false)
    }
    signedNumericLiteralUsage(expression)?.let { return it }
    val numericLiteral = element?.takeIf { it.node.elementType == TiBasicTokenTypes.NUMERIC_LITERAL } ?: return null
    val value = parseTiBasicDecimalLiteral(numericLiteral.text) ?: return null
    return TiBasicNumericLiteralUsage(
        documentationElement = numericLiteral,
        sourceText = numericLiteral.text,
        sourceValue = value,
    )
}

private fun signedNumericLiteralUsage(expression: TiBasicExpression?): TiBasicNumericLiteralUsage? {
    expression ?: return null
    val children = expression.node.nonWhitespaceChildren
    if (children.isEmpty() || children.last().elementType != TiBasicTokenTypes.NUMERIC_LITERAL) return null
    if (children.dropLast(1).any { it.elementType !in UNARY_EXPRESSION_OPERATOR_TYPES }) return null
    val sourceText = children.joinToString(separator = "") { it.text }
    val value = parseTiBasicDecimalLiteral(sourceText) ?: return null
    return TiBasicNumericLiteralUsage(
        documentationElement = expression,
        sourceText = sourceText,
        sourceValue = value,
    )
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

private fun resolveCallColorUsage(element: PsiElement?): TiBasicCallColorUsage? {
    val expression = when (element) {
        null -> return null
        is TiBasicExpression -> element
        else -> PsiTreeUtil.getParentOfType(element, TiBasicExpression::class.java, false)
    } ?: return null
    val callStatement = expression.parent as? TiBasicCallStatement ?: return null
    if (callStatement.subprogramName() != CALL_COLOR_SUBPROGRAM) return null
    return when (callStatement.arguments().indexOf(expression)) {
        CALL_COLOR_SET_ARG_INDEX -> TiBasicCallColorUsage(expression, TiBasicCallColorArgument.CHARACTER_SET)
        CALL_COLOR_FOREGROUND_ARG_INDEX -> TiBasicCallColorUsage(expression, TiBasicCallColorArgument.FOREGROUND_COLOR)
        CALL_COLOR_BACKGROUND_ARG_INDEX -> TiBasicCallColorUsage(expression, TiBasicCallColorArgument.BACKGROUND_COLOR)
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
    if (TiBasicVariableCollector.collectedConstantFallbackSuppressed()) return null
    return TiBasicVariableCollector.collectCached(file)
        .find { entry -> entry.name == variableName.uppercase() && entry.type in acceptedTypes }
        ?.constValue
}

internal fun constantNumericVariableValue(variableName: String?, file: TiBasicFile?): Int? =
    constantVariableValue(
        variableName = variableName,
        file = file,
        acceptedTypes = setOf(TiBasicVariableType.NUMERIC),
    )?.toIntOrNull()

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

private fun buildNumericLiteralDocumentation(usage: TiBasicNumericLiteralUsage): String {
    val sections = mutableListOf(
        htmlSection("Usage", "Numeric constant"),
        htmlSection("Value", tiBasicDecimalString(usage.sourceValue)),
    )
    sections += tiRadix100Sections(usage.sourceValue)
    return htmlDocument("Numeric value ${usage.sourceText}", sections)
}

private fun tiRadix100Sections(sourceValue: BigDecimal): List<String> {
    val storedValue = tiBasicRadix100Number(sourceValue)
        ?: return listOf(htmlSection(TI_VALUE_SECTION, OUT_OF_RANGE_MESSAGE))
    return buildList {
        add(htmlSection(TI_VALUE_SECTION, tiBasicDecimalString(storedValue.value)))
        add(htmlSectionRaw(TI_RADIX_100_SECTION, htmlCode(storedValue.radixNotation())))
        add(htmlSectionRaw(TI_BYTES_HEX_SECTION, htmlCode(storedValue.hexBytes())))
        add(htmlSectionRaw(TI_BYTES_DECIMAL_SECTION, htmlCode(storedValue.decimalBytes())))
        add(
            htmlSection(
                EXACTNESS_SECTION,
                if (sourceValue.compareTo(storedValue.value) == 0) {
                    STORED_EXACTLY_MESSAGE
                } else {
                    ROUNDED_TO_TI_MESSAGE
                },
            ),
        )
    }
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

private fun documentationTitle(baseTitle: String, value: Int): String = "$baseTitle $value"

private fun htmlAsciiCharactersForRange(range: IntRange): String {
    val asciiCharacters = range.mapNotNull { code ->
        asciiCharacterName(code)?.let { name ->
            "$code: <code>${StringUtil.escapeXmlEntities(name)}</code>"
        }
    }
    return if (asciiCharacters.isEmpty()) {
        StringUtil.escapeXmlEntities("No ASCII characters in this character set")
    } else {
        asciiCharacters.joinToString("<br/>")
    }
}

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
