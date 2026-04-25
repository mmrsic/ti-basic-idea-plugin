package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

const val DEFAULT_AUTO_LINE_NUMBER_DELTA = 10

@Service(Service.Level.APP)
@State(
    name = "TiBasicAutoLineNumberSettings",
    storages = [Storage("editor.xml")],
    category = SettingsCategory.CODE,
)
class TiBasicAutoLineNumberSettings : PersistentStateComponent<TiBasicAutoLineNumberSettings> {

    var autoLineNumberDelta: Int = DEFAULT_AUTO_LINE_NUMBER_DELTA
    var roundToTens: Boolean = false

    companion object {
        fun getInstance(): TiBasicAutoLineNumberSettings =
            ApplicationManager.getApplication().getService(TiBasicAutoLineNumberSettings::class.java)
    }

    override fun getState(): TiBasicAutoLineNumberSettings = this

    override fun loadState(state: TiBasicAutoLineNumberSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
