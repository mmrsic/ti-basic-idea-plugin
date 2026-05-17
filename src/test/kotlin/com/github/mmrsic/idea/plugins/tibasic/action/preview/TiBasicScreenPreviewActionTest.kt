package com.github.mmrsic.idea.plugins.tibasic.action.preview

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project

class TiBasicScreenPreviewActionTest : TiBasicTestBase() {

    fun testActionIdResolvesToTiBasicScreenPreviewAction() {
        val action = ActionManager.getInstance().getAction(TiBasicScreenPreviewActionMetadata.actionId)

        assertNotNull(action)
        assertTrue(action is TiBasicScreenPreviewAction)
    }

    fun testActionUsesLocalizedTemplateText() {
        val action = ActionManager.getInstance().getAction(TiBasicScreenPreviewActionMetadata.actionId)

        assertNotNull(action)
        assertEquals("Preview TI-Basic Screen...", action.templatePresentation.text)
    }

    fun `test action is enabled for selection containing CALL HCHAR`() {
        myFixture.configureByText("test.tibasic", "100 CALL HCHAR(1,1,65)")
        val document = myFixture.editor.document
        myFixture.editor.selectionModel.setSelection(document.getLineStartOffset(0), document.getLineEndOffset(0))
        val action = TiBasicScreenPreviewAction()
        val event = actionEvent(action)

        action.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
    }

    fun `test action is disabled when selection has no CALL HCHAR or CALL VCHAR`() {
        myFixture.configureByText("test.tibasic", "100 CALL SCREEN(2)")
        val document = myFixture.editor.document
        myFixture.editor.selectionModel.setSelection(document.getLineStartOffset(0), document.getLineEndOffset(0))
        val action = TiBasicScreenPreviewAction()
        val event = actionEvent(action)

        action.update(event)

        assertTrue(event.presentation.isVisible)
        assertFalse(event.presentation.isEnabled)
    }

    fun `test action evaluates selected lines before showing preview`() {
        myFixture.configureByText(
            "test.tibasic",
            """
            100 CALL HCHAR(1,1,65)
            110 CALL SCREEN(5)
            120 PRINT "UNCHANGED"
            """.trimIndent(),
        )
        val document = myFixture.editor.document
        myFixture.editor.selectionModel.setSelection(document.getLineStartOffset(0), document.getLineEndOffset(1))
        var capturedPreview: TiBasicScreenPreview? = null
        val action = object : TiBasicScreenPreviewAction() {
            override fun showPreview(project: Project, preview: TiBasicScreenPreview) {
                capturedPreview = preview
            }
        }

        myFixture.testAction(action)

        assertNotNull(capturedPreview)
        assertEquals("A", capturedPreview?.cellAt(1, 1)?.displayText)
        assertEquals(com.github.mmrsic.idea.plugins.tibasic.lang.TiColor.DarkBlue, capturedPreview?.cellAt(1, 1)?.bg)
    }

    private fun actionEvent(action: TiBasicScreenPreviewAction): AnActionEvent =
        AnActionEvent.createEvent(
            action,
            SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, project)
                .add(CommonDataKeys.EDITOR, myFixture.editor)
                .add(CommonDataKeys.PSI_FILE, myFixture.file)
                .build(),
            action.templatePresentation.clone(),
            ActionPlaces.UNKNOWN,
            ActionUiKind.NONE,
            null,
        )
}
