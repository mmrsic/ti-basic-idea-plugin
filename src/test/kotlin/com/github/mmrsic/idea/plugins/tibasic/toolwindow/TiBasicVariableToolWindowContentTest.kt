package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PlatformTestUtil
import java.awt.Dimension
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.RowSorter
import javax.swing.SortOrder

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

    fun `test refresh keeps user selected sort order`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 LET A=1\n110 LET B=1")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val table = table(content)
        table.rowSorter.sortKeys = listOf(RowSorter.SortKey(NAME_COLUMN, SortOrder.DESCENDING))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        assertEquals(listOf("B", "A"), displayedVariableNamesInViewOrder(content))

        WriteCommandAction.runWriteCommandAction(project) {
            myFixture.editor.document.setText("100 LET A=2\n110 LET B=1")
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(listOf(RowSorter.SortKey(NAME_COLUMN, SortOrder.DESCENDING)), table.rowSorter.sortKeys)
        assertEquals(listOf("B", "A"), displayedVariableNamesInViewOrder(content))
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

    fun `test array rows display combined dimensions declaration`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 OPTION BASE 1\n200 DIM A(10,10,10)\n300 LET A(1,1,1)=5")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val dimensionsDisplay = displayedDimensions(content, "A", "Numeric Array")
        assertEquals("DIM A(1-10,1-10,1-10)", dimensionsDisplay.text)
        assertEquals(listOf(200), dimensionsDisplay.occurrences.map { it.lineNumber })
    }

    fun `test scalar-only programs hide array-specific columns`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 LET A=1\n110 LET B$=\"X\"")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val tableModel = tableModel(content)
        assertEquals(listOf("Name", "Type", "Writes", "Reads", "Range"), displayedColumnNames(tableModel))
        assertFalse(tableModel.hasColumn(DIMENSIONS_COLUMN))
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
            listOf("Name", "Type", "Dimensions", "Writes", "Reads", "Range"),
            displayedColumnNames(tableModel(content)),
        )
    }

    fun `test range column displays finite value list`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 LET E$=\"HELLO\"\n110 LET F$=\"BYE\"\n120 LET G$=E$\n130 LET G$=F$")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("Range", columnName(tableModel(content), RANGE_COLUMN))
        assertEquals("\"BYE\", \"HELLO\"", displayedValue(content, "G$", "String", RANGE_COLUMN))
    }

    fun `test range column abbreviates consecutive numeric values`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 FOR I=1 TO 5\n110 NEXT I")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("[1; 5]", displayedValue(content, "I", "Numeric", RANGE_COLUMN))
    }

    fun `test range column shows descending FOR values in ascending order`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 FOR I=5 TO 1 STEP -1\n110 NEXT I")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("[1; 5]", displayedValue(content, "I", "Numeric", RANGE_COLUMN))
    }

    fun `test range column shows incremented scalar values from nested FOR loops`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText(
            "test.tibasic",
            """
            970 S=31
            980 FOR H=9 TO 16
            990 FOR V=10 TO 25
            1000 S=S+1
            1010 CALL HCHAR(H,V,S)
            1020 NEXT V
            1030 NEXT H
            """.trimIndent(),
        )
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("[31; 159]", displayedValue(content, "S", "Numeric", RANGE_COLUMN))
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

    fun `test array constants toggle controls range display on demand`() {
        TiBasicVariableToolWindowSettings.getInstance().showArrayElementConstants = false
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 LET I=1\n110 LET P$(I)=\"FF\"")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(null, displayedValue(content, "P$", "String Array", RANGE_COLUMN))

        constantsToggle(content).doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("(1)=\"FF\"", displayedValue(content, "P$", "String Array", RANGE_COLUMN))
        TiBasicVariableToolWindowSettings.getInstance().showArrayElementConstants = false
    }

    fun `test array constants toggle refreshes on EDT without explicit read action`() {
        TiBasicVariableToolWindowSettings.getInstance().showArrayElementConstants = false
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 LET I=1\n110 LET P$(I)=\"FF\"")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        ApplicationManager.getApplication().invokeAndWait {
            constantsToggle(content).doClick()
        }
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("(1)=\"FF\"", displayedValue(content, "P$", "String Array", RANGE_COLUMN))
        TiBasicVariableToolWindowSettings.getInstance().showArrayElementConstants = false
    }

    fun `test range column shows READ DATA derived array element values when enabled`() {
        TiBasicVariableToolWindowSettings.getInstance().showArrayElementConstants = true
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 READ A$\n110 LET F$(1)=A$\n120 DATA \"X\"")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("(1)=\"X\"", displayedValue(content, "F$", "String Array", RANGE_COLUMN))
        TiBasicVariableToolWindowSettings.getInstance().showArrayElementConstants = false
    }

    fun `test range column compacts consecutive single dimension array constants with same value`() {
        TiBasicVariableToolWindowSettings.getInstance().showArrayElementConstants = true
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText(
            "test.tibasic",
            """
            590 FOR I=0 TO 127
            600 A$(I)="0"
            610 NEXT I
            """.trimIndent(),
        )
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("(0-127)=\"0\"", displayedValue(content, "A$", "String Array", RANGE_COLUMN))
        TiBasicVariableToolWindowSettings.getInstance().showArrayElementConstants = false
    }

    fun `test range column stays empty for string arrays with more than ten constant elements`() {
        TiBasicVariableToolWindowSettings.getInstance().showArrayElementConstants = true
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText(
            "test.tibasic",
            (1..11).joinToString("\n") { index -> "${index * 10} LET F$($index)=\"V$index\"" },
        )
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(null, displayedValue(content, "F$", "String Array", RANGE_COLUMN))
        TiBasicVariableToolWindowSettings.getInstance().showArrayElementConstants = false
    }

    fun `test range column shows CALL KEY status as bracketed fixed range`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 CALL KEY(0,K,S)")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("[-1; 1]", displayedValue(content, "S", "Numeric", RANGE_COLUMN))
    }

    fun `test range column shows CALL KEY status interval plus separate literal`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "1 CALL KEY(0,K,S)\n2 PRINT S\n3 S=4")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("[-1; 1], 4", displayedValue(content, "S", "Numeric", RANGE_COLUMN))
    }

    fun `test range column merges CALL KEY status interval with adjacent literal`() {
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "1 CALL KEY(0,K,S)\n2 S=2")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("[-1; 2]", displayedValue(content, "S", "Numeric", RANGE_COLUMN))
    }

    fun `test range column grows row height for wrapped array constants`() {
        TiBasicVariableToolWindowSettings.getInstance().showArrayElementConstants = true
        val content = TiBasicVariableToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText(
            "test.tibasic",
            """
            100 LET F$(10)="TEN"
            110 LET F$(2)="TWO"
            120 LET F$(1)="ONE"
            130 LET F$(11)="ELEVEN"
            """.trimIndent(),
        )
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val table = table(content)
        table.size = Dimension(260, 200)
        val rangeColumn = modelColumnIndex(tableModel(content), RANGE_COLUMN)
        table.columnModel.getColumn(rangeColumn).width = 70
        table.columnModel.getColumn(rangeColumn).preferredWidth = 70
        table.doLayout()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val row = (0 until table.rowCount).first { rowIndex ->
            table.getValueAt(rowIndex, 0) == "F$" && table.getValueAt(rowIndex, 1) == "String Array"
        }
        assertEquals(
            "(1)=\"ONE\"; (2)=\"TWO\"; (10)=\"TEN\"; (11)=\"ELEVEN\"",
            table.getValueAt(row, rangeColumn),
        )
        assertTrue(table.getRowHeight(row) > table.getFontMetrics(table.font).height + table.rowMargin)
        TiBasicVariableToolWindowSettings.getInstance().showArrayElementConstants = false
    }

    private fun displayedVariableNames(content: TiBasicVariableToolWindowContent): List<String> {
        val tableModel = tableModel(content)
        return (0 until tableModel.rowCount).map { row ->
            tableModel.getValueAt(row, 0) as String
        }
    }

    private fun displayedVariableNamesInViewOrder(content: TiBasicVariableToolWindowContent): List<String> {
        val table = table(content)
        return (0 until table.rowCount).map { row ->
            table.getValueAt(row, NAME_COLUMN) as String
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

    private fun displayedDimensions(
        content: TiBasicVariableToolWindowContent,
        name: String,
        type: String,
    ): TiBasicVariableDimensionsDisplay =
        displayedValue(content, name, type, DIMENSIONS_COLUMN) as? TiBasicVariableDimensionsDisplay
            ?: error("Expected dimensions display for $name")

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

    private fun constantsToggle(content: TiBasicVariableToolWindowContent): JCheckBox {
        val toggleField = TiBasicVariableToolWindowContent::class.java.getDeclaredField("constantsToggle")
        toggleField.isAccessible = true
        return toggleField.get(content) as JCheckBox
    }
}
