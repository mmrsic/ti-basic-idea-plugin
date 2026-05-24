package com.github.mmrsic.idea.plugins.tibasic.debug.run

import com.github.mmrsic.idea.plugins.tibasic.debug.TiBasicDebugMetadata
import com.github.mmrsic.idea.plugins.tibasic.lang.fileTypeExtensions
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.TextBrowseFolderListener
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class TiBasicDebugRunConfigurationEditor(project: Project) : SettingsEditor<TiBasicDebugRunConfiguration>() {

    private val fileField = TextFieldWithBrowseButton()
    private val panel = JPanel(BorderLayout())

    init {
        panel.add(JLabel(TiBasicDebugMetadata.message(TiBasicDebugMetadata.configurationEditorFileLabelKey)), BorderLayout.NORTH)
        panel.add(fileField, BorderLayout.CENTER)
        fileField.addActionListener(
            TextBrowseFolderListener(
                FileChooserDescriptor(true, false, false, false, false, false)
                    .withFileFilter { virtualFile -> fileTypeExtensions.any { extension -> virtualFile.name.endsWith(".$extension", ignoreCase = true) } },
                project,
            ),
        )
    }

    override fun resetEditorFrom(configuration: TiBasicDebugRunConfiguration) {
        fileField.text = configuration.filePath.orEmpty()
    }

    override fun applyEditorTo(configuration: TiBasicDebugRunConfiguration) {
        configuration.filePath = fileField.text.takeIf(String::isNotBlank)
        configuration.setGeneratedName()
    }

    override fun createEditor(): JComponent = panel
}
