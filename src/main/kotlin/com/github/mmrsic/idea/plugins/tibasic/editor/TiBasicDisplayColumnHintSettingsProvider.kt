package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicBundle
import com.intellij.codeInsight.hints.declarative.InlayHintsCustomSettingsProvider
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBRadioButton
import java.awt.GridLayout
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JPanel

class TiBasicDisplayColumnHintSettingsProvider : InlayHintsCustomSettingsProvider<ColumnHintDisplayMode> {

    private var currentMode = TiBasicColumnHintSettings.getInstance().displayMode

    override fun createComponent(project: Project, language: Language): JComponent {
        val panel = JPanel(GridLayout(ColumnHintDisplayMode.entries.size, 1))
        val buttonGroup = ButtonGroup()
        for (mode in ColumnHintDisplayMode.entries) {
            val button = JBRadioButton(TiBasicBundle.message(mode.labelKey), currentMode == mode)
            button.addItemListener { if (button.isSelected) currentMode = mode }
            buttonGroup.add(button)
            panel.add(button)
        }
        return panel
    }

    override fun isDifferentFrom(project: Project, settings: ColumnHintDisplayMode) = currentMode != settings

    override fun getSettingsCopy(): ColumnHintDisplayMode = currentMode

    override fun persistSettings(project: Project, settings: ColumnHintDisplayMode, language: Language) {
        TiBasicColumnHintSettings.getInstance().displayMode = settings
    }

    override fun putSettings(project: Project, settings: ColumnHintDisplayMode, language: Language) = Unit
}
