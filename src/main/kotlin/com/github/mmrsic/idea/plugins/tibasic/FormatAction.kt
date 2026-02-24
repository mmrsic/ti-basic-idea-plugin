package com.github.mmrsic.idea.plugins.tibasic

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicInvalidLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLine
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiElement

class FormatAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val file = event.getData(CommonDataKeys.PSI_FILE)
        event.presentation.isEnabledAndVisible = file is TiBasicFile
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.PSI_FILE) as? TiBasicFile ?: return
        val selectionModel = event.getData(CommonDataKeys.EDITOR)?.selectionModel
        if (selectionModel != null && selectionModel.hasSelection()) {
            val selStart = selectionModel.selectionStart
            val selEnd = selectionModel.selectionEnd
            val selectedLines = file.children.filter { element ->
                isFormattable(element) &&
                        element.textRange.startOffset < selEnd &&
                        element.textRange.endOffset > selStart
            }
            if (selectedLines.isNotEmpty()) {
                val rangeStart = selectedLines.first().textRange.startOffset
                val rangeEnd = selectedLines.last().textRange.endOffset
                replaceRange(project, file, rangeStart, rangeEnd, formattedText(selectedLines))
            }
        } else {
            replaceFileText(project, file, formattedText(file))
        }
    }

    private fun isFormattable(element: PsiElement): Boolean =
        element is TiBasicLine || element is TiBasicInvalidLine
}

