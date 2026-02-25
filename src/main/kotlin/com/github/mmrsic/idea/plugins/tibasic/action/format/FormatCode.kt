package com.github.mmrsic.idea.plugins.tibasic.action.format

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicKeywords
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicInvalidLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLetStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLine
import com.intellij.psi.PsiElement

fun formattedText(file: TiBasicFile): String = formattedText(
    file.children.filter { it is TiBasicLine || it is TiBasicInvalidLine }
)

fun formattedText(lines: List<PsiElement>): String =
    lines.joinToString("\n") { child ->
        when (child) {
            is TiBasicLine -> formattedLine(child)
            is TiBasicInvalidLine -> child.text
            else -> child.text
        }
    }

private fun formattedLine(line: TiBasicLine): String {
    val statement = line.children.firstOrNull() ?: return "${line.lineNumber()}"
    val statementText = statement.text.trim()

    val firstTokenType = statement.node.firstChildNode?.elementType
    if (firstTokenType == TiBasicTokenTypes.GOTO_KEYWORD) {
        return formattedGotoLine(line.lineNumber(), statement.node.firstChildNode!!.text, statementText)
    }

    val keywordMatch = TiBasicKeywords.getKeywords()
        .map { it.uppercase() }
        .firstOrNull { statementText.uppercase().startsWith(it) }
        ?: return "${line.lineNumber()} ${
            if (statement is TiBasicLetStatement) removeWhitespaceOutsideStrings(uppercaseOutsideStrings(statementText))
            else uppercaseOutsideStrings(statementText)
        }"
    val afterKeyword = statementText.drop(keywordMatch.length)
    if (keywordMatch == "REM") {
        val trimmedRem = afterKeyword.trim()
        return if (trimmedRem.isEmpty()) "${line.lineNumber()} $keywordMatch"
        else "${line.lineNumber()} $keywordMatch  $trimmedRem"
    }
    val trimmedArgument = afterKeyword.trim()
    return if (trimmedArgument.isEmpty()) {
        "${line.lineNumber()} $keywordMatch"
    } else {
        "${line.lineNumber()} $keywordMatch ${removeWhitespaceOutsideStrings(trimmedArgument)}"
    }
}

private fun formattedGotoLine(lineNumber: Int, keywordTokenText: String, statementText: String): String {
    val normalizedKeyword = keywordTokenText.trim().replace(Regex("""[ \t]+"""), " ").uppercase()
    val canonicalKeyword = if (normalizedKeyword == "GO TO") "GO TO" else "GOTO"
    val argText = statementText.drop(keywordTokenText.length).trim()
    return if (argText.isEmpty()) "$lineNumber $canonicalKeyword"
    else "$lineNumber $canonicalKeyword $argText"
}

fun uppercaseOutsideStrings(text: String): String =
    transformOutsideStrings(text) { ch -> ch.uppercaseChar() }

fun removeWhitespaceOutsideStrings(text: String): String =
    transformOutsideStrings(text) { ch -> if (ch.isWhitespace()) null else ch }

private fun transformOutsideStrings(text: String, transform: (Char) -> Char?): String {
    val result = StringBuilder(text.length)
    var i = 0
    var inString = false
    while (i < text.length) {
        val ch = text[i]
        when {
            ch == '"' && inString && i + 1 < text.length && text[i + 1] == '"' -> {
                result.append('"')
                result.append('"')
                i += 2
            }

            ch == '"' -> {
                inString = !inString
                result.append(ch)
                i++
            }

            inString -> {
                result.append(ch)
                i++
            }

            else -> {
                val out = transform(ch)
                if (out != null) result.append(out)
                i++
            }
        }
    }
    return result.toString()
}
