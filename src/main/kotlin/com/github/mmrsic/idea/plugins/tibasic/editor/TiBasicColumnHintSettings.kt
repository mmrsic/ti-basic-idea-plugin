package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.APP)
@State(name = "TiBasicColumnHintSettings", storages = [Storage("editor.xml")], category = SettingsCategory.CODE)
class TiBasicColumnHintSettings : PersistentStateComponent<TiBasicColumnHintSettings.State> {

    var guidesEnabled: Boolean = true

    data class State(
        var guidesEnabled: Boolean = true,
        var displayMode: String? = null,
    )

    companion object {
        private const val LEGACY_DISABLED_MODE = "DISABLED"

        fun getInstance(): TiBasicColumnHintSettings =
            ApplicationManager.getApplication().getService(TiBasicColumnHintSettings::class.java)
    }

    override fun getState(): State = State(guidesEnabled = guidesEnabled)

    override fun loadState(state: State) {
        guidesEnabled = state.displayMode?.let(::guidesEnabledFromLegacyMode) ?: state.guidesEnabled
    }

    private fun guidesEnabledFromLegacyMode(displayMode: String): Boolean = displayMode != LEGACY_DISABLED_MODE
}
