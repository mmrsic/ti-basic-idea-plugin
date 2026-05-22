package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(
    name = "TiBasicVariableToolWindowSettings",
    storages = [Storage("editor.xml")],
    category = SettingsCategory.CODE,
)
class TiBasicVariableToolWindowSettings : PersistentStateComponent<TiBasicVariableToolWindowSettings> {

    var showArrayElementConstants: Boolean = false

    companion object {
        fun getInstance(): TiBasicVariableToolWindowSettings =
            ApplicationManager.getApplication().getService(TiBasicVariableToolWindowSettings::class.java)
    }

    override fun getState(): TiBasicVariableToolWindowSettings = this

    override fun loadState(state: TiBasicVariableToolWindowSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
