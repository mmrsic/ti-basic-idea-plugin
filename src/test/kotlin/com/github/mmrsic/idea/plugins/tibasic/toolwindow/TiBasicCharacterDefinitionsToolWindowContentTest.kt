package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PlatformTestUtil
import javax.swing.JTable

class TiBasicCharacterDefinitionsToolWindowContentTest : TiBasicTestBase() {

    fun `test current TI-Basic file changes refresh displayed character definitions`() {
        val content = TiBasicCharacterDefinitionsToolWindowContent(project)
        Disposer.register(testRootDisposable, content)

        myFixture.configureByText("test.tibasic", "100 CALL CHAR(65,\"FF\")")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        assertEquals(listOf(65 to "FF00000000000000"), displayedDefinitions(content))

        WriteCommandAction.runWriteCommandAction(project) {
            myFixture.editor.document.setText("100 CALL CHAR(66,\"0F\")")
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(listOf(66 to "0F00000000000000"), displayedDefinitions(content))
    }

    fun `test non TI-Basic document changes do not trigger character definition refresh`() {
        val content = TiBasicCharacterDefinitionsToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        myFixture.configureByText("test.tibasic", "100 CALL CHAR(65,\"FF\")")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(listOf(65 to "FF00000000000000"), displayedDefinitions(content))

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

        assertEquals(listOf(65 to "FF00000000000000"), displayedDefinitions(content))
    }

    fun `test displayed character definitions include pictogram icons`() {
        val content = TiBasicCharacterDefinitionsToolWindowContent(project)
        Disposer.register(testRootDisposable, content)

        myFixture.configureByText("test.tibasic", "100 CALL CHAR(65,\"FF\")")
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val tableModel = tableModel(content)
        assertEquals(5, tableModel.columnCount)
        assertEquals("Icon", tableModel.getColumnName(CHARACTER_ICON_COLUMN))
        assertEquals(1, (tableModel.getValueAt(0, CHARACTER_ICON_COLUMN) as TiBasicCharacterIcons).icons.size)
    }

    fun `test displayed character definitions include distinct color variants from CALL COLOR`() {
        val content = TiBasicCharacterDefinitionsToolWindowContent(project)
        Disposer.register(testRootDisposable, content)

        myFixture.configureByText(
            "test.tibasic",
            """
            100 CALL CHAR(65,"FF")
            110 CALL COLOR(5,7,16)
            120 CALL COLOR(5,7,16)
            130 CALL COLOR(5,2,3)
            """.trimIndent(),
        )
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val icons = tableModel(content).getValueAt(0, CHARACTER_ICON_COLUMN) as TiBasicCharacterIcons
        assertEquals(3, icons.icons.size)
        assertEquals(
            listOf(
                TiBasicCharacterColorVariant(
                    fg = com.github.mmrsic.idea.plugins.tibasic.lang.TiColor.DarkRed,
                    bg = com.github.mmrsic.idea.plugins.tibasic.lang.TiColor.White,
                ),
                TiBasicCharacterColorVariant(
                    fg = com.github.mmrsic.idea.plugins.tibasic.lang.TiColor.Black,
                    bg = com.github.mmrsic.idea.plugins.tibasic.lang.TiColor.MediumGreen,
                ),
            ),
            icons.colorVariants,
        )
    }

    fun `test repeated visual character definitions keep separate codes`() {
        val content = TiBasicCharacterDefinitionsToolWindowContent(project)
        Disposer.register(testRootDisposable, content)

        myFixture.configureByText(
            "test.tibasic",
            """
            100 CALL CHAR(65,"FF")
            110 CALL CHAR(66,"FF")
            120 CALL COLOR(5,2,3)
            """.trimIndent(),
        )
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(
            listOf(
                65 to "FF00000000000000",
                66 to "FF00000000000000",
            ),
            displayedDefinitions(content),
        )
    }

    fun `test identical character definitions with different line numbers are grouped into one row`() {
        val content = TiBasicCharacterDefinitionsToolWindowContent(project)
        Disposer.register(testRootDisposable, content)

        myFixture.configureByText(
            "test.tibasic",
            """
            100 CALL CHAR(65,"FF")
            120 CALL CHAR(65,"FF")
            110 CALL CHAR(65,"0F")
            """.trimIndent(),
        )
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(
            listOf(
                65 to "FF00000000000000",
                65 to "0F00000000000000",
            ),
            displayedDefinitions(content),
        )
        assertEquals(listOf(100, 120), displayedLineNumbers(content, 0))
        assertEquals(listOf(110), displayedLineNumbers(content, 1))
    }

    fun `test icon renderer creates one label per displayed icon`() {
        val renderer = TiBasicCharacterDefinitionIconRenderer()
        val table = JTable(1, 1)

        renderer.getTableCellRendererComponent(
            table = table,
            value = TiBasicCharacterIcons(
                pattern = "FF00000000000000",
                colorVariants = listOf(
                    TiBasicCharacterColorVariant(
                        fg = com.github.mmrsic.idea.plugins.tibasic.lang.TiColor.DarkRed,
                        bg = com.github.mmrsic.idea.plugins.tibasic.lang.TiColor.White,
                    ),
                    TiBasicCharacterColorVariant(
                        fg = com.github.mmrsic.idea.plugins.tibasic.lang.TiColor.Black,
                        bg = com.github.mmrsic.idea.plugins.tibasic.lang.TiColor.MediumGreen,
                    ),
                ),
            ),
            isSelected = false,
            hasFocus = false,
            row = 0,
            column = 0,
        )

        assertEquals(3, renderer.panel.componentCount)
    }

    private fun displayedDefinitions(content: TiBasicCharacterDefinitionsToolWindowContent): List<Pair<Int, String>> {
        val tableModel = tableModel(content)
        return (0 until tableModel.rowCount).map { row ->
            val code = tableModel.getValueAt(row, CHARACTER_CODE_COLUMN) as Int
            val pattern = tableModel.getValueAt(row, CHARACTER_PATTERN_COLUMN) as String
            code to pattern
        }
    }

    private fun displayedLineNumbers(
        content: TiBasicCharacterDefinitionsToolWindowContent,
        row: Int,
    ): List<Int> =
        (tableModel(content).getValueAt(row, CHARACTER_LINE_COLUMN) as? List<*>)
            ?.filterIsInstance<TiBasicCharacterDefinitionOccurrence>()
            ?.map(TiBasicCharacterDefinitionOccurrence::lineNumber)
            .orEmpty()

    private fun tableModel(content: TiBasicCharacterDefinitionsToolWindowContent): TiBasicCharacterDefinitionsTableModel {
        val tableModelField = TiBasicCharacterDefinitionsToolWindowContent::class.java.getDeclaredField("tableModel")
        tableModelField.isAccessible = true
        return tableModelField.get(content) as TiBasicCharacterDefinitionsTableModel
    }
}
