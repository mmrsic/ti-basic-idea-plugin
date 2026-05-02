package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class TiBasicColumnHintSettingsTest : TiBasicTestBase() {

    fun `test negative preview distance in persisted state is sanitized`() {
        val settings = TiBasicColumnHintSettings()

        settings.loadState(TiBasicColumnHintSettings.State(guidePreviewDistance = -1))

        assertEquals(MIN_DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE, settings.guidePreviewDistance)
    }
}
