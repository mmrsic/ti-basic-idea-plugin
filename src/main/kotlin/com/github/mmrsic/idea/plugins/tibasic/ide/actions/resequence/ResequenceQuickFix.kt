package com.github.mmrsic.idea.plugins.tibasic.ide.actions.resequence

import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.common.util.replaceFileText
import com.github.mmrsic.idea.plugins.tibasic.language.format.RESEQUENCE_DEFAULT_START
import com.github.mmrsic.idea.plugins.tibasic.language.format.RESEQUENCE_DEFAULT_STEP
import com.github.mmrsic.idea.plugins.tibasic.language.format.resequencedText
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class ResequenceQuickFix(element: PsiElement) : LocalQuickFixAndIntentionActionOnPsiElement(element) {

    override fun getFamilyName(): String = "TI-Basic"

    override fun getText(): String = "Resequence all line numbers (step $RESEQUENCE_DEFAULT_STEP from $RESEQUENCE_DEFAULT_START)"

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement,
    ) {
        val tiBasicFile = file as? TiBasicFile ?: return
        val newText = resequencedText(tiBasicFile)
        replaceFileText(project, tiBasicFile, newText)
    }
}
