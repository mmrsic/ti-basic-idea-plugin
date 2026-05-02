package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

private const val CONTROLLER_REM_LINE_PREFIX = "100 REM "

class TiBasicDisplayColumnGuideControllerTest : TiBasicTestBase() {

    fun `test installs renderer highlighter for ti-basic editor`() {
        withGuideSettings(guidesEnabled = true, guidePreviewDistance = 0) {
            configureFile(remLine(29))
            val controller = TiBasicDisplayColumnGuideController.install(myFixture.editor)

            assertNotNull(controller)
            assertTrue(hasDisplayColumnGuideRenderer())
        }
    }

    fun `test does not install renderer highlighter when guides are disabled`() {
        withGuideSettings(guidesEnabled = false, guidePreviewDistance = DEFAULT_DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE) {
            configureFile(remLine(29))
            val controller = TiBasicDisplayColumnGuideController.install(myFixture.editor)

            assertNotNull(controller)
            assertFalse(hasDisplayColumnGuideRenderer())
        }
    }

    fun `test installs renderer highlighter within preview distance before first wrap`() {
        withGuideSettings(guidesEnabled = true, guidePreviewDistance = DEFAULT_DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE) {
            configureFile(remLine(26))

            val controller = TiBasicDisplayColumnGuideController.install(myFixture.editor)

            assertNotNull(controller)
            assertTrue(hasDisplayColumnGuideRenderer())
        }
    }

    fun `test does not install renderer highlighter for non ti-basic editor`() {
        myFixture.configureByText("test.txt", "12345678901234567890123456789")

        val controller = TiBasicDisplayColumnGuideController.install(myFixture.editor)

        assertNull(controller)
        assertFalse(
            hasDisplayColumnGuideRenderer(),
        )
    }

    private fun hasDisplayColumnGuideRenderer(): Boolean =
        myFixture.editor.markupModel.allHighlighters.any { highlighter ->
            highlighter.customRenderer is TiBasicDisplayColumnGuideRenderer
        }

    private fun remLine(totalLineLength: Int): String =
        CONTROLLER_REM_LINE_PREFIX + "A".repeat(totalLineLength - CONTROLLER_REM_LINE_PREFIX.length)

    private inline fun withGuideSettings(guidesEnabled: Boolean, guidePreviewDistance: Int, block: () -> Unit) {
        val settings = TiBasicColumnHintSettings.getInstance()
        val previousGuidesEnabled = settings.guidesEnabled
        val previousGuidePreviewDistance = settings.guidePreviewDistance
        try {
            settings.guidesEnabled = guidesEnabled
            settings.guidePreviewDistance = guidePreviewDistance
            block()
        } finally {
            settings.guidesEnabled = previousGuidesEnabled
            settings.guidePreviewDistance = previousGuidePreviewDistance
            TiBasicDisplayColumnGuideController.uninstall(myFixture.editor)
        }
    }
}
