package com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.debug

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugMetadata
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugProgramSnapshot
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugSessionService
import com.github.mmrsic.idea.plugins.tibasic.language.runtime.screen.tiBasicCharacterPattern
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
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

    fun `test debug tool window keeps current listing row vertically centered when possible`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        content.setSize(1000, 900)
        val file = configureFile(
            (10..220).joinToString("\n") { lineNumber -> "$lineNumber PRINT \"$lineNumber\"" },
        )
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        prepareListingViewport(content.listingScrollPane, width = 320, height = 240)
        repeat(110) {
            content.stepButton.doClick()
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        }

        val viewport = content.listingScrollPane.viewport
        val selectedBounds = content.listing.getCellBounds(content.listing.selectedIndex, content.listing.selectedIndex)

        assertNotNull(selectedBounds)
        selectedBounds!!
        val selectedCenterY = selectedBounds.y - viewport.viewPosition.y + selectedBounds.height / 2
        assertTrue(kotlin.math.abs(selectedCenterY - viewport.extentSize.height / 2) <= selectedBounds.height)
    }

    fun `test debug tool window aligns short listing to top without empty rows above`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        content.setSize(1000, 900)
        val file = configureFile(
            """
            100 PRINT "A"
            110 PRINT "B"
            """.trimIndent(),
        )
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        prepareListingViewport(content.listingScrollPane, width = 320, height = 240)

        assertEquals(0, content.listingScrollPane.viewport.viewPosition.y)
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

    fun `test debug tool window shows keyboard unit and variable names for CALL KEY zero`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile("100 CALL KEY(0,K,S)")
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(content.keyboardPanel.isVisible)
        assertEquals(
            TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowKeyboardUnitKey, 5, "1-15, 32-159, 187"),
            content.keyboardUnitLabel.text,
        )
        assertEquals(
            TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowKeyboardReturnVariableKey, "K"),
            content.keyboardInputLabel.text,
        )
        assertEquals("-1", content.keyboardInputField.text)
        assertEquals(
            TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowKeyboardStatusVariableKey, "S", "0"),
            content.keyboardStatusLabel.text,
        )
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
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(
            TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowKeyboardStatusVariableKey, "S", "1"),
            content.keyboardStatusLabel.text,
        )
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

    fun `test debug tool window shows CALL SCREEN arguments in footer area`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 LET C=2
            110 CALL SCREEN(C+1)
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(content.argumentsPanel.isVisible)
        assertEquals("color-code = 03 (Medium Green)", content.argumentsTextArea.text)
        assertEquals(1, content.argumentsTextArea.rows)
    }

    fun `test debug tool window shows CALL SCREEN string number mismatch in footer area`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile("100 CALL SCREEN(\"A\")")
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(content.argumentsPanel.isVisible)
        assertEquals("<incorrect expression> (string-number-mismatch)", content.argumentsTextArea.text)
    }

    fun `test debug tool window shows CALL CHAR arguments and pixel preview in footer area`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile("100 CALL CHAR(65,\"F0\")")
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(content.argumentsPanel.isVisible)
        assertEquals(
            """
            ascii-code = 65
            pattern-string = F000000000000000
            (pixel-representation)
            """.trimIndent(),
            content.argumentsTextArea.text,
        )
        assertEquals(3, content.argumentsTextArea.rows)
        assertTrue(content.argumentPatternPreviewComponent.isVisible)
        assertEquals("F000000000000000", content.argumentPatternPreviewComponent.hexPattern)
    }

    fun `test debug tool window shows CALL CHAR ignored tail warning in footer area`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile("100 CALL CHAR(65,\"1234567890ABCDEF99\")")
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(
            """
            ascii-code = 65
            pattern-string = 1234567890ABCDEF (ignored tail: 99)
            (pixel-representation)
            """.trimIndent(),
            content.argumentsTextArea.text,
        )
        assertTrue(content.argumentPatternPreviewComponent.isVisible)
        assertEquals("1234567890ABCDEF", content.argumentPatternPreviewComponent.hexPattern)
    }

    fun `test debug tool window shows IF evaluation trace in footer area`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 LET X=2
            110 IF X-1 THEN 300
            300 PRINT "YES"
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(content.argumentsPanel.isVisible)
        assertEquals(
            """
            2 - 1 -> 1
            1 -> true
            """.trimIndent(),
            content.argumentsTextArea.text,
        )
        assertEquals(2, content.argumentsTextArea.rows)
    }

    fun `test debug tool window shows FOR arguments in footer area`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 LET A=2
            110 LET B=4
            120 FOR I=A+1 TO B*2 STEP A-1
            130 NEXT I
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(content.argumentsPanel.isVisible)
        assertEquals(
            """
            initial-value = 03
            limit = 08
            increment = 01
            (iterations = 6)
            """.trimIndent(),
            content.argumentsTextArea.text,
        )
        assertEquals(4, content.argumentsTextArea.rows)
    }

    fun `test debug tool window shows NEXT arguments and jump decision in footer area`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 LET A=2
            110 LET B=1
            120 FOR I=A+1 TO 5 STEP B
            130 NEXT I
            140 PRINT I
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(content.argumentsPanel.isVisible)
        assertEquals(
            """
            increment = 01
            control-variable I = 4
            limit = 05 (jump to 130)
            """.trimIndent(),
            content.argumentsTextArea.text,
        )
        assertEquals(3, content.argumentsTextArea.rows)
    }

    fun `test debug tool window hides arguments footer area when current line has no arguments`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile("100 PRINT \"HELLO\"")
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertFalse(content.argumentsPanel.isVisible)
        assertEquals("", content.argumentsTextArea.text)
    }

    fun `test debug tool window shows multiple arguments on separate lines`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile("100 CALL COLOR(5,3,1)")
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(content.argumentsPanel.isVisible)
        assertEquals(
            """
            character set = 05
            foreground color = 03 (Medium Green)
            background color = 01 (Transparent)
            """.trimIndent(),
            content.argumentsTextArea.text,
        )
        assertEquals(3, content.argumentsTextArea.rows)
    }

    fun `test debug tool window shows CALL HCHAR arguments in footer area`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile("100 CALL HCHAR(2,3,65,4)")
        project.getService(TiBasicDebugSessionService::class.java)
            .startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertTrue(content.argumentsPanel.isVisible)
        assertEquals(
            """
            row = 02
            column = 03
            character-code = 65
            repeat = 04
            """.trimIndent(),
            content.argumentsTextArea.text,
        )
        assertEquals(4, content.argumentsTextArea.rows)
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
        assertEquals((1..16).toSet(), content.screenComponent.state.characterSetColors.keys)
        assertTrue(content.screenComponent.state.characterSetColors.values.all { colors ->
            colors.fg.name == "Black" && colors.bg.name == "Transparent"
        })
        assertTrue(content.keepAspectRatioCheckBox.isSelected)
        assertTrue(content.screenComponent.keepAspectRatio)
        assertTrue(content.characterSetPreviewComponent.keepAspectRatio)
        assertEquals(32 * 8 + 4 + 4, content.screenComponent.preferredSize.width)
        assertEquals(24 * 8, content.screenComponent.preferredSize.height)
        assertEquals(content.screenComponent.preferredSize.width, content.characterSetPreviewComponent.preferredSize.width)
        assertEquals(content.screenComponent.preferredSize.height, content.characterSetPreviewComponent.preferredSize.height)
        assertEquals("LightGreen", content.characterSetPreviewComponent.state.screenBackground.name)
        assertEquals((1..16).toSet(), content.characterSetPreviewComponent.state.characterSetColors.keys)
        assertTrue(content.characterSetPreviewComponent.state.characterSetColors.values.all { colors ->
            colors.fg.name == "Black" && colors.bg.name == "Transparent"
        })
        assertEquals("0020100804081020", tiBasicCharacterPattern(62))
        assertEquals("0000007844784844", tiBasicCharacterPattern(114))
    }

    fun `test debug tool window paints character set preview beside screen`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 CALL SCREEN(2)
            110 CALL COLOR(1,16,2)
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        repeat(2) {
            content.stepButton.doClick()
            PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        }

        val image = BufferedImage(
            content.characterSetPreviewComponent.preferredSize.width,
            content.characterSetPreviewComponent.preferredSize.height,
            BufferedImage.TYPE_INT_RGB,
        )
        content.characterSetPreviewComponent.setSize(content.characterSetPreviewComponent.preferredSize)
        content.characterSetPreviewComponent.paint(image.graphics)

        assertEquals("Black", content.characterSetPreviewComponent.state.screenBackground.name)
        assertEquals("White", content.characterSetPreviewComponent.state.characterSetColors.getValue(1).fg.name)
        val previewBounds = content.characterSetPreviewComponent.scaledPreviewBounds()
        assertEquals(0x000000, image.getRGB(previewBounds.x + 2, previewBounds.y + 2) and 0xFFFFFF)
        val characterSetOrigin = content.characterSetPreviewComponent.characterSetOrigin()
        val firstCharacterX = previewBounds.x + characterSetOrigin.x + 12
        val firstCharacterY = previewBounds.y + characterSetOrigin.y + 5
        val previewHasVisiblePixels = (firstCharacterX until firstCharacterX + 8).any { x ->
            (firstCharacterY until firstCharacterY + 12).any { y -> image.getRGB(x, y) and 0xFFFFFF != 0x000000 }
        }
        assertTrue(previewHasVisiblePixels)
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

    fun `test debug tool window updates character patterns after CALL CHAR step`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 CALL CHAR(65,"F0")
            110 PRINT "A"
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("F000000000000000", content.screenComponent.state.characterPatterns[65])
        assertEquals("F000000000000000", content.characterSetPreviewComponent.state.characterPatterns[65])
    }

    fun `test debug tool window updates character set colors after CALL COLOR step`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 CALL COLOR(5,3,1)
            110 PRINT "A"
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals("MediumGreen", content.screenComponent.state.characterSetColors[5]?.fg?.name)
        assertEquals("Transparent", content.screenComponent.state.characterSetColors[5]?.bg?.name)
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

    fun `test debug tool window updates screen codes after CALL VCHAR step`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        val file = configureFile(
            """
            100 CALL VCHAR(24,32,66,3)
            110 PRINT "A"
            """.trimIndent(),
        )
        val sessionService = project.getService(TiBasicDebugSessionService::class.java)
        sessionService.startSession(TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document))
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        content.stepButton.doClick()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

        assertEquals(66, content.screenComponent.state.characterCodes[23][31])
        assertEquals(66, content.screenComponent.state.characterCodes[0][0])
        assertEquals(66, content.screenComponent.state.characterCodes[1][0])
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
        assertFalse(content.characterSetPreviewComponent.keepAspectRatio)
        assertEquals(400, screenBounds.width)
        assertEquals(400, screenBounds.height)
    }

    fun `test debug tool window scales character set preview to available space while keeping aspect ratio`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        content.characterSetPreviewComponent.setSize(400, 400)

        val previewBounds = content.characterSetPreviewComponent.scaledPreviewBounds()
        val screenBounds = content.screenComponent.apply { setSize(400, 400) }.scaledScreenBounds()

        assertTrue(content.characterSetPreviewComponent.keepAspectRatio)
        assertEquals(screenBounds, previewBounds)
    }

    fun `test debug tool window can scale character set preview without keeping aspect ratio`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)
        content.keepAspectRatioCheckBox.doClick()
        content.characterSetPreviewComponent.setSize(400, 400)

        val previewBounds = content.characterSetPreviewComponent.scaledPreviewBounds()
        val screenBounds = content.screenComponent.apply { setSize(400, 400) }.scaledScreenBounds()

        assertFalse(content.characterSetPreviewComponent.keepAspectRatio)
        assertEquals(screenBounds, previewBounds)
    }

    fun `test debug tool window centers character set preview on second screen`() {
        val content = TiBasicDebugToolWindowContent(project)
        Disposer.register(testRootDisposable, content)

        val characterSetOrigin = content.characterSetPreviewComponent.characterSetOrigin()

        assertEquals((content.screenComponent.preferredSize.width - CHARACTER_SET_PREVIEW_WIDTH) / 2, characterSetOrigin.x)
        assertEquals((content.screenComponent.preferredSize.height - CHARACTER_SET_PREVIEW_HEIGHT) / 2, characterSetOrigin.y)
    }

    private fun prepareListingViewport(
        scrollPane: JBScrollPane,
        width: Int,
        height: Int,
    ) {
        scrollPane.setSize(width, height)
        scrollPane.doLayout()
        scrollPane.viewport.setSize(width, height)
        scrollPane.viewport.doLayout()
        scrollPane.viewport.extentSize = scrollPane.viewport.size
    }
}
