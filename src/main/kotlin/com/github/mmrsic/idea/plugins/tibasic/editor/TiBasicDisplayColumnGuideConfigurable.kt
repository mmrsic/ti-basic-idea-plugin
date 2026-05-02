package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicBundle
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel

private const val DISPLAY_COLUMN_GUIDE_SETTINGS_TITLE_KEY = "display.columns.settings.title"
private const val DISPLAY_COLUMN_GUIDE_ENABLED_KEY = "display.columns.enabled"
private const val DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE_KEY = "display.columns.preview.distance"
private const val DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE_INVALID_KEY = "display.columns.preview.distance.invalid"

class TiBasicDisplayColumnGuideConfigurable : BoundConfigurable(TiBasicBundle.message(DISPLAY_COLUMN_GUIDE_SETTINGS_TITLE_KEY)) {

    private val settings = TiBasicColumnHintSettings.getInstance()
    private lateinit var enabledCheckBox: JBCheckBox
    private lateinit var previewDistanceField: JBTextField

    override fun createPanel() = panel {
        row {
            cell(createEnabledCheckBox())
        }
        row(TiBasicBundle.message(DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE_KEY)) {
            cell(createPreviewDistanceField())
        }
    }

    override fun isModified(): Boolean =
        enabledCheckBox.isSelected != settings.guidesEnabled ||
            previewDistanceField.text.trim() != settings.guidePreviewDistance.toString()

    @Throws(ConfigurationException::class)
    override fun apply() {
        val previewDistance = previewDistanceField.text.trim().toIntOrNull()
        if (previewDistance == null || previewDistance < MIN_DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE) {
            throw ConfigurationException(TiBasicBundle.message(DISPLAY_COLUMN_GUIDE_PREVIEW_DISTANCE_INVALID_KEY))
        }
        settings.guidesEnabled = enabledCheckBox.isSelected
        settings.guidePreviewDistance = previewDistance
        TiBasicDisplayColumnGuideController.refreshAllEditors()
    }

    override fun reset() {
        enabledCheckBox.isSelected = settings.guidesEnabled
        previewDistanceField.text = settings.guidePreviewDistance.toString()
    }

    private fun createEnabledCheckBox(): JBCheckBox =
        JBCheckBox(TiBasicBundle.message(DISPLAY_COLUMN_GUIDE_ENABLED_KEY), settings.guidesEnabled)
            .also { enabledCheckBox = it }

    private fun createPreviewDistanceField(): JBTextField =
        JBTextField(settings.guidePreviewDistance.toString(), 6)
            .also { previewDistanceField = it }
}
