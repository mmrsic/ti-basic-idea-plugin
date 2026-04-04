package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(name = "TiBasicColumnHintSettings", storages = [Storage("editor.xml")], category = SettingsCategory.CODE)
class TiBasicColumnHintSettings : PersistentStateComponent<TiBasicColumnHintSettings> {

    var displayMode: ColumnHintDisplayMode = ColumnHintDisplayMode.ALL_LINES

    companion object {
        fun getInstance(): TiBasicColumnHintSettings =
            ApplicationManager.getApplication().getService(TiBasicColumnHintSettings::class.java)
    }

    override fun getState(): TiBasicColumnHintSettings = this

    override fun loadState(state: TiBasicColumnHintSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
