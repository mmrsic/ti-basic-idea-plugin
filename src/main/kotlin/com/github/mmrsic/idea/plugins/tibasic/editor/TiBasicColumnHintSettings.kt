package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

internal const val DEFAULT_DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE = 2
internal const val MIN_DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE = 0

@Service(Service.Level.APP)
@State(name = "TiBasicColumnHintSettings", storages = [Storage("editor.xml")], category = SettingsCategory.CODE)
class TiBasicColumnHintSettings : PersistentStateComponent<TiBasicColumnHintSettings.State> {

    var guidesEnabled: Boolean = true
    var guidePreviewDistance: Int = DEFAULT_DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE

    data class State(
        var guidesEnabled: Boolean = true,
        var guidePreviewDistance: Int = DEFAULT_DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE,
        var displayMode: String? = null,
    )

    companion object {
        private const val LEGACY_DISABLED_MODE = "DISABLED"

        fun getInstance(): TiBasicColumnHintSettings =
            ApplicationManager.getApplication().getService(TiBasicColumnHintSettings::class.java)
    }

    override fun getState(): State = State(
        guidesEnabled = guidesEnabled,
        guidePreviewDistance = guidePreviewDistance,
    )

    override fun loadState(state: State) {
        guidesEnabled = state.displayMode?.let(::guidesEnabledFromLegacyMode) ?: state.guidesEnabled
        guidePreviewDistance = sanitizeGuidePreviewDistance(state.guidePreviewDistance)
    }

    private fun guidesEnabledFromLegacyMode(displayMode: String): Boolean = displayMode != LEGACY_DISABLED_MODE

    private fun sanitizeGuidePreviewDistance(guidePreviewDistance: Int): Int =
        guidePreviewDistance.coerceAtLeast(MIN_DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE)
}
