package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.TableRowSorter

class TiBasicVariableToolWindowContent(private val project: Project) : JPanel(BorderLayout()) {

    private val tableModel = TiBasicVariableTableModel()
    private val lineNumberRenderer = TiBasicVariableLineNumberRenderer()
    private val table = JBTable(tableModel)
    private val fileLabel = JLabel(" ")
    private var currentFile: TiBasicFile? = null
    private val activeHighlighters = ArrayList<RangeHighlighter>()

    init {
        setupTable()
        setupToolbar()
        add(JBScrollPane(table), BorderLayout.CENTER)
        installDocumentListener()
        installFileEditorListener()
        refresh()
    }

    private fun setupTable() {
        table.setDefaultRenderer(List::class.java, lineNumberRenderer)
        val sorter = TableRowSorter(tableModel)
        val lineNumberComparator = compareBy<Any?> {
            (it as? List<*>)
                ?.filterIsInstance<TiBasicVariableOccurrence>()
                ?.minOfOrNull { occ -> occ.lineNumber }
                ?: Int.MAX_VALUE
        }
        sorter.setComparator(WRITES_COLUMN, lineNumberComparator)
        sorter.setComparator(READS_COLUMN, lineNumberComparator)
        table.rowSorter = sorter
        sorter.sortKeys = listOf(RowSorter.SortKey(0, SortOrder.ASCENDING))
        table.addMouseListener(LineNumberClickHandler())
        table.selectionModel.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) updateHighlightsForSelection()
        }
    }

    private fun setupToolbar() {
        val toolbar = JToolBar().also { it.isFloatable = false }
        toolbar.add(fileLabel)
        add(toolbar, BorderLayout.NORTH)
    }

    private fun refresh() {
        clearHighlights()
        val psiFile = currentTiBasicFile()
        currentFile = psiFile
        fileLabel.text = if (psiFile != null) " ${psiFile.name}" else " (no TI-Basic file active)"
        val entries = if (psiFile != null) TiBasicVariableCollector.collect(psiFile) else emptyList()
        tableModel.updateEntries(entries)
    }

    private fun updateHighlightsForSelection() {
        val selectedRow = table.selectedRow
        if (selectedRow < 0) {
            clearHighlights()
            return
        }
        val modelRow = table.convertRowIndexToModel(selectedRow)
        highlightVariable(tableModel.entryAt(modelRow))
    }

    private fun highlightVariable(entry: TiBasicVariableEntry) {
        clearHighlights()
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val highlightManager = HighlightManager.getInstance(project)
        val nameLength = entry.name.length
        for (occ in entry.occurrences) {
            val attributesKey = if (occ.accessType == AccessType.WRITE)
                EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES
            else
                EditorColors.SEARCH_RESULT_ATTRIBUTES
            highlightManager.addRangeHighlight(
                editor, occ.offset, occ.offset + nameLength,
                attributesKey, false, activeHighlighters,
            )
        }
    }

    private fun clearHighlights() {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            val highlightManager = HighlightManager.getInstance(project)
            activeHighlighters.forEach { highlightManager.removeSegmentHighlighter(editor, it) }
        }
        activeHighlighters.clear()
    }

    private fun currentTiBasicFile(): TiBasicFile? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
        val document = editor.document
        return PsiDocumentManager.getInstance(project).getPsiFile(document) as? TiBasicFile
    }

    private fun installDocumentListener() {
        com.intellij.openapi.editor.EditorFactory.getInstance().eventMulticaster
            .addDocumentListener(object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(event.document) as? TiBasicFile
                        ?: return
                    if (psiFile === currentFile || currentFile == null) refresh()
                }
            }, project)
    }

    private fun installFileEditorListener() {
        project.messageBus.connect().subscribe(
            FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) = refresh()
                override fun selectionChanged(event: com.intellij.openapi.fileEditor.FileEditorManagerEvent) = refresh()
            },
        )
    }

    private inner class LineNumberClickHandler : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            val row = table.rowAtPoint(e.point)
            val col = table.columnAtPoint(e.point)
            if (row < 0 || col !in WRITES_COLUMN..READS_COLUMN) return

            val cellRect = table.getCellRect(row, col, false)
            val relX = e.x - cellRect.x

            val modelRow = table.convertRowIndexToModel(row)
            val occurrences = (tableModel.getValueAt(modelRow, col) as? List<*>)
                ?.filterIsInstance<TiBasicVariableOccurrence>() ?: return

            table.prepareRenderer(lineNumberRenderer, row, col)
            lineNumberRenderer.panel.setSize(cellRect.width, cellRect.height)
            lineNumberRenderer.panel.doLayout()

            val clickedLabel = lineNumberRenderer.panel.components
                .firstOrNull { c -> c is JLabel && c.x <= relX && relX < c.x + c.width } as? JLabel
                ?: return
            val lineNum = clickedLabel.text.toIntOrNull() ?: return

            val occurrence = occurrences.firstOrNull { it.lineNumber == lineNum } ?: return
            navigateToOffset(occurrence.offset)
        }
    }

    private fun navigateToOffset(offset: Int) {
        val file = currentFile ?: return
        val vFile = file.virtualFile ?: return
        com.intellij.openapi.fileEditor.OpenFileDescriptor(project, vFile, offset).navigate(true)
    }
}
