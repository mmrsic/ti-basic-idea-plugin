package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.lang.fileTypeExtensions
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JToolBar

abstract class TiBasicFileToolWindowContent(
    protected val project: Project,
) : JPanel(BorderLayout()), Disposable {

    protected val fileLabel = JLabel(" ")
    protected var currentFile: TiBasicFile? = null

    protected fun initializeToolWindow(table: JBTable) {
        add(JBScrollPane(table), BorderLayout.CENTER)
        setupToolbar()
        installDocumentListener()
        installFileEditorListener()
        refresh()
    }

    protected abstract fun refreshForFile(file: TiBasicFile?)

    protected fun installLineNavigation(table: JBTable, lineColumn: Int, offsetAtRow: (modelRow: Int) -> Int) {
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                val row = table.rowAtPoint(event.point)
                val column = table.columnAtPoint(event.point)
                if (row < 0 || column != lineColumn) return
                val modelRow = table.convertRowIndexToModel(row)
                navigateToOffset(offsetAtRow(modelRow))
            }
        })
        table.columnModel.getColumn(lineColumn).cellRenderer = javax.swing.table.DefaultTableCellRenderer().also {
            it.horizontalAlignment = JLabel.LEFT
            it.foreground = JBColor.BLUE
        }
    }

    protected fun navigateToOffset(offset: Int) {
        val file = currentFile ?: return
        val vFile = file.virtualFile ?: return
        com.intellij.openapi.fileEditor.OpenFileDescriptor(project, vFile, offset).navigate(true)
    }

    protected fun currentTiBasicFile(): TiBasicFile? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
        val document = editor.document
        return PsiDocumentManager.getInstance(project).getPsiFile(document) as? TiBasicFile
    }

    override fun dispose() = Unit

    private fun setupToolbar() {
        val toolbar = JToolBar().also { it.isFloatable = false }
        toolbar.add(fileLabel)
        add(toolbar, BorderLayout.NORTH)
    }

    private fun refresh() {
        val psiFile = currentTiBasicFile()
        currentFile = psiFile
        fileLabel.text = if (psiFile != null) " ${psiFile.name}" else " (no TI-Basic file active)"
        refreshForFile(psiFile)
    }

    private fun installDocumentListener() {
        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        com.intellij.openapi.editor.EditorFactory.getInstance().eventMulticaster
            .addDocumentListener(object : com.intellij.openapi.editor.event.DocumentListener {
                override fun documentChanged(event: com.intellij.openapi.editor.event.DocumentEvent) {
                    val currentVirtualFile = currentFile?.virtualFile ?: return
                    val changedVirtualFile = FileDocumentManager.getInstance().getFile(event.document) ?: return
                    if (changedVirtualFile != currentVirtualFile || changedVirtualFile.extension !in fileTypeExtensions) {
                        return
                    }
                    psiDocumentManager.performWhenAllCommitted {
                        if (!project.isDisposed && currentFile?.virtualFile == changedVirtualFile) {
                            refresh()
                        }
                    }
                }
            }, this)
    }

    private fun installFileEditorListener() {
        project.messageBus.connect(this).subscribe(
            FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) = refresh()
                override fun selectionChanged(event: com.intellij.openapi.fileEditor.FileEditorManagerEvent) = refresh()
            },
        )
    }
}
