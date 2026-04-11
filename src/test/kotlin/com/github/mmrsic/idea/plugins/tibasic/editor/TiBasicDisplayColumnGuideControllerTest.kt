package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicDisplayColumnGuideControllerTest : TiBasicTestBase() {

    fun `test installs renderer highlighter for ti-basic editor`() {
        val previousSetting = TiBasicColumnHintSettings.getInstance().guidesEnabled
        try {
            TiBasicColumnHintSettings.getInstance().guidesEnabled = true
            configureFile("100 PRINT \"12345678901234567890123456789\"")

            val controller = TiBasicDisplayColumnGuideController.install(myFixture.editor)

            assertNotNull(controller)
            assertTrue(
                myFixture.editor.markupModel.allHighlighters.any { highlighter ->
                    highlighter.customRenderer is TiBasicDisplayColumnGuideRenderer
                },
            )
        } finally {
            TiBasicColumnHintSettings.getInstance().guidesEnabled = previousSetting
            TiBasicDisplayColumnGuideController.uninstall(myFixture.editor)
        }
    }

    fun `test does not install renderer highlighter when guides are disabled`() {
        val previousSetting = TiBasicColumnHintSettings.getInstance().guidesEnabled
        try {
            TiBasicColumnHintSettings.getInstance().guidesEnabled = false
            configureFile("100 PRINT \"12345678901234567890123456789\"")

            val controller = TiBasicDisplayColumnGuideController.install(myFixture.editor)

            assertNotNull(controller)
            assertFalse(
                myFixture.editor.markupModel.allHighlighters.any { highlighter ->
                    highlighter.customRenderer is TiBasicDisplayColumnGuideRenderer
                },
            )
        } finally {
            TiBasicColumnHintSettings.getInstance().guidesEnabled = previousSetting
            TiBasicDisplayColumnGuideController.uninstall(myFixture.editor)
        }
    }

    fun `test does not install renderer highlighter for non ti-basic editor`() {
        myFixture.configureByText("test.txt", "12345678901234567890123456789")

        val controller = TiBasicDisplayColumnGuideController.install(myFixture.editor)

        assertNull(controller)
        assertFalse(
            myFixture.editor.markupModel.allHighlighters.any { highlighter ->
                highlighter.customRenderer is TiBasicDisplayColumnGuideRenderer
            },
        )
    }
}

