package com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.debug

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugMetadata
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugProgramSnapshot
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugSessionService
import com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen.tiBasicCharacterPattern
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import java.awt.image.BufferedImage

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

    fun `test debug tool window wraps program code after twenty eight characters`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile("100 PRINT \"ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890\"")
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(2, content.listModel.size)
        assertEquals("100", content.listModel.get(0).lineNumber)
        assertEquals(" PRINT \"ABCDEFGHIJKLMNOPQRST", content.listModel.get(0).codeChunk)
        assertNull(content.listModel.get(1).lineNumber)
        assertEquals("UVWXYZ1234567890\"", content.listModel.get(1).codeChunk)
        assertTrue(content.listModel.get(1).continuation)
    }

    fun `test debug tool window shows initial TI screen state`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile("100 PRINT \"HELLO\"")
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(24, content.screenComponent.state.characterCodes.size)
        assertEquals(32, content.screenComponent.state.characterCodes.first().size)
        assertEquals(listOf(62, 32, 114, 117, 110), content.screenComponent.state.characterCodes[22].subList(2, 7))
        assertTrue(
            content.screenComponent.state.characterCodes.withIndex().all { (rowIndex, row) ->
                row.withIndex().all { (columnIndex, code) ->
                    val isRunPromptCell = rowIndex == 22 && columnIndex in 2..6
                    isRunPromptCell || code == 32
                }
            },
        )
        assertEquals("LightGreen", content.screenComponent.state.screenBackground.name)
        assertEquals("Black", content.screenComponent.state.characterForeground.name)
        assertEquals("Transparent", content.screenComponent.state.characterBackground.name)
        assertTrue(content.keepAspectRatioCheckBox.isSelected)
        assertTrue(content.screenComponent.keepAspectRatio)
        assertEquals(32 * 8 + 4 + 4, content.screenComponent.preferredSize.width)
        assertEquals(24 * 8, content.screenComponent.preferredSize.height)
        assertEquals("0020100804081020", tiBasicCharacterPattern(62))
        assertEquals("0000007844784844", tiBasicCharacterPattern(114))
    }

    fun `test debug tool window paints visible screen codes`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile("100 PRINT \"HELLO\"")
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val image = BufferedImage(
            content.screenComponent.preferredSize.width,
            content.screenComponent.preferredSize.height,
            BufferedImage.TYPE_INT_RGB,
        )
        content.screenComponent.setSize(content.screenComponent.preferredSize)
        content.screenComponent.paint(image.graphics)

        val backgroundRgb = content.screenComponent.state.screenBackground.rgbValue
        val promptStartX = 4 + 2 * 8
        val promptStartY = 22 * 8
        val hasVisiblePromptPixel = (promptStartX until promptStartX + 5 * 8).any { x ->
            (promptStartY until promptStartY + 8).any { y -> image.getRGB(x, y) and 0xFFFFFF != backgroundRgb }
        }
        assertTrue(hasVisiblePromptPixel)
    }

    fun `test debug tool window shades area outside TI screen bounds`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile("100 PRINT \"HELLO\"")
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        val image = BufferedImage(
            content.screenComponent.preferredSize.width + 20,
            content.screenComponent.preferredSize.height + 20,
            BufferedImage.TYPE_INT_RGB,
        )
        content.screenComponent.setSize(image.width, image.height)
        content.screenComponent.paint(image.graphics)

        val screenBounds = content.screenComponent.scaledScreenBounds()
        val insideScreenRgb = image.getRGB(screenBounds.x + 2, screenBounds.y + 2) and 0xFFFFFF
        val shadedAreaRgb = image.getRGB(screenBounds.x + 2, screenBounds.y - 1) and 0xFFFFFF
        assertEquals(content.screenComponent.state.screenBackground.rgbValue, insideScreenRgb)
        assertTrue(shadedAreaRgb != insideScreenRgb)
    }

    fun `test debug tool window shows cleared screen after CALL CLEAR step`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 CALL CLEAR
            110 PRINT "HELLO"
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(content.screenComponent.state.characterCodes.flatten().all { code -> code == 32 })
    }

    fun `test debug tool window keeps screen background after CALL CLEAR step`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 CALL SCREEN(2)
            110 CALL CLEAR
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("Black", content.screenComponent.state.screenBackground.name)
    }

    fun `test debug tool window updates screen background after CALL SCREEN step`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 CALL SCREEN(2)
            110 PRINT "HELLO"
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("Black", content.screenComponent.state.screenBackground.name)
    }

    fun `test debug tool window renders PRINT output in the screen area`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 PRINT "HI"
            110 PRINT "NEXT"
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals('H'.code, content.screenComponent.state.characterCodes[22][2])
        assertEquals('I'.code, content.screenComponent.state.characterCodes[22][3])
        assertEquals('N'.code, content.screenComponent.state.characterCodes[23][2])
        assertEquals('E'.code, content.screenComponent.state.characterCodes[23][3])
        assertEquals('X'.code, content.screenComponent.state.characterCodes[23][4])
        assertEquals('T'.code, content.screenComponent.state.characterCodes[23][5])
    }

    fun `test debug tool window scales screen to available space while keeping aspect ratio`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        content.screenComponent.setSize(400, 400)

        val screenBounds = content.screenComponent.scaledScreenBounds()

        assertEquals(400, screenBounds.width)
        assertTrue(screenBounds.height < 400)
    }

    fun `test debug tool window can scale screen without keeping aspect ratio`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        content.keepAspectRatioCheckBox.doClick()
        content.screenComponent.setSize(400, 400)

        val screenBounds = content.screenComponent.scaledScreenBounds()

        assertFalse(content.keepAspectRatioCheckBox.isSelected)
        assertFalse(content.screenComponent.keepAspectRatio)
        assertEquals(400, screenBounds.width)
        assertEquals(400, screenBounds.height)
    }
}
