package com.github.mmrsic.idea.plugins.tibasic

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicCommentLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLine
import com.intellij.psi.PsiElement

fun formattedText(file: TiBasicFile): String = formattedText(
    file.children.filter { it is TiBasicLine || it is TiBasicCommentLine }
)

fun formattedText(lines: List<PsiElement>): String =
    lines.joinToString("\n") { child ->
        when (child) {
            is TiBasicLine -> formattedLine(child)
            is TiBasicCommentLine -> child.commentText()
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
    var inString = false
    for (ch in text) {
        when {
            ch == '"' -> {
                inString = !inString
                result.append(ch)
            }

            inString -> result.append(ch)
            else -> result.append(ch.uppercaseChar())
        }
    }
    return result.toString()
}

fun removeWhitespaceOutsideStrings(text: String): String {
    val result = StringBuilder(text.length)
    var inString = false
    for (ch in text) {
        when {
            ch == '"' -> {
                inString = !inString
                result.append(ch)
            }

            inString -> result.append(ch)
            ch.isWhitespace() -> Unit
            else -> result.append(ch)
        }
    }
    return result.toString()
}


