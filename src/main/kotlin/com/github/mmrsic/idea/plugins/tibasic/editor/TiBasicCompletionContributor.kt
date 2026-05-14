package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lang.BuiltInFunctionSignature
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicBuiltInFunctions
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicCallSubprograms
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicKeywords
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicLanguage
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicForStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicNextStatement
import com.github.mmrsic.idea.plugins.tibasic.toolwindow.TiBasicVariableCollector
import com.github.mmrsic.idea.plugins.tibasic.toolwindow.TiBasicVariableType
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder

private const val AUTO_LINE_NUMBER_GROUPING = 0
private const val VARIABLE_GROUPING = 1
private const val CALL_SUBPROGRAM_GROUPING = 2
private const val FUNCTION_GROUPING = 3
private const val PROMINENT_AUTO_LINE_NUMBER_PRIORITY = 100.0
private const val COMPLETION_PARENS = "()"
private const val SELECT_WITH_OPEN_PAREN = '('
private const val RPAREN = ')'
private const val COMPLETION_SEPARATOR = " "
private const val AUTO_LINE_NUMBER_TYPE_TEXT = "line number"
private const val VARIABLE_TYPE_TEXT = "variable"
private const val ARRAY_TYPE_TEXT = "array"
private const val SUBPROGRAM_TYPE_TEXT = "subprogram"
private const val FUNCTION_TYPE_TEXT = "function"
private const val KEYWORD_TYPE_TEXT = "keyword"
private const val PAREN_CARET_OFFSET_FROM_TAIL = 1
private const val NEXT_KEYWORD = "NEXT"
private val functionLikeKeywords = setOf("TAB")
private val nextKeywordPrefixRegex = Regex("""^N(?:E(?:XT?)?)?$""", RegexOption.IGNORE_CASE)
private val nextVariablePrefixRegex = Regex("""^NEXT\s+([A-Za-z][A-Za-z0-9$]*)?$""", RegexOption.IGNORE_CASE)

class TiBasicCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val file = extractTiBasicFile(parameters) ?: return
        val identifierRange = identifierRangeBeforeCaret(parameters)
        val identifierPrefix = identifierRange?.let { parameters.editor.document.text.substring(it.first, it.last + 1) }.orEmpty()
        val completionResult = if (identifierPrefix.isNotEmpty() || isEmptyArraySubscriptContext(parameters)) {
            result.withPrefixMatcher(identifierPrefix).caseInsensitive()
        } else {
            result.caseInsensitive()
        }
        if (isCallSubprogramContext(parameters)) {
            TiBasicCallSubprograms.names().sorted().forEach { name ->
                completionResult.addElement(
                    PrioritizedLookupElement.withGrouping(
                        callSubprogramCompletion(name),
                        CALL_SUBPROGRAM_GROUPING,
                    )
                )
            }
            return
        }
        if (isOptionBaseContext(parameters)) {
            val wordAtCursor = wordBeforeCaret(parameters)
            result.withPrefixMatcher(wordAtCursor).caseInsensitive()
                .addElement(LookupElementBuilder.create("BASE").withTypeText("keyword").autoCompleteSingleMatch())
            return
        }
        nextCompletionContext(parameters)?.let { nextContext ->
            nextCompletions(file, nextContext)
                .forEach(result.caseInsensitive()::addElement)
            return
        }
        if (shouldOfferAutoLineNumberCompletion(parameters.editor, file)) {
            completionResult.addElement(
                promotedAutoLineNumberCompletion(
                    autoLineNumberCompletion(file),
                    shouldPromoteAutoLineNumber(parameters),
                )
            )
        }
        val functionNames = TiBasicBuiltInFunctions.allNames()
        functionNames.sorted()
            .forEach { name ->
                completionResult.addElement(
                    PrioritizedLookupElement.withGrouping(
                        functionCompletion(name),
                        FUNCTION_GROUPING,
                    )
                )
            }
        TiBasicKeywords.getKeywords()
            .filter { it !in functionNames }
            .forEach { keyword ->
                completionResult.addElement(keywordCompletion(keyword))
            }
        variableCompletions(file, identifierPrefix, identifierRange?.first)
            .forEach { completion ->
                completionResult.addElement(
                    PrioritizedLookupElement.withGrouping(
                        completion.lookupElement(),
                        VARIABLE_GROUPING,
                    )
                )
            }
    }

    private fun extractTiBasicFile(parameters: CompletionParameters): TiBasicFile? =
        if (parameters.invocationCount == 0) null
        else if (parameters.position.language != TiBasicLanguage) null
        else parameters.position.containingFile as? TiBasicFile

    private fun isCallSubprogramContext(parameters: CompletionParameters): Boolean {
        val pos = parameters.position
        if (pos.node.elementType == TiBasicTokenTypes.CALL_SUBPROGRAM_NAME) return true
        val prevLeaf = parameters.originalFile.findElementAt(parameters.offset - 1)
        return prevLeaf?.node?.elementType == TiBasicTokenTypes.CALL_KEYWORD
    }

    private val optionBaseRegex = Regex("""^OPTION\s+[A-Za-z0-9]*$""", RegexOption.IGNORE_CASE)

    private fun isOptionBaseContext(parameters: CompletionParameters): Boolean {
        val docText = parameters.editor.document.text
        val lineStart = docText.lastIndexOf('\n', parameters.offset - 1) + 1
        val textBeforeCaret = docText.substring(lineStart, parameters.offset)
        val statementPart = textBeforeCaret.trimStart().dropWhile { it.isDigit() }.trimStart()
        return optionBaseRegex.matches(statementPart)
    }

    private fun wordBeforeCaret(parameters: CompletionParameters): String {
        val docText = parameters.editor.document.text
        val offset = parameters.offset
        var start = offset
        while (start > 0 && isCompletionIdentifierChar(docText[start - 1])) start--
        return docText.substring(start, offset)
    }

    private fun identifierRangeBeforeCaret(parameters: CompletionParameters): IntRange? {
        val docText = parameters.editor.document.text
        val offset = parameters.offset
        var start = offset
        while (start > 0 && isCompletionIdentifierChar(docText[start - 1])) start--
        return if (start == offset) null else start until offset
    }

    private fun isEmptyArraySubscriptContext(parameters: CompletionParameters): Boolean {
        val text = parameters.editor.document.charsSequence
        val offset = parameters.offset
        return offset in 1 until text.length
                && text[offset - 1] == '(' && text[offset] == ')'
    }

    private fun variableCompletions(file: TiBasicFile, identifierPrefix: String, identifierStartOffset: Int?): List<VariableCompletion> =
        TiBasicVariableCollector.collectCached(file)
            .filterNot { entry ->
                identifierStartOffset != null &&
                        identifierPrefix.isNotEmpty() &&
                        entry.name == identifierPrefix.uppercase() &&
                        entry.occurrences.size == 1 &&
                        entry.occurrences.single().offset == identifierStartOffset
            }
            .mapNotNull { variable ->
                when (variable.type) {
                    TiBasicVariableType.NUMERIC, TiBasicVariableType.STRING ->
                        VariableCompletion(variable.name, VARIABLE_TYPE_TEXT)

                    TiBasicVariableType.NUMERIC_ARRAY,
                    TiBasicVariableType.STRING_ARRAY,
                        -> VariableCompletion(
                        variable.name + COMPLETION_PARENS,
                        ARRAY_TYPE_TEXT,
                        arrayCompletionInsertHandler,
                    )

                    TiBasicVariableType.USER_FUNCTION -> null
                }
            }
            .distinctBy { it.lookupText }
            .sortedBy { it.lookupText }

    private fun callSubprogramCompletion(name: String): LookupElement =
        callableCompletion(
            name,
            SUBPROGRAM_TYPE_TEXT,
            TiBasicCallSubprograms.byName(name)?.requiresParentheses() == true,
        )

    private fun autoLineNumberCompletion(file: TiBasicFile): LookupElement =
        LookupElementBuilder.create(generatedAutoLineNumber(file).toString())
            .withTypeText(AUTO_LINE_NUMBER_TYPE_TEXT)
            .withInsertHandler(autoLineNumberCompletionInsertHandler)
            .autoCompleteSingleMatch()

    private fun shouldPromoteAutoLineNumber(parameters: CompletionParameters): Boolean =
        currentLineContext(parameters.editor).let { lineContext ->
            lineContext.text.isBlank() || typedLineNumberPrefix(lineContext) != null
        }

    private fun promotedAutoLineNumberCompletion(completion: LookupElement, shouldPromote: Boolean): LookupElement {
        val groupedCompletion = PrioritizedLookupElement.withGrouping(
            completion,
            AUTO_LINE_NUMBER_GROUPING,
        )
        return if (shouldPromote) {
            PrioritizedLookupElement.withPriority(groupedCompletion, PROMINENT_AUTO_LINE_NUMBER_PRIORITY)
        } else {
            groupedCompletion
        }
    }

    private fun functionCompletion(name: String): LookupElement {
        val requiresParentheses = TiBasicBuiltInFunctions.byName(name)?.requiresParentheses() == true
        return callableCompletion(
            name,
            FUNCTION_TYPE_TEXT,
            requiresParentheses,
        )
    }

    private fun nextCompletions(file: TiBasicFile, context: NextCompletionContext): List<LookupElement> {
        val completions = buildList {
            if (context is NextKeywordCompletionContext) {
                add(keywordCompletion(NEXT_KEYWORD))
            }
            addAll(
                openNextControlVariableNames(file, context.statementStartOffset)
                    .map { variableName -> nextVariableCompletion(variableName, context) },
            )
        }.distinctBy(LookupElement::getLookupString)
        return when (context) {
            is NextKeywordCompletionContext ->
                completions.filter { completion ->
                    completion.lookupString.startsWith(context.keywordPrefix, ignoreCase = true)
                }

            is NextVariableCompletionContext ->
                completions.filter { completion ->
                    completion.lookupString.removePrefix("$NEXT_KEYWORD ")
                        .startsWith(context.variablePrefix, ignoreCase = true)
                }
        }.mapIndexed { index, completion ->
            PrioritizedLookupElement.withPriority(completion, (completions.size - index).toDouble())
        }
    }

    private fun nextVariableCompletion(
        variableName: String,
        context: NextCompletionContext,
    ): LookupElement =
        LookupElementBuilder.create("$NEXT_KEYWORD $variableName")
            .withLookupStrings(setOf(variableName))
            .withTypeText(KEYWORD_TYPE_TEXT)
            .withInsertHandler(nextCompletionInsertHandler(context.statementStartOffset))
            .autoCompleteSingleMatch()

    private fun callableCompletion(lookupText: String, typeText: String, requiresParentheses: Boolean): LookupElement =
        LookupElementBuilder.create(lookupText)
            .let { builder ->
                if (requiresParentheses) {
                    builder.withTailText(COMPLETION_PARENS, true)
                } else {
                    builder
                }
            }
            .withTypeText(typeText)
            .let { builder ->
                if (requiresParentheses) {
                    builder.withInsertHandler(parenCompletionInsertHandler)
                } else {
                    builder
                }
            }
            .autoCompleteSingleMatch()

    private fun keywordCompletion(keyword: String): LookupElement =
        if (keyword in functionLikeKeywords) {
            callableCompletion(keyword, KEYWORD_TYPE_TEXT, true)
        } else {
            LookupElementBuilder.create(keyword)
                .withTypeText(KEYWORD_TYPE_TEXT)
                .autoCompleteSingleMatch()
        }

    private data class VariableCompletion(
        val lookupText: String,
        val typeText: String,
        val insertHandler: InsertHandler<LookupElement>? = null,
    ) {
        fun lookupElement(): LookupElement =
            LookupElementBuilder.create(lookupText)
                .withTypeText(typeText)
                .let { builder -> insertHandler?.let(builder::withInsertHandler) ?: builder }
                .autoCompleteSingleMatch()
    }

}

private fun LookupElementBuilder.autoCompleteSingleMatch(): LookupElement = withAutoCompletionPolicy(
    AutoCompletionPolicy.ALWAYS_AUTOCOMPLETE,
)

private sealed interface NextCompletionContext {
    val statementStartOffset: Int
}

private data class NextKeywordCompletionContext(
    override val statementStartOffset: Int,
    val keywordPrefix: String,
) : NextCompletionContext

private data class NextVariableCompletionContext(
    override val statementStartOffset: Int,
    val variablePrefix: String,
) : NextCompletionContext

private fun isCompletionIdentifierChar(char: Char): Boolean = char.isLetterOrDigit() || char == '$'

private fun BuiltInFunctionSignature.requiresParentheses(): Boolean = argCount > 0
private fun com.github.mmrsic.idea.plugins.tibasic.lang.CallSubprogramSignature.requiresParentheses(): Boolean =
    validArgCounts.any { it > 0 }

private fun nextCompletionContext(parameters: CompletionParameters): NextCompletionContext? {
    val document = parameters.editor.document
    val offset = parameters.offset
    val lineNumber = document.getLineNumber(offset)
    val lineStartOffset = document.getLineStartOffset(lineNumber)
    val linePrefix = currentLineContext(parameters.editor).text.take(offset - lineStartOffset)
    val statementPrefix = statementPrefixBeforeCaret(linePrefix) ?: return null
    val statementStartOffset = lineStartOffset + statementPrefix.startInLine
    return when {
        nextKeywordPrefixRegex.matches(statementPrefix.text) ->
            NextKeywordCompletionContext(
                statementStartOffset = statementStartOffset,
                keywordPrefix = statementPrefix.text.uppercase(),
            )

        nextVariablePrefixRegex.matches(statementPrefix.text) ->
            NextVariableCompletionContext(
                statementStartOffset = statementStartOffset,
                variablePrefix = statementPrefix.text.uppercase().substringAfter("$NEXT_KEYWORD ", ""),
            )

        else -> null
    }
}

private data class StatementPrefix(
    val text: String,
    val startInLine: Int,
)

private fun statementPrefixBeforeCaret(linePrefix: String): StatementPrefix? {
    var statementStartInLine = 0
    while (statementStartInLine < linePrefix.length && linePrefix[statementStartInLine].isWhitespace()) {
        statementStartInLine++
    }
    while (statementStartInLine < linePrefix.length && linePrefix[statementStartInLine].isDigit()) {
        statementStartInLine++
    }
    while (statementStartInLine < linePrefix.length && linePrefix[statementStartInLine].isWhitespace()) {
        statementStartInLine++
    }
    return linePrefix.substring(statementStartInLine)
        .takeIf(String::isNotEmpty)
        ?.let { statementText -> StatementPrefix(statementText, statementStartInLine) }
}

private fun openNextControlVariableNames(file: TiBasicFile, statementStartOffset: Int): List<String> {
    val openControlVariables = ArrayDeque<String>()
    file.lines()
        .asSequence()
        .filter { line -> line.textOffset < statementStartOffset }
        .mapNotNull { line -> line.children.firstOrNull() }
        .forEach { statement ->
            when (statement) {
                is TiBasicForStatement -> statement.controlVariableName()?.let(openControlVariables::addLast)
                is TiBasicNextStatement -> statement.controlVariableName()?.let { variableName ->
                    if (openControlVariables.lastOrNull() == variableName) {
                        openControlVariables.removeLast()
                    }
                }
            }
        }
    return openControlVariables
        .toList()
        .asReversed()
        .distinct()
}

private val parenCompletionInsertHandler = InsertHandler<LookupElement> { context, _ ->
    context.setAddCompletionChar(false)
    val document = context.document
    val tailOffset = context.tailOffset
    if (tailOffset < document.textLength && document.charsSequence[tailOffset] == SELECT_WITH_OPEN_PAREN) {
        val caretOffset = if (
            tailOffset + 1 < document.textLength &&
            document.charsSequence[tailOffset + 1] == RPAREN
        ) {
            tailOffset + 1
        } else {
            tailOffset
        }
        context.editor.caretModel.moveToOffset(caretOffset)
        return@InsertHandler
    }
    document.insertString(tailOffset, COMPLETION_PARENS)
    context.editor.caretModel.moveToOffset(tailOffset + PAREN_CARET_OFFSET_FROM_TAIL)
}

val arrayCompletionInsertHandler = InsertHandler<LookupElement> { context, _ ->
    context.setAddCompletionChar(false)
    val document = context.document
    if (
        context.completionChar == SELECT_WITH_OPEN_PAREN &&
        context.tailOffset < document.textLength &&
        document.charsSequence[context.tailOffset] == RPAREN
    ) {
        document.deleteString(context.tailOffset, context.tailOffset + 1)
    }
    context.editor.caretModel.moveToOffset(context.tailOffset - PAREN_CARET_OFFSET_FROM_TAIL)
}

private val autoLineNumberCompletionInsertHandler = InsertHandler<LookupElement> { context, _ ->
    context.setAddCompletionChar(false)
    val document = context.document
    val tailOffset = context.tailOffset
    if (tailOffset >= document.textLength || !document.charsSequence[tailOffset].isWhitespace()) {
        document.insertString(tailOffset, COMPLETION_SEPARATOR)
    }
    context.editor.caretModel.moveToOffset(tailOffset + COMPLETION_SEPARATOR.length)
}

private fun nextCompletionInsertHandler(statementStartOffset: Int) = InsertHandler<LookupElement> { context, item ->
    context.setAddCompletionChar(false)
    val completionText = item.lookupString
    context.document.replaceString(statementStartOffset, context.tailOffset, completionText)
    context.editor.caretModel.moveToOffset(statementStartOffset + completionText.length)
}
