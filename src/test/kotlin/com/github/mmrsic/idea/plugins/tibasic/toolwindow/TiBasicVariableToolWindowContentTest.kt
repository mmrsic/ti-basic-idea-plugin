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

    private fun displayedVariableNames(content: TiBasicVariableToolWindowContent): List<String> {
        val tableModelField = TiBasicVariableToolWindowContent::class.java.getDeclaredField("tableModel")
        tableModelField.isAccessible = true
        val tableModel = tableModelField.get(content) as TiBasicVariableTableModel
        return (0 until tableModel.rowCount).map { row ->
            tableModel.getValueAt(row, 0) as String
        }
    }
}
