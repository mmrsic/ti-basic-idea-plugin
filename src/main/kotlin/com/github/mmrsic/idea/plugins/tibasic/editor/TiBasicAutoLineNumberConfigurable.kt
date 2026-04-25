package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicBundle
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class TiBasicAutoLineNumberConfigurable : SearchableConfigurable {

    private val settings = TiBasicAutoLineNumberSettings.getInstance()
    private var deltaField: JBTextField? = null
    private var roundToTensCheckBox: JBCheckBox? = null

    override fun getId(): String = "tibasic.auto.line.number"

    override fun getDisplayName(): String = TiBasicBundle.message("auto.line.number.settings.title")

    override fun createComponent(): JComponent {
        deltaField = JBTextField(settings.autoLineNumberDelta.toString(), 6)
        roundToTensCheckBox = JBCheckBox(
            TiBasicBundle.message("auto.line.number.round.to.tens"),
            settings.roundToTens,
        )
        return panel {
            row(TiBasicBundle.message("auto.line.number.delta")) {
                cell(deltaField!!)
                    .align(AlignX.FILL)
            }
            row {
                cell(roundToTensCheckBox!!)
            }
        }
    }

    override fun isModified(): Boolean =
        deltaField?.text?.trim() != settings.autoLineNumberDelta.toString() ||
            roundToTensCheckBox?.isSelected != settings.roundToTens

    @Throws(ConfigurationException::class)
    override fun apply() {
        val delta = deltaField?.text?.trim()?.toIntOrNull()
        if (delta == null || delta < 1) {
            throw ConfigurationException(TiBasicBundle.message("auto.line.number.delta.invalid"))
        }
        settings.autoLineNumberDelta = delta
        settings.roundToTens = roundToTensCheckBox?.isSelected == true
    }

    override fun reset() {
        deltaField?.text = settings.autoLineNumberDelta.toString()
        roundToTensCheckBox?.isSelected = settings.roundToTens
    }

    override fun disposeUIResources() {
        deltaField = null
        roundToTensCheckBox = null
    }
}
