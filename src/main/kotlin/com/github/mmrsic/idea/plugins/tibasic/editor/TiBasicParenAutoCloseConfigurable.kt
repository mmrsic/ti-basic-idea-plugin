package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicBundle
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel

class TiBasicParenAutoCloseConfigurable : BoundConfigurable(TiBasicBundle.message("paren.auto.close.settings.title")) {

    private val settings = TiBasicParenAutoCloseSettings.getInstance()

    override fun createPanel() = panel {
        row {
            checkBox(TiBasicBundle.message("paren.auto.close.on.shift.enter"))
                .bindSelected(settings::autoCloseOnShiftEnter)
        }
        row {
            checkBox(TiBasicBundle.message("paren.auto.close.on.enter"))
                .bindSelected(settings::autoCloseOnEnter)
        }
    }
}
