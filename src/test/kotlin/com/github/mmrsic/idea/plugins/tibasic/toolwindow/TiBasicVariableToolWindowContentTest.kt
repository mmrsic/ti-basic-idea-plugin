package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PlatformTestUtil

class TiBasicVariableToolWindowContentTest : TiBasicTestBase() {

    fun `test current TI-Basic file changes refresh displayed variables`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)

        myFixture.configureByText("test.tibasic", "100 LET A=1")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        assertEquals(listOf("A"), displayedVariableNames(content))

        WriteCommandAction.runWriteCommandAction(project) {
            myFixture.editor.document.setText("100 LET B=1")
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(listOf("B"), displayedVariableNames(content))
    }

    fun `test non TI-Basic document changes do not trigger invokeLater side effects`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 LET A=1")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(listOf("A"), displayedVariableNames(content))

        val virtualFile = myFixture.tempDirFixture.createFile(
            "README.md",
            "| A | B |\n| --- | --- |\n| 1 | 2 |",
        )
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
            ?: error("Expected document for ${virtualFile.path}")

        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(document.textLength, "\n| 3 | 4 |")
        }

        PsiDocumentManager.getInstance(project).commitAllDocuments()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(listOf("A"), displayedVariableNames(content))
    }

    fun `test array rows display dimensions and option base`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 OPTION BASE 1\n200 DIM A(10,10,10)\n300 LET A(1,1,1)=5")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("10,10,10", displayedValue(content, "A", "Array Declaration", DIMENSIONS_COLUMN))
        assertEquals("1", displayedValue(content, "A", "Array Declaration", BASE_COLUMN))
        assertEquals(listOf(200), displayedOccurrences(content, "A", "Array Declaration", DIM_LINE_COLUMN).map { it.lineNumber })
        assertEquals("10,10,10", displayedValue(content, "A", "Numeric Array", DIMENSIONS_COLUMN))
        assertEquals("1", displayedValue(content, "A", "Numeric Array", BASE_COLUMN))
        assertTrue(displayedOccurrences(content, "A", "Numeric Array", DIM_LINE_COLUMN).isEmpty())
    }

    private fun displayedVariableNames(content: TiBasicVariableToolWindowContent): List<String> {
        val tableModel = tableModel(content)
        return (0 until tableModel.rowCount).map { row ->
            tableModel.getValueAt(row, 0) as String
        }
    }

    private fun displayedValue(
        content: TiBasicVariableToolWindowContent,
        name: String,
        type: String,
        column: Int,
    ): Any? {
        val tableModel = tableModel(content)
        val row = (0 until tableModel.rowCount).first { rowIndex ->
            tableModel.getValueAt(rowIndex, 0) == name && tableModel.getValueAt(rowIndex, 1) == type
        }
        return tableModel.getValueAt(row, column)
    }

    private fun displayedOccurrences(
        content: TiBasicVariableToolWindowContent,
        name: String,
        type: String,
        column: Int,
    ): List<TiBasicVariableOccurrence> =
        (displayedValue(content, name, type, column) as? List<*>)
            ?.filterIsInstance<TiBasicVariableOccurrence>()
            ?: emptyList()

    private fun tableModel(content: TiBasicVariableToolWindowContent): TiBasicVariableTableModel {
        val tableModelField = TiBasicVariableToolWindowContent::class.java.getDeclaredField("tableModel")
        tableModelField.isAccessible = true
        return tableModelField.get(content) as TiBasicVariableTableModel
    }
}
