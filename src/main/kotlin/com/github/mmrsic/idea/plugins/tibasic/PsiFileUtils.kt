package com.github.mmrsic.idea.plugins.tibasic

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

fun replaceFileText(project: Project, file: PsiFile, newText: String) {
    val documentManager = PsiDocumentManager.getInstance(project)
    val document = documentManager.getDocument(file) ?: return
    WriteCommandAction.runWriteCommandAction(project) {
        document.replaceString(0, document.textLength, newText)
        documentManager.commitDocument(document)
    }
}

