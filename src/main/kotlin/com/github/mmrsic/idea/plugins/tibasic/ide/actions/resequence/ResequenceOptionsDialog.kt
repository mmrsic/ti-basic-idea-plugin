package com.github.mmrsic.idea.plugins.tibasic.ide.actions.resequence

import com.github.mmrsic.idea.plugins.tibasic.language.format.RESEQUENCE_DEFAULT_START
import com.github.mmrsic.idea.plugins.tibasic.language.format.RESEQUENCE_DEFAULT_STEP
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent

class ResequenceOptionsDialog(project: Project) : DialogWrapper(project) {

    private val startField = JBTextField(RESEQUENCE_DEFAULT_START.toString(), 6)
    private val stepField = JBTextField(RESEQUENCE_DEFAULT_STEP.toString(), 6)

    val chosenStart: Int get() = startField.text.trim().toInt()
    val chosenStep: Int get() = stepField.text.trim().toInt()

    init {
        title = "Resequence Line Numbers"
        init()
    }

    override fun createCenterPanel(): JComponent = FormBuilder.createFormBuilder()
        .addLabeledComponent(JBLabel("Start number:"), startField)
        .addLabeledComponent(JBLabel("Step:"), stepField)
        .panel

    override fun getPreferredFocusedComponent(): JComponent = startField

    override fun doValidate(): ValidationInfo? {
        val startText = startField.text.trim()
        val startValue = startText.toIntOrNull()
        if (startValue == null || startValue < 1) {
            return ValidationInfo("Start number must be a positive integer", startField)
        }
        val stepText = stepField.text.trim()
        val stepValue = stepText.toIntOrNull()
        if (stepValue == null || stepValue < 1) {
            return ValidationInfo("Step must be a positive integer", stepField)
        }
        return null
    }
}
