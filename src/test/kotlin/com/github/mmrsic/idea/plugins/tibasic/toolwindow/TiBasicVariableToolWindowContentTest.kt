package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PlatformTestUtil
import java.awt.Dimension
import javax.swing.JLabel
import javax.swing.JTable

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

        assertEquals("10,10,10", displayedValue(content, "A", "Numeric Array", DIMENSIONS_COLUMN))
        assertEquals("1", displayedValue(content, "A", "Numeric Array", BASE_COLUMN))
        assertEquals(listOf(200), displayedOccurrences(content, "A", "Numeric Array", DIM_LINE_COLUMN).map { it.lineNumber })
    }

    fun `test scalar-only programs hide array-specific columns`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 LET A=1\n110 LET B$=\"X\"")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val tableModel = tableModel(content)
        assertEquals(listOf("Name", "Type", "Writes", "Reads", "Range"), displayedColumnNames(tableModel))
        assertFalse(tableModel.hasColumn(DIMENSIONS_COLUMN))
        assertFalse(tableModel.hasColumn(BASE_COLUMN))
        assertFalse(tableModel.hasColumn(DIM_LINE_COLUMN))
    }

    fun `test adding an array shows array-specific columns`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 LET A=1")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        WriteCommandAction.runWriteCommandAction(project) {
            myFixture.editor.document.setText("100 DIM A(10)\n110 LET A(1)=1")
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(
            listOf("Name", "Type", "Dimensions", "Base", "DIM", "Writes", "Reads", "Range"),
            displayedColumnNames(tableModel(content)),
        )
    }

    fun `test range column displays finite value list`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 LET E$=\"HELLO\"\n110 LET F$=\"BYE\"\n120 LET G$=E$\n130 LET G$=F$")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("Range", columnName(tableModel(content), RANGE_COLUMN))
        assertEquals("\"HELLO\", \"BYE\"", displayedValue(content, "G$", "String", RANGE_COLUMN))
    }

    fun `test range column abbreviates consecutive numeric values`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 FOR I=1 TO 5\n110 NEXT I")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("1-5", displayedValue(content, "I", "Numeric", RANGE_COLUMN))
    }

    fun `test rows grow automatically when wrapped cells need more height`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText(
            "test.tibasic",
            "100 LET E$=\"307C6EF8FE7C7C30\"\n110 LET F$=\"0C3E761F7F3E3E0C\"\n120 LET G$=E$\n130 LET G$=F$",
        )
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val table = table(content)
        table.size = Dimension(220, 200)
        val rangeColumn = modelColumnIndex(tableModel(content), RANGE_COLUMN)
        table.columnModel.getColumn(rangeColumn).width = 60
        table.columnModel.getColumn(rangeColumn).preferredWidth = 60
        table.doLayout()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val row = (0 until table.rowCount).first { rowIndex ->
            table.getValueAt(rowIndex, 0) == "G$" && table.getValueAt(rowIndex, 1) == "String"
        }
        assertTrue(table.getRowHeight(row) > table.getFontMetrics(table.font).height + table.rowMargin)
    }

    fun `test variable table headers are left aligned`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 LET A=1")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val table = table(content)
        val header = table.tableHeader
        for (column in 0 until table.columnCount) {
            val headerComponent = header.defaultRenderer.getTableCellRendererComponent(
                table,
                table.columnModel.getColumn(column).headerValue,
                false,
                false,
                -1,
                column,
            )
            val label = headerComponent as? JLabel ?: error("Expected JLabel header renderer for column $column")
            assertEquals(JLabel.LEFT, label.horizontalAlignment)
        }
    }

    fun `test range column displays only unique values`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 LET E$=\"HELLO\"\n110 LET E$=\"HELLO\"")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("Range", columnName(tableModel(content), RANGE_COLUMN))
        assertEquals("\"HELLO\"", displayedValue(content, "E$", "String", RANGE_COLUMN))
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
            tableModel.getValueAt(rowIndex, modelColumnIndex(tableModel, NAME_COLUMN)) == name &&
                tableModel.getValueAt(rowIndex, modelColumnIndex(tableModel, TYPE_COLUMN)) == type
        }
        return tableModel.getValueAt(row, modelColumnIndex(tableModel, column))
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

    private fun displayedColumnNames(tableModel: TiBasicVariableTableModel): List<String> =
        (0 until tableModel.columnCount).map(tableModel::getColumnName)

    private fun columnName(
        tableModel: TiBasicVariableTableModel,
        columnId: Int,
    ): String = tableModel.getColumnName(modelColumnIndex(tableModel, columnId))

    private fun modelColumnIndex(
        tableModel: TiBasicVariableTableModel,
        columnId: Int,
    ): Int = tableModel.modelColumnIndex(columnId) ?: error("Expected visible column $columnId")

    private fun table(content: TiBasicVariableToolWindowContent): JTable {
        val tableField = TiBasicVariableToolWindowContent::class.java.getDeclaredField("table")
        tableField.isAccessible = true
        return tableField.get(content) as JTable
    }
}
