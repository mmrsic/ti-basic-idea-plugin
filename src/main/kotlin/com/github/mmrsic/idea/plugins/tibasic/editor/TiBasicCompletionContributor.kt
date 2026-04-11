package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicBuiltInFunctions
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicCallSubprograms
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicKeywords
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicLanguage
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.toolwindow.TiBasicVariableCollector
import com.github.mmrsic.idea.plugins.tibasic.toolwindow.TiBasicVariableType
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder

private const val VARIABLE_GROUPING = 1
private const val CALL_SUBPROGRAM_GROUPING = 2
private const val FUNCTION_GROUPING = 3
private const val ARRAY_PARENS = "()"
private const val SELECT_WITH_OPEN_PAREN = '('
private const val RPAREN = ')'
private const val VARIABLE_TYPE_TEXT = "variable"
private const val ARRAY_TYPE_TEXT = "array"
private const val ARRAY_CARET_OFFSET_FROM_TAIL = 1

class TiBasicCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.invocationCount == 0) {
            return
        }
        if (parameters.position.language != TiBasicLanguage) {
            return
        }
        val file = parameters.position.containingFile as? TiBasicFile ?: return
        val identifierPrefix = identifierBeforeCaret(parameters)
        val completionResult = if (identifierPrefix.isNotEmpty() || isEmptyArraySubscriptContext(parameters)) {
            result.withPrefixMatcher(identifierPrefix).caseInsensitive()
        } else {
            result.caseInsensitive()
        }
        if (isCallSubprogramContext(parameters)) {
            TiBasicCallSubprograms.names().sorted().forEach { name ->
                completionResult.addElement(
                    PrioritizedLookupElement.withGrouping(
                        LookupElementBuilder.create(name).withTypeText("subprogram").autoCompleteSingleMatch(),
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
        val functionNames = TiBasicBuiltInFunctions.allNames()
        functionNames.sorted()
            .forEach { name ->
                completionResult.addElement(
                    PrioritizedLookupElement.withGrouping(
                        LookupElementBuilder.create(name).withTypeText("function").autoCompleteSingleMatch(),
                        FUNCTION_GROUPING,
                    )
                )
            }
        TiBasicKeywords.getKeywords()
            .filter { it !in functionNames }
            .forEach { keyword ->
                completionResult.addElement(LookupElementBuilder.create(keyword).withTypeText("keyword").autoCompleteSingleMatch())
            }
        variableCompletions(file)
            .forEach { completion ->
                completionResult.addElement(
                    PrioritizedLookupElement.withGrouping(
                        completion.lookupElement(),
                        VARIABLE_GROUPING,
                    )
                )
            }
    }

    private fun isCallSubprogramContext(parameters: CompletionParameters): Boolean {
        val pos = parameters.position
        if (pos.node.elementType == TiBasicTokenTypes.CALL_SUBPROGRAM_NAME) return true
        val prevLeaf = parameters.originalFile.findElementAt(parameters.offset - 1)
        return prevLeaf?.node?.elementType == TiBasicTokenTypes.CALL_KEYWORD
    }

    private fun isOptionBaseContext(parameters: CompletionParameters): Boolean {
        val docText = parameters.editor.document.text
        val lineStart = docText.lastIndexOf('\n', parameters.offset - 1) + 1
        val textBeforeCaret = docText.substring(lineStart, parameters.offset)
        val statementPart = textBeforeCaret.trimStart().dropWhile { it.isDigit() }.trimStart()
        return Regex("""^OPTION\s+[A-Za-z0-9]*$""", RegexOption.IGNORE_CASE).matches(statementPart)
    }

    private fun wordBeforeCaret(parameters: CompletionParameters): String {
        val docText = parameters.editor.document.text
        val offset = parameters.offset
        var start = offset
        while (start > 0 && isCompletionIdentifierChar(docText[start - 1])) start--
        return docText.substring(start, offset)
    }

    private fun identifierBeforeCaret(parameters: CompletionParameters): String = wordBeforeCaret(parameters)

    private fun isEmptyArraySubscriptContext(parameters: CompletionParameters): Boolean {
        val text = parameters.editor.document.charsSequence
        val offset = parameters.offset
        return offset > 0 &&
            offset < text.length &&
            text[offset - 1] == '(' &&
            text[offset] == ')'
    }

    private fun variableCompletions(file: TiBasicFile): List<VariableCompletion> =
        TiBasicVariableCollector.collectCached(file)
            .mapNotNull { variable ->
                when (variable.type) {
                    TiBasicVariableType.NUMERIC, TiBasicVariableType.STRING ->
                        VariableCompletion(variable.name, VARIABLE_TYPE_TEXT)

                    TiBasicVariableType.NUMERIC_ARRAY,
                    TiBasicVariableType.STRING_ARRAY,
                    TiBasicVariableType.DIM_DECLARATION,
                        -> VariableCompletion(
                        variable.name + ARRAY_PARENS,
                        ARRAY_TYPE_TEXT,
                        arrayCompletionInsertHandler,
                    )

                    TiBasicVariableType.USER_FUNCTION -> null
                }
            }
            .distinctBy { it.lookupText }
            .sortedBy { it.lookupText }

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
    com.intellij.codeInsight.lookup.AutoCompletionPolicy.ALWAYS_AUTOCOMPLETE,
)

private fun isCompletionIdentifierChar(char: Char): Boolean = char.isLetterOrDigit() || char == '$'

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
    context.editor.caretModel.moveToOffset(context.tailOffset - ARRAY_CARET_OFFSET_FROM_TAIL)
}
