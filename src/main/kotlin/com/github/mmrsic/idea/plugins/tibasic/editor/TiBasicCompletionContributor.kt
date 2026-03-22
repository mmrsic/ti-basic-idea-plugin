package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicBuiltInFunctions
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicCallSubprograms
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicKeywords
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicLanguage
import com.github.mmrsic.idea.plugins.tibasic.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder

private const val VARIABLE_GROUPING = 1
private const val CALL_SUBPROGRAM_GROUPING = 2
private const val FUNCTION_GROUPING = 3

class TiBasicCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.invocationCount == 0) {
            return
        }
        if (parameters.position.language != TiBasicLanguage) {
            return
        }
        val file = parameters.position.containingFile as? TiBasicFile ?: return
        if (isCallSubprogramContext(parameters)) {
            TiBasicCallSubprograms.names().sorted().forEach { name ->
                result.caseInsensitive().addElement(
                    PrioritizedLookupElement.withGrouping(LookupElementBuilder.create(name).withTypeText("subprogram"), CALL_SUBPROGRAM_GROUPING)
                )
            }
            return
        }
        if (isOptionBaseContext(parameters)) {
            val wordAtCursor = wordBeforeCaret(parameters)
            result.withPrefixMatcher(wordAtCursor).caseInsensitive()
                .addElement(LookupElementBuilder.create("BASE").withTypeText("keyword"))
            return
        }
        val functionNames = TiBasicBuiltInFunctions.allNames()
        functionNames.sorted()
            .forEach { name ->
                result.caseInsensitive().addElement(
                    PrioritizedLookupElement.withGrouping(LookupElementBuilder.create(name).withTypeText("function"), FUNCTION_GROUPING)
                )
            }
        TiBasicKeywords.getKeywords()
            .filter { it !in functionNames }
            .forEach { keyword -> result.caseInsensitive().addElement(LookupElementBuilder.create(keyword).withTypeText("keyword")) }
        file.variableAccesses()
            .map { it.node.firstChildNode.text.uppercase() }
            .distinct()
            .sorted()
            .forEach { name ->
                result.caseInsensitive().addElement(
                    PrioritizedLookupElement.withGrouping(
                        LookupElementBuilder.create(name).withTypeText("variable"),
                        VARIABLE_GROUPING,
                    )
                )
            }
    }

    private fun isCallSubprogramContext(parameters: CompletionParameters): Boolean {
        val pos = parameters.position
        val tokenType = pos.node.elementType
        if (tokenType == TiBasicTokenTypes.CALL_SUBPROGRAM_NAME) return true
        val parent = pos.parent ?: return false
        if (parent is TiBasicCallStatement) return true
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
        while (start > 0 && docText[start - 1].isLetterOrDigit()) start--
        return docText.substring(start, offset)
    }
}
