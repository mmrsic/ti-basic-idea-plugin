package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.debug.TiBasicDebugMetadata
import com.github.mmrsic.idea.plugins.tibasic.debug.TiBasicDebugProgramSnapshot
import com.github.mmrsic.idea.plugins.tibasic.debug.TiBasicDebugSessionService
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil

class TiBasicDebugToolWindowContentTest : TiBasicTestBase() {

    fun `test debug tool window marks current program counter line from snapshot`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            200 PRINT "SECOND"
            100 PRINT "FIRST"
            """.trimIndent(),
        )

        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(" test.tibasic", content.fileLabel.text)
        assertEquals(1, content.listing.selectedIndex)
    }

    fun `test debug tool window shows runtime error after stepping`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile("100 GOTO 999")
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badLineNumberKey), content.messageLabel.text)
        assertTrue(content.statusLabel.text.contains(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowStatusPendingStopKey)))
    }

    fun `test debug tool window shows effective keyboard mode five for CALL KEY zero`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile("100 CALL KEY(0,K,S)")
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(content.keyboardPanel.isVisible)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowKeyboardModeKey, 5, "1-15, 32-159, 187"), content.keyboardModeLabel.text)
        assertEquals("-1", content.keyboardInputField.text)
    }

    fun `test debug tool window applies keyboard input on step`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 CALL KEY(5,K,S)
            110 PRINT K
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.keyboardInputField.text = "186.6"
        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertFalse(content.keyboardPanel.isVisible)
        val entries = (0 until content.numericVariablesModel.size).map(content.numericVariablesModel::get)
        assertTrue(entries.any { it.startsWith("K = >") && it.endsWith("| 187") })
        assertTrue(entries.any { it.startsWith("S = >") && it.endsWith("| 1") })
    }

    fun `test debug tool window lists known string variables in TI-Basic format`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 LET A$="HELLO"
            110 PRINT A$
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(listOf("A$ = 05 H E L L O"), (0 until content.stringVariablesModel.size).map(content.stringVariablesModel::get))
    }

    fun `test debug tool window lists known numeric variables in TI-Basic format`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 LET A=4711
            110 PRINT A
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val entries = (0 until content.numericVariablesModel.size).map(content.numericVariablesModel::get)
        assertEquals(1, entries.size)
        assertTrue(entries.single().startsWith("A = >"))
        assertTrue(entries.single().endsWith("| 4711"))
    }

    fun `test debug tool window shows string truncation warning`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val overlongString = "A".repeat(256)
        val file = configureFile(
            """
            100 LET A$="$overlongString"
            110 PRINT A$
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.stringCutTo255CharactersKey), content.messageLabel.text)
    }

    fun `test debug tool window inspects expressions from input field`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 LET A$="HELLO"
            110 PRINT A$
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        content.inspectField.text = "SEG$(A$,2,3)"
        content.inspectButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("\"ELL\" = 03 E L L", content.inspectResultLabel.text)
    }
}
