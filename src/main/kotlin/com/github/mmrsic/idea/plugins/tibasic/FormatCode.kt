package com.github.mmrsic.idea.plugins.tibasic

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicInvalidLine
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
    val printStatement = line.children.firstOrNull() ?: return "${line.lineNumber()}"
    val statementText = printStatement.text.trim()
    val keywordMatch = TiBasicKeywords.getKeywords()
        .map { it.uppercase() }
        .firstOrNull { statementText.uppercase().startsWith(it) }
        ?: return "${line.lineNumber()} ${uppercaseOutsideStrings(statementText)}"
    val afterKeyword = statementText.drop(keywordMatch.length)
    val trimmedArgument = afterKeyword.trim()
    return if (trimmedArgument.isEmpty()) {
        "${line.lineNumber()} $keywordMatch"
    } else {
        "${line.lineNumber()} $keywordMatch ${removeWhitespaceOutsideStrings(trimmedArgument)}"
    }
}

fun uppercaseOutsideStrings(text: String): String {
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
                result.append(ch.uppercaseChar())
                i++
            }
        }
    }
    return result.toString()
}

fun removeWhitespaceOutsideStrings(text: String): String {
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

            ch.isWhitespace() -> i++

            else -> {
                result.append(ch)
                i++
            }
        }
    }
    return result.toString()
}


