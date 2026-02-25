package com.github.mmrsic.idea.plugins.tibasic.action.format

import com.github.mmrsic.idea.plugins.tibasic.ext.firstChildOfType
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicKeywords
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.*
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
    if (firstTokenType == TiBasicTokenTypes.ON_KEYWORD) {
        return formattedOnGotoLine(line.lineNumber(), statement as TiBasicOnGotoStatement)
    }
    if (firstTokenType == TiBasicTokenTypes.IF_KEYWORD) {
        return formattedIfLine(line.lineNumber(), statement as TiBasicIfStatement)
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
        val formattedArg = if (statement is TiBasicLetStatement)
            removeWhitespaceOutsideStrings(uppercaseOutsideStrings(trimmedArgument))
        else
            removeWhitespaceOutsideStrings(trimmedArgument)
        "${line.lineNumber()} $keywordMatch $formattedArg"
    }
}

private fun formattedIfLine(lineNumber: Int, statement: TiBasicIfStatement): String {
    val stmtStart = statement.textRange.startOffset
    val stmtText = statement.text
    val ifNode = statement.node.firstChildNode!!
    val thenNode = statement.node.firstChildOfType(TiBasicTokenTypes.THEN_KEYWORD)
    val elseNode = statement.node.firstChildOfType(TiBasicTokenTypes.ELSE_KEYWORD)

    if (thenNode == null) {
        val argText = stmtText.drop(ifNode.textLength).trim()
        return if (argText.isEmpty()) "$lineNumber IF"
        else "$lineNumber IF ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(argText))}"
    }

    val thenRelStart = thenNode.startOffset - stmtStart
    val thenRelEnd = thenNode.startOffset + thenNode.textLength - stmtStart
    val exprPart = stmtText.substring(ifNode.textLength, thenRelStart).trim()

    return buildString {
        append("$lineNumber IF")
        if (exprPart.isNotEmpty()) append(" ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(exprPart))}")
        append(" THEN")
        if (elseNode != null) {
            val elseRelStart = elseNode.startOffset - stmtStart
            val elseRelEnd = elseNode.startOffset + elseNode.textLength - stmtStart
            val thenLinePart = stmtText.substring(thenRelEnd, elseRelStart).trim()
            val afterElsePart = stmtText.substring(elseRelEnd).trim()
            if (thenLinePart.isNotEmpty()) append(" $thenLinePart")
            append(" ELSE")
            if (afterElsePart.isNotEmpty()) append(" $afterElsePart")
        } else {
            val afterThenPart = stmtText.substring(thenRelEnd).trim()
            if (afterThenPart.isNotEmpty()) append(" $afterThenPart")
        }
    }
}

private fun formattedGotoLine(lineNumber: Int, keywordTokenText: String, statementText: String): String {
    val canonicalKeyword = canonicalGotoKeyword(keywordTokenText)
    val argText = statementText.drop(keywordTokenText.length).trim()
    return if (argText.isEmpty()) "$lineNumber $canonicalKeyword"
    else "$lineNumber $canonicalKeyword $argText"
}

private fun formattedOnGotoLine(lineNumber: Int, statement: TiBasicOnGotoStatement): String {
    val stmtStart = statement.textRange.startOffset
    val onKeywordNode = statement.node.firstChildNode!!
    val gotoNode = statement.node.firstChildOfType(TiBasicTokenTypes.GOTO_KEYWORD)
    val stmtText = statement.text
    if (gotoNode == null) {
        val argText = stmtText.drop(onKeywordNode.textLength).trim()
        return if (argText.isEmpty()) "$lineNumber ON"
        else "$lineNumber ON ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(argText))}"
    }
    val canonicalGoto = canonicalGotoKeyword(gotoNode.text)
    val gotoRelStart = gotoNode.startOffset - stmtStart
    val gotoRelEnd = gotoNode.startOffset + gotoNode.textLength - stmtStart
    val exprPart = stmtText.substring(onKeywordNode.textLength, gotoRelStart).trim()
    val lineNumsPart = stmtText.substring(gotoRelEnd).trim()
    return buildString {
        append("$lineNumber ON")
        if (exprPart.isNotEmpty()) append(" ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(exprPart))}")
        append(" $canonicalGoto")
        if (lineNumsPart.isNotEmpty()) append(" ${removeWhitespaceOutsideStrings(lineNumsPart)}")
    }
}

private fun canonicalGotoKeyword(rawText: String): String {
    val normalized = rawText.trim().replace(Regex("""[ \t]+"""), " ").uppercase()
    return if (normalized == "GO TO") "GO TO" else "GOTO"
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
