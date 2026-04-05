package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SettingsCategory
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(
    name = "TiBasicParenAutoCloseSettings",
    storages = [Storage("editor.xml")],
    category = SettingsCategory.CODE,
)
class TiBasicParenAutoCloseSettings : PersistentStateComponent<TiBasicParenAutoCloseSettings> {

    var autoCloseOnShiftEnter: Boolean = true
    var autoCloseOnEnter: Boolean = false

    companion object {
        fun getInstance(): TiBasicParenAutoCloseSettings =
            ApplicationManager.getApplication().getService(TiBasicParenAutoCloseSettings::class.java)
    }

    override fun getState(): TiBasicParenAutoCloseSettings = this

    override fun loadState(state: TiBasicParenAutoCloseSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
