package com.github.mmrsic.idea.plugins.tibasic.util

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

fun replaceFileText(project: Project, file: PsiFile, newText: String) {
    withWriteCommand(project, file) { document ->
        document.replaceString(0, document.textLength, newText)
    }
}

fun replaceRange(project: Project, file: PsiFile, startOffset: Int, endOffset: Int, newText: String) {
    withWriteCommand(project, file) { document ->
        document.replaceString(startOffset, endOffset, newText)
    }
}

private fun withWriteCommand(project: Project, file: PsiFile, action: (com.intellij.openapi.editor.Document) -> Unit) {
    val documentManager = PsiDocumentManager.getInstance(project)
    val document = documentManager.getDocument(file) ?: return
    WriteCommandAction.runWriteCommandAction(project) {
        action(document)
        documentManager.commitDocument(document)
    }
}
