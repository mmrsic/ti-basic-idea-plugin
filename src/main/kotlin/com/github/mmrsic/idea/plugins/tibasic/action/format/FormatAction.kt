package com.github.mmrsic.idea.plugins.tibasic.action.format

import com.github.mmrsic.idea.plugins.tibasic.action.TiBasicFileAction
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicInvalidLine
import com.github.mmrsic.idea.plugins.tibasic.psi.statement.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.util.replaceFileText
import com.github.mmrsic.idea.plugins.tibasic.util.replaceRange
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiElement

class FormatAction : TiBasicFileAction() {

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
