package com.github.mmrsic.idea.plugins.tibasic.action.format

import com.github.mmrsic.idea.plugins.tibasic.ext.allChildren
import com.github.mmrsic.idea.plugins.tibasic.ext.childrenOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.firstChildOfType
import com.github.mmrsic.idea.plugins.tibasic.ext.nonWhitespaceChildren
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicKeywords
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.parser.TiBasicNodeTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicCloseStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicDataStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicForStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicIfStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicInputStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicInvalidLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLetStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicNextStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicOnGosubStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicOnGotoStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicOpenOption
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicOpenStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicOptionBaseStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicPrintStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicReadStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicRestoreStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType

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
    when (firstTokenType) {
        TiBasicTokenTypes.GOTO_KEYWORD ->
            return formattedGotoLine(line.lineNumber(), statement.node.firstChildNode!!.text, statementText)

        TiBasicTokenTypes.GOSUB_KEYWORD ->
            return formattedGosubLine(line.lineNumber(), statement.node.firstChildNode!!.text, statementText)

        TiBasicTokenTypes.RETURN_KEYWORD ->
            return formattedSimpleLine(line.lineNumber(), statement)

        TiBasicTokenTypes.ON_KEYWORD ->
            return when (statement) {
                is TiBasicOnGosubStatement -> formattedOnGosubLine(line.lineNumber(), statement)
                else -> formattedOnGotoLine(line.lineNumber(), statement as TiBasicOnGotoStatement)
            }

        TiBasicTokenTypes.IF_KEYWORD ->
            return formattedIfLine(line.lineNumber(), statement as TiBasicIfStatement)

        TiBasicTokenTypes.FOR_KEYWORD ->
            return formattedForLine(line.lineNumber(), statement as TiBasicForStatement)

        TiBasicTokenTypes.NEXT_KEYWORD ->
            return formattedNextLine(line.lineNumber(), statement as TiBasicNextStatement)

        TiBasicTokenTypes.INPUT_KEYWORD ->
            return formattedInputLine(line.lineNumber(), statement as TiBasicInputStatement)

        TiBasicTokenTypes.PRINT_KEYWORD ->
            return formattedPrintLine(line.lineNumber(), statement as TiBasicPrintStatement)

        TiBasicTokenTypes.READ_KEYWORD ->
            return formattedReadLine(line.lineNumber(), statement as TiBasicReadStatement)

        TiBasicTokenTypes.DATA_KEYWORD ->
            return formattedDataLine(line.lineNumber(), statement as TiBasicDataStatement)

        TiBasicTokenTypes.RESTORE_KEYWORD ->
            return formattedRestoreLine(line.lineNumber(), statement as TiBasicRestoreStatement)

        TiBasicTokenTypes.CALL_KEYWORD ->
            return formattedCallLine(line.lineNumber(), statement as TiBasicCallStatement)

        TiBasicTokenTypes.OPTION_BASE_KEYWORD ->
            return formattedOptionBaseLine(line.lineNumber(), statement as TiBasicOptionBaseStatement)

        TiBasicTokenTypes.OPEN_KEYWORD ->
            return formattedOpenLine(line.lineNumber(), statement as TiBasicOpenStatement)

        TiBasicTokenTypes.CLOSE_KEYWORD ->
            return formattedCloseLine(line.lineNumber(), statement as TiBasicCloseStatement)
    }

    val statementUpper = statementText.uppercase()
    val firstToken = statementUpper.takeWhile { it.isLetterOrDigit() }
    val keywordMatch = TiBasicKeywords.getKeywords()
        .map { it.uppercase() }
        .firstOrNull { it == firstToken }
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
        val formattedArg = removeWhitespaceOutsideStrings(uppercaseOutsideStrings(trimmedArgument))
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
        else "$lineNumber IF ${uppercaseOutsideStrings(argText)}"
    }

    val thenRelStart = thenNode.startOffset - stmtStart
    val thenRelEnd = thenNode.startOffset + thenNode.textLength - stmtStart
    val exprPart = stmtText.substring(ifNode.textLength, thenRelStart).trim()

    val formattedExpr = if (exprPart.isNotEmpty())
        removeWhitespaceOutsideStrings(uppercaseOutsideStrings(exprPart))
    else ""
    return buildString {
        append("$lineNumber IF")
        if (formattedExpr.isNotEmpty()) append(" $formattedExpr")
        append(if (formattedExpr.endsWith(")")) "THEN" else " THEN")
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

private fun formattedForLine(lineNumber: Int, statement: TiBasicForStatement): String {
    val stmtStart = statement.textRange.startOffset
    val stmtText = statement.text
    val forNode = statement.node.firstChildNode!!
    val eqNode = statement.node.firstChildOfType(TiBasicTokenTypes.EQ_OP)
    val toNode = statement.node.firstChildOfType(TiBasicTokenTypes.TO_KEYWORD)
    val stepNode = statement.node.firstChildOfType(TiBasicTokenTypes.STEP_KEYWORD)

    if (eqNode == null || toNode == null) {
        val argText = stmtText.drop(forNode.textLength).trim()
        return if (argText.isEmpty()) "$lineNumber FOR"
        else "$lineNumber FOR ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(argText))}"
    }

    val eqRelStart = eqNode.startOffset - stmtStart
    val eqRelEnd = eqRelStart + eqNode.textLength
    val toRelStart = toNode.startOffset - stmtStart
    val toRelEnd = toRelStart + toNode.textLength
    val varPart = stmtText.substring(forNode.textLength, eqRelStart).trim()
    val initialPart = stmtText.substring(eqRelEnd, toRelStart).trim()

    return buildString {
        append("$lineNumber FOR")
        if (varPart.isNotEmpty()) append(" ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(varPart))}")
        append("=")
        if (initialPart.isNotEmpty()) append(removeWhitespaceOutsideStrings(uppercaseOutsideStrings(initialPart)))
        append(" TO")
        if (stepNode != null) {
            val stepRelStart = stepNode.startOffset - stmtStart
            val stepRelEnd = stepRelStart + stepNode.textLength
            val limitPart = stmtText.substring(toRelEnd, stepRelStart).trim()
            val stepPart = stmtText.substring(stepRelEnd).trim()
            if (limitPart.isNotEmpty()) append(" ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(limitPart))}")
            append(" STEP")
            if (stepPart.isNotEmpty()) append(" ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(stepPart))}")
        } else {
            val limitPart = stmtText.substring(toRelEnd).trim()
            if (limitPart.isNotEmpty()) append(" ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(limitPart))}")
        }
    }
}

private fun formattedNextLine(lineNumber: Int, statement: TiBasicNextStatement): String =
    formattedSimpleLine(lineNumber, statement) { removeWhitespaceOutsideStrings(uppercaseOutsideStrings(it)) }

private fun formattedInputLine(lineNumber: Int, statement: TiBasicInputStatement): String {
    val stmtStart = statement.textRange.startOffset
    val stmtText = statement.text
    val inputNode = statement.node.firstChildNode!!
    val colonNode = statement.node.firstChildOfType(TiBasicTokenTypes.COLON)

    return if (colonNode == null) {
        val varsPart = stmtText.drop(inputNode.textLength).trim()
        if (varsPart.isEmpty()) "$lineNumber INPUT"
        else "$lineNumber INPUT ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(varsPart))}"
    } else {
        val colonRelStart = colonNode.startOffset - stmtStart
        val colonRelEnd = colonRelStart + colonNode.textLength
        val promptPart = stmtText.substring(inputNode.textLength, colonRelStart).trim()
        val varsPart = stmtText.substring(colonRelEnd).trim()
        buildString {
            append("$lineNumber INPUT")
            if (promptPart.isNotEmpty()) append(" ${uppercaseOutsideStrings(promptPart)}")
            append(":")
            if (varsPart.isNotEmpty()) append(removeWhitespaceOutsideStrings(uppercaseOutsideStrings(varsPart)))
        }
    }
}

private fun formattedPrintLine(lineNumber: Int, statement: TiBasicPrintStatement): String {
    val keyword = statement.node.firstChildNode!!.text.uppercase()
    if (!statement.isFileOutput()) {
        return formattedSimpleLine(lineNumber, statement) { removeWhitespaceOutsideStrings(uppercaseOutsideStrings(it)) }
    }
    val fileNumberExpr = statement.fileNumberExpr()
        ?: return formattedSimpleLine(lineNumber, statement) { removeWhitespaceOutsideStrings(uppercaseOutsideStrings(it)) }
    val fileNumberText = removeWhitespaceOutsideStrings(uppercaseOutsideStrings(fileNumberExpr.text.trim()))
    val recPart = statement.recordNumberExpr()?.let { recExpr ->
        ".REC ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(recExpr.text.trim()))}"
    } ?: ""
    val colonNode = statement.node.firstChildOfType(TiBasicTokenTypes.COLON)
    val argsPart = if (colonNode != null) {
        val colonRelEnd = colonNode.startOffset - statement.textRange.startOffset + colonNode.textLength
        val argsText = statement.text.substring(colonRelEnd).trim()
        if (argsText.isEmpty()) ":" else ":${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(argsText))}"
    } else ""
    return "$lineNumber $keyword #$fileNumberText$recPart$argsPart"
}

private fun formattedReadLine(lineNumber: Int, statement: TiBasicReadStatement): String =
    formattedSimpleLine(lineNumber, statement) { removeWhitespaceOutsideStrings(uppercaseOutsideStrings(it)) }

private fun formattedDataLine(lineNumber: Int, statement: TiBasicDataStatement): String {
    val dataItems = statement.node.allChildren
        .dropWhile { it.elementType == TiBasicTokenTypes.DATA_KEYWORD }
        .filter { it.elementType != TokenType.WHITE_SPACE }
    if (dataItems.isEmpty()) return "$lineNumber DATA"
    val itemsText = dataItems.joinToString("") { token ->
        when (token.elementType) {
            TiBasicTokenTypes.COMMA -> ","
            TiBasicTokenTypes.STRING_LITERAL -> token.text
            else -> token.text.uppercase()
        }
    }
    return "$lineNumber DATA $itemsText"
}

private fun formattedRestoreLine(lineNumber: Int, statement: TiBasicRestoreStatement): String {
    if (!statement.isFileRestore()) {
        return formattedSimpleLine(lineNumber, statement)
    }
    val fileNumberExpr = statement.fileNumberExpr()
        ?: return formattedSimpleLine(lineNumber, statement)
    val fileNumberText = removeWhitespaceOutsideStrings(uppercaseOutsideStrings(fileNumberExpr.text.trim()))
    val recPart = statement.recordNumberExpr()?.let { recExpr ->
        ",REC ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(recExpr.text.trim()))}"
    } ?: ""
    return "$lineNumber RESTORE #$fileNumberText$recPart"
}

private fun formattedCallLine(lineNumber: Int, statement: TiBasicCallStatement): String {
    val name = statement.subprogramName() ?: return "$lineNumber CALL"
    val args = statement.arguments()
    return if (args.isEmpty()) {
        "$lineNumber CALL $name"
    } else {
        val argsText = args.joinToString(",") { removeWhitespaceOutsideStrings(uppercaseOutsideStrings(it.text.trim())) }
        "$lineNumber CALL $name($argsText)"
    }
}

private fun formattedSimpleLine(lineNumber: Int, statement: PsiElement, argTransform: (String) -> String = { it }): String {
    val keywordNode = statement.node.firstChildNode!!
    val argText = statement.text.drop(keywordNode.textLength).trim()
    return if (argText.isEmpty()) "$lineNumber ${keywordNode.text.uppercase()}"
    else "$lineNumber ${keywordNode.text.uppercase()} ${argTransform(argText)}"
}

private fun formattedOptionBaseLine(lineNumber: Int, statement: TiBasicOptionBaseStatement): String {
    val keywordNode = statement.node.firstChildNode!!
    val argText = statement.text.drop(keywordNode.textLength).trim()
    return if (argText.isEmpty()) "$lineNumber OPTION BASE"
    else "$lineNumber OPTION BASE $argText"
}

private fun formattedOpenLine(lineNumber: Int, statement: TiBasicOpenStatement): String {
    val stmtStart = statement.textRange.startOffset
    val stmtText = statement.text
    val hashNode = statement.node.firstChildOfType(TiBasicTokenTypes.HASH)
    val colonNode = statement.node.firstChildOfType(TiBasicTokenTypes.COLON)
    val fileNumberNode = statement.fileNumberExpr()?.node
    val fileNameNode = statement.fileNameExpr()?.node
    if (hashNode == null || colonNode == null || fileNumberNode == null || fileNameNode == null) {
        val argText = stmtText.drop(statement.node.firstChildNode!!.textLength).trim()
        return if (argText.isEmpty()) "$lineNumber OPEN"
        else "$lineNumber OPEN ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(argText))}"
    }
    val fileNumberText = removeWhitespaceOutsideStrings(
        uppercaseOutsideStrings(
            stmtText.substring(fileNumberNode.startOffset - stmtStart, fileNumberNode.startOffset + fileNumberNode.textLength - stmtStart).trim()
        )
    )
    val fileNameText = uppercaseOutsideStrings(
        stmtText.substring(fileNameNode.startOffset - stmtStart, fileNameNode.startOffset + fileNameNode.textLength - stmtStart).trim()
    )
    val optionsPart = statement.options().joinToString("") { formattedOpenOption(it, stmtStart, stmtText) }
    return "$lineNumber OPEN #$fileNumberText:$fileNameText$optionsPart"
}

private fun formattedOpenOption(option: TiBasicOpenOption, stmtStart: Int, stmtText: String): String {
    val keywordNode = option.node.nonWhitespaceChildren
        .firstOrNull { it.elementType != TiBasicTokenTypes.COMMA } ?: return ""
    val keywordText = keywordNode.text.uppercase()
    val orgExpr = option.optionExpression()
    return if (orgExpr != null) {
        val exprText = removeWhitespaceOutsideStrings(
            uppercaseOutsideStrings(
                stmtText.substring(orgExpr.node.startOffset - stmtStart, orgExpr.node.startOffset + orgExpr.node.textLength - stmtStart).trim()
            )
        )
        ",$keywordText $exprText"
    } else {
        ",$keywordText"
    }
}

private fun formattedCloseLine(lineNumber: Int, statement: TiBasicCloseStatement): String {
    val stmtStart = statement.textRange.startOffset
    val stmtText = statement.text
    val hashNode = statement.node.firstChildOfType(TiBasicTokenTypes.HASH)
    val expressions = statement.node.childrenOfType(TiBasicNodeTypes.EXPRESSION)
    val fileNumberNode = expressions.getOrNull(0)
    if (hashNode == null || fileNumberNode == null) {
        val argText = stmtText.drop(statement.node.firstChildNode!!.textLength).trim()
        return if (argText.isEmpty()) "$lineNumber CLOSE"
        else "$lineNumber CLOSE ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(argText))}"
    }
    val fileNumberText = removeWhitespaceOutsideStrings(
        uppercaseOutsideStrings(
            stmtText.substring(fileNumberNode.startOffset - stmtStart, fileNumberNode.startOffset + fileNumberNode.textLength - stmtStart).trim()
        )
    )
    val deleteModifier = if (statement.hasDeleteModifier()) ":DELETE" else ""
    return "$lineNumber CLOSE #$fileNumberText$deleteModifier"
}

private fun formattedGotoLine(lineNumber: Int, keywordTokenText: String, statementText: String): String {
    val canonicalKeyword = canonicalGotoKeyword(keywordTokenText)
    val argText = statementText.drop(keywordTokenText.length).trim()
    return if (argText.isEmpty()) "$lineNumber $canonicalKeyword"
    else "$lineNumber $canonicalKeyword $argText"
}

private fun formattedGosubLine(lineNumber: Int, keywordTokenText: String, statementText: String): String {
    val canonicalKeyword = canonicalGosubKeyword(keywordTokenText)
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

private fun formattedOnGosubLine(lineNumber: Int, statement: TiBasicOnGosubStatement): String {
    val stmtStart = statement.textRange.startOffset
    val onKeywordNode = statement.node.firstChildNode!!
    val gosubNode = statement.node.firstChildOfType(TiBasicTokenTypes.GOSUB_KEYWORD)
    val stmtText = statement.text
    if (gosubNode == null) {
        val argText = stmtText.drop(onKeywordNode.textLength).trim()
        return if (argText.isEmpty()) "$lineNumber ON"
        else "$lineNumber ON ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(argText))}"
    }
    val canonicalGosub = canonicalGosubKeyword(gosubNode.text)
    val gosubRelStart = gosubNode.startOffset - stmtStart
    val gosubRelEnd = gosubNode.startOffset + gosubNode.textLength - stmtStart
    val exprPart = stmtText.substring(onKeywordNode.textLength, gosubRelStart).trim()
    val lineNumsPart = stmtText.substring(gosubRelEnd).trim()
    return buildString {
        append("$lineNumber ON")
        if (exprPart.isNotEmpty()) append(" ${removeWhitespaceOutsideStrings(uppercaseOutsideStrings(exprPart))}")
        append(" $canonicalGosub")
        if (lineNumsPart.isNotEmpty()) append(" ${removeWhitespaceOutsideStrings(lineNumsPart)}")
    }
}

private fun canonicalGotoKeyword(rawText: String): String {
    val normalized = rawText.trim().replace(Regex("""[ \t]+"""), " ").uppercase()
    return if (normalized == "GO TO") "GO TO" else "GOTO"
}

private fun canonicalGosubKeyword(rawText: String): String {
    val normalized = rawText.trim().replace(Regex("""[ \t]+"""), " ").uppercase()
    return if (normalized == "GO SUB") "GO SUB" else "GOSUB"
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
