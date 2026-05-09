package com.github.mmrsic.idea.plugins.tibasic.editor

private const val STRING_CHARACTER_TRIGGER_PREFIX = "\\"
private const val CONTROL_KEY_ACRONYM_PREFIX = "C"
private const val CONTROL_KEY_CARET_PREFIX = "^"
private const val CONTROL_KEY_SHORT_PREFIX = "C-"
private const val CONTROL_KEY_LONG_PREFIX = "CTRL-"
private const val FUNCTION_KEY_ACRONYM_PREFIX = "F"
private const val FUNCTION_KEY_SHORT_PREFIX = "F-"
private const val FUNCTION_KEY_LONG_PREFIX = "FCTN-"
private const val MIN_CHARACTER_CODE = 0
private const val MAX_CHARACTER_CODE = 255
private const val NUMERIC_CHARACTER_CODE_WIDTH = 3

private data class TiBasicKeyboardCharacterCode(
    val key: String,
    val code: Int,
)

private data class TiBasicStringCharacterTrigger(
    val trigger: String,
    val replacementText: String,
)

internal data class TiBasicStringCharacterMatch(
    val startInLine: Int,
    val replacementText: String,
)

private val supportedFunctionKeyCharacterCodes = listOf(
    TiBasicKeyboardCharacterCode("7", 1),
    TiBasicKeyboardCharacterCode("4", 2),
    TiBasicKeyboardCharacterCode("1", 3),
    TiBasicKeyboardCharacterCode("2", 4),
    TiBasicKeyboardCharacterCode("=", 5),
    TiBasicKeyboardCharacterCode("8", 6),
    TiBasicKeyboardCharacterCode("3", 7),
    TiBasicKeyboardCharacterCode("S", 8),
    TiBasicKeyboardCharacterCode("D", 9),
    TiBasicKeyboardCharacterCode("X", 10),
    TiBasicKeyboardCharacterCode("E", 11),
    TiBasicKeyboardCharacterCode("6", 12),
    TiBasicKeyboardCharacterCode("ENTER", 13),
    TiBasicKeyboardCharacterCode("5", 14),
    TiBasicKeyboardCharacterCode("9", 15),
)

private val supportedControlKeyCharacterCodes = listOf(
    TiBasicKeyboardCharacterCode("@", 128),
    TiBasicKeyboardCharacterCode("A", 129),
    TiBasicKeyboardCharacterCode("B", 130),
    TiBasicKeyboardCharacterCode("C", 131),
    TiBasicKeyboardCharacterCode("D", 132),
    TiBasicKeyboardCharacterCode("E", 133),
    TiBasicKeyboardCharacterCode("F", 134),
    TiBasicKeyboardCharacterCode("G", 135),
    TiBasicKeyboardCharacterCode("H", 136),
    TiBasicKeyboardCharacterCode("I", 137),
    TiBasicKeyboardCharacterCode("J", 138),
    TiBasicKeyboardCharacterCode("K", 139),
    TiBasicKeyboardCharacterCode("L", 140),
    TiBasicKeyboardCharacterCode("M", 141),
    TiBasicKeyboardCharacterCode("N", 142),
    TiBasicKeyboardCharacterCode("O", 143),
    TiBasicKeyboardCharacterCode("P", 144),
    TiBasicKeyboardCharacterCode("Q", 145),
    TiBasicKeyboardCharacterCode("R", 146),
    TiBasicKeyboardCharacterCode("S", 147),
    TiBasicKeyboardCharacterCode("T", 148),
    TiBasicKeyboardCharacterCode("U", 149),
    TiBasicKeyboardCharacterCode("V", 150),
    TiBasicKeyboardCharacterCode("W", 151),
    TiBasicKeyboardCharacterCode("X", 152),
    TiBasicKeyboardCharacterCode("Y", 153),
    TiBasicKeyboardCharacterCode("Z", 154),
    TiBasicKeyboardCharacterCode(".", 155),
    TiBasicKeyboardCharacterCode(";", 156),
    TiBasicKeyboardCharacterCode("=", 157),
    TiBasicKeyboardCharacterCode("8", 158),
    TiBasicKeyboardCharacterCode("9", 159),
    TiBasicKeyboardCharacterCode("/", 187),
)

private val numericStringCharacterTriggers = (MIN_CHARACTER_CODE..MAX_CHARACTER_CODE)
    .map(::numericCharacterTrigger)

private val aliasedStringCharacterTriggers =
    supportedFunctionKeyCharacterCodes.flatMap(::functionKeyCharacterTriggers) +
        supportedControlKeyCharacterCodes.flatMap(::controlKeyCharacterTriggers)

private val supportedStringCharacterTriggers = (numericStringCharacterTriggers + aliasedStringCharacterTriggers)
    .sortedByDescending { it.trigger.length }

private fun numericCharacterTrigger(code: Int): TiBasicStringCharacterTrigger =
    TiBasicStringCharacterTrigger(
        trigger = "$STRING_CHARACTER_TRIGGER_PREFIX${code.toString().padStart(NUMERIC_CHARACTER_CODE_WIDTH, '0')}",
        replacementText = sourceTextForCharacterCode(code),
    )

private fun functionKeyCharacterTriggers(definition: TiBasicKeyboardCharacterCode): List<TiBasicStringCharacterTrigger> =
    listOf(
        keyboardTrigger(definition, FUNCTION_KEY_ACRONYM_PREFIX),
        keyboardTrigger(definition, FUNCTION_KEY_SHORT_PREFIX),
        keyboardTrigger(definition, FUNCTION_KEY_LONG_PREFIX),
    )

private fun controlKeyCharacterTriggers(definition: TiBasicKeyboardCharacterCode): List<TiBasicStringCharacterTrigger> =
    listOf(
        keyboardTrigger(definition, CONTROL_KEY_ACRONYM_PREFIX),
        keyboardTrigger(definition, CONTROL_KEY_CARET_PREFIX),
        keyboardTrigger(definition, CONTROL_KEY_SHORT_PREFIX),
        keyboardTrigger(definition, CONTROL_KEY_LONG_PREFIX),
    )

private fun keyboardTrigger(
    definition: TiBasicKeyboardCharacterCode,
    prefix: String,
): TiBasicStringCharacterTrigger =
    TiBasicStringCharacterTrigger(
        trigger = "$STRING_CHARACTER_TRIGGER_PREFIX$prefix${definition.key}",
        replacementText = sourceTextForCharacterCode(definition.code),
    )

private fun sourceTextForCharacterCode(code: Int): String =
    when (val character = code.toChar()) {
        DOUBLE_QUOTE -> DOUBLE_QUOTE_PAIR
        else -> character.toString()
    }

internal fun matchStringCharacterTrigger(
    lineContext: LineContext,
    typedChar: Char,
): TiBasicStringCharacterMatch? {
    val prefixWithTypedChar = lineContext.text.take(lineContext.caretInLine) + typedChar
    val matchedTrigger = supportedStringCharacterTriggers.firstOrNull { trigger ->
        prefixWithTypedChar.endsWith(trigger.trigger, ignoreCase = true)
    } ?: return null
    return TiBasicStringCharacterMatch(
        startInLine = lineContext.caretInLine - (matchedTrigger.trigger.length - 1),
        replacementText = matchedTrigger.replacementText,
    )
}
