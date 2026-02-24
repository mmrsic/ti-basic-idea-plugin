package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicKeywords
import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicLanguage
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder

class TiBasicCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (parameters.position.language != TiBasicLanguage) {
            return
        }
        TiBasicKeywords.getKeywords().forEach { keyword ->
            result.caseInsensitive().addElement(LookupElementBuilder.create(keyword).withTypeText("keyword"))
        }
    }
}
