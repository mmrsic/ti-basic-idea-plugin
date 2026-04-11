package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicBundle
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel

private const val DISPLAY_COLUMN_GUIDE_SETTINGS_TITLE_KEY = "display.columns.settings.title"
private const val DISPLAY_COLUMN_GUIDE_ENABLED_KEY = "display.columns.enabled"

class TiBasicDisplayColumnGuideConfigurable : BoundConfigurable(TiBasicBundle.message(DISPLAY_COLUMN_GUIDE_SETTINGS_TITLE_KEY)) {

    private val settings = TiBasicColumnHintSettings.getInstance()
    private lateinit var enabledCheckBox: JBCheckBox

    override fun createPanel() = panel {
        row {
            cell(createEnabledCheckBox())
        }
    }

    override fun isModified(): Boolean = enabledCheckBox.isSelected != settings.guidesEnabled

    override fun apply() {
        settings.guidesEnabled = enabledCheckBox.isSelected
        TiBasicDisplayColumnGuideController.refreshAllEditors()
    }

    override fun reset() {
        enabledCheckBox.isSelected = settings.guidesEnabled
    }

    private fun createEnabledCheckBox(): JBCheckBox =
        JBCheckBox(TiBasicBundle.message(DISPLAY_COLUMN_GUIDE_ENABLED_KEY), settings.guidesEnabled)
            .also { enabledCheckBox = it }
}

