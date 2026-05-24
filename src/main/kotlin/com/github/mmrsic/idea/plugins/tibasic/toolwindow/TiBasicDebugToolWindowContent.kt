package com.github.mmrsic.idea.plugins.tibasic.toolwindow

import com.github.mmrsic.idea.plugins.tibasic.debug.TiBasicDebugMetadata
import com.github.mmrsic.idea.plugins.tibasic.debug.TiBasicDebugSession
import com.github.mmrsic.idea.plugins.tibasic.debug.TiBasicDebugSessionService
import com.github.mmrsic.idea.plugins.tibasic.debug.TiBasicDebugSessionStatus
import com.github.mmrsic.idea.plugins.tibasic.debug.inspectExpression
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Component
import java.awt.Font
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JToolBar
import javax.swing.border.TitledBorder

class TiBasicDebugToolWindowContent(
    private val project: Project,
) : JPanel(BorderLayout()), Disposable {

    internal val fileLabel = JLabel(" ")
    internal val statusLabel = JLabel(" ")
    internal val messageLabel = JLabel(" ")
    internal val stepButton = JButton(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowStepKey))
    internal val stopButton = JButton(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowStopKey))
    internal val inspectField = JBTextField()
    internal val inspectButton = JButton(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowInspectActionKey))
    internal val inspectResultLabel = JLabel(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowInspectEmptyKey))
    internal val keyboardPanel = JPanel(BorderLayout())
    internal val keyboardModeLabel = JLabel(" ")
    internal val keyboardInputField = JBTextField()
    internal val listModel = DefaultListModel<String>()
    internal val listing = JBList(listModel)
    internal val numericVariablesModel = DefaultListModel<String>()
    internal val numericVariablesList = JBList(numericVariablesModel)
    internal val stringVariablesModel = DefaultListModel<String>()
    internal val stringVariablesList = JBList(stringVariablesModel)

    private val layout = CardLayout()
    private val centerPanel = JPanel(layout)
    private val emptyLabel = JLabel(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowEmptyKey))
    private val sessionService = project.getService(TiBasicDebugSessionService::class.java)
    private var currentSourceLineIndex: Int? = null

    init {
        stepButton.addActionListener {
            sessionService.updateKeyboardScanInput(keyboardInputField.text)
            sessionService.step()
        }
        stopButton.addActionListener { sessionService.stop() }
        inspectButton.addActionListener { updateInspectResult(sessionService.currentSession()) }
        inspectField.addActionListener { updateInspectResult(sessionService.currentSession()) }
        keyboardInputField.addActionListener {
            sessionService.updateKeyboardScanInput(keyboardInputField.text)
            sessionService.step()
        }
        listing.font = Font(Font.MONOSPACED, Font.PLAIN, listing.font.size)
        numericVariablesList.font = Font(Font.MONOSPACED, Font.PLAIN, numericVariablesList.font.size)
        stringVariablesList.font = Font(Font.MONOSPACED, Font.PLAIN, stringVariablesList.font.size)
        listing.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean,
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, false, false) as JLabel
                val isProgramCounter = index == currentSourceLineIndex
                component.text = if (isProgramCounter) "$PROGRAM_COUNTER_PREFIX$value" else "$NO_PROGRAM_COUNTER_PREFIX$value"
                component.isOpaque = true
                component.background = if (isProgramCounter) list?.selectionBackground else list?.background
                component.foreground = if (isProgramCounter) list?.selectionForeground else list?.foreground
                return component
            }
        }
        add(createToolbar(), BorderLayout.NORTH)
        centerPanel.add(emptyLabel, EMPTY_CARD)
        centerPanel.add(
            JPanel(BorderLayout()).also { panel ->
                panel.add(createInteractionPanel(), BorderLayout.NORTH)
                panel.add(
                    JSplitPane(JSplitPane.VERTICAL_SPLIT, JBScrollPane(listing), createVariablesPanel()).also { splitPane ->
                        splitPane.resizeWeight = LISTING_PANEL_WEIGHT
                    },
                    BorderLayout.CENTER,
                )
            },
            LIST_CARD,
        )
        add(centerPanel, BorderLayout.CENTER)
        add(messageLabel, BorderLayout.SOUTH)
        sessionService.addListener(
            { _, session -> render(session) },
            this,
        )
        render(sessionService.currentSession())
    }

    override fun dispose() = Unit

    private fun createToolbar(): JComponent =
        JToolBar().also { toolbar ->
            toolbar.isFloatable = false
            toolbar.add(fileLabel)
            toolbar.addSeparator()
            toolbar.add(statusLabel)
            toolbar.addSeparator()
            toolbar.add(stepButton)
            toolbar.add(stopButton)
        }

    private fun render(session: TiBasicDebugSession?) {
        if (session == null) {
            fileLabel.text = " "
            statusLabel.text = " "
            messageLabel.text = " "
            stepButton.isEnabled = false
            stopButton.isEnabled = false
            inspectButton.isEnabled = false
            currentSourceLineIndex = null
            listModel.clear()
            numericVariablesModel.clear()
            numericVariablesModel.addElement(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowNoNumbersKey))
            stringVariablesModel.clear()
            stringVariablesModel.addElement(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowNoStringsKey))
            keyboardPanel.isVisible = false
            keyboardModeLabel.text = " "
            keyboardInputField.text = ""
            inspectResultLabel.text = TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowInspectEmptyKey)
            layout.show(centerPanel, EMPTY_CARD)
            return
        }
        fileLabel.text = " ${session.snapshot.fileName}"
        statusLabel.text = " ${TiBasicDebugMetadata.message(session.status.bundleKey)}"
        messageLabel.text = session.statusMessage ?: " "
        currentSourceLineIndex = session.currentSourceLineIndex
        stepButton.isEnabled = session.status != TiBasicDebugSessionStatus.Stopped
        stopButton.isEnabled = session.status != TiBasicDebugSessionStatus.Stopped
        inspectButton.isEnabled = true
        if (listModel.size != session.snapshot.sourceLines.size || session.snapshot.sourceLines.indices.any { listModel.get(it) != session.snapshot.sourceLines[it] }) {
            listModel.clear()
            session.snapshot.sourceLines.forEach(listModel::addElement)
        }
        updateNumericVariables(session)
        updateStringVariables(session)
        updateKeyboardRequest(session)
        updateInspectResult(session)
        layout.show(centerPanel, LIST_CARD)
        currentSourceLineIndex?.let { sourceLineIndex ->
            listing.selectedIndex = sourceLineIndex
            listing.ensureIndexIsVisible(sourceLineIndex)
        } ?: listing.clearSelection()
        listing.repaint()
    }

    private fun createVariablesPanel(): JComponent =
        JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createNumericVariablesPanel(), createStringVariablesPanel()).also { splitPane ->
            splitPane.resizeWeight = VARIABLES_PANEL_WEIGHT
        }

    private fun createInteractionPanel(): JComponent =
        JPanel(BorderLayout()).also { panel ->
            panel.add(createInspectPanel(), BorderLayout.NORTH)
            panel.add(createKeyboardPanel(), BorderLayout.SOUTH)
        }

    private fun createNumericVariablesPanel(): JComponent =
        JPanel(BorderLayout()).also { panel ->
            panel.border = TitledBorder(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowNumbersTitleKey))
            panel.add(JBScrollPane(numericVariablesList), BorderLayout.CENTER)
        }

    private fun createStringVariablesPanel(): JComponent =
        JPanel(BorderLayout()).also { panel ->
            panel.border = TitledBorder(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowStringsTitleKey))
            panel.add(JBScrollPane(stringVariablesList), BorderLayout.CENTER)
        }

    private fun createInspectPanel(): JComponent =
        JPanel(BorderLayout()).also { panel ->
            panel.border = TitledBorder(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowInspectTitleKey))
            panel.add(
                JPanel(BorderLayout()).also { inputPanel ->
                    inputPanel.add(inspectField, BorderLayout.CENTER)
                    inputPanel.add(inspectButton, BorderLayout.EAST)
                },
                BorderLayout.NORTH,
            )
            inspectResultLabel.font = Font(Font.MONOSPACED, Font.PLAIN, inspectResultLabel.font.size)
            panel.add(inspectResultLabel, BorderLayout.SOUTH)
        }

    private fun createKeyboardPanel(): JComponent =
        keyboardPanel.also { panel ->
            panel.border = TitledBorder(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowKeyboardTitleKey))
            panel.add(keyboardModeLabel, BorderLayout.NORTH)
            panel.add(
                JPanel(BorderLayout()).also { inputPanel ->
                    inputPanel.add(JLabel(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowKeyboardInputKey)), BorderLayout.WEST)
                    inputPanel.add(keyboardInputField, BorderLayout.CENTER)
                },
                BorderLayout.CENTER,
            )
            panel.isVisible = false
        }

    private fun updateStringVariables(session: TiBasicDebugSession) {
        val entries = session.stringVariables.entries
            .sortedBy(Map.Entry<String, *>::key)
            .map { (name, value) -> "$name = ${value.internalDisplay}" }
            .ifEmpty { listOf(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowNoStringsKey)) }
        if (stringVariablesModel.size != entries.size || entries.indices.any { stringVariablesModel.get(it) != entries[it] }) {
            stringVariablesModel.clear()
            entries.forEach(stringVariablesModel::addElement)
        }
    }

    private fun updateNumericVariables(session: TiBasicDebugSession) {
        val entries = session.numericVariables.entries
            .sortedBy(Map.Entry<String, *>::key)
            .map { (name, value) -> "$name = ${value.internalDisplay} | ${value.usualDisplay}" }
            .ifEmpty { listOf(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowNoNumbersKey)) }
        if (numericVariablesModel.size != entries.size || entries.indices.any { numericVariablesModel.get(it) != entries[it] }) {
            numericVariablesModel.clear()
            entries.forEach(numericVariablesModel::addElement)
        }
    }

    private fun updateKeyboardRequest(session: TiBasicDebugSession) {
        val request = session.keyboardRequest
        keyboardPanel.isVisible = request != null
        keyboardModeLabel.text = request?.let { keyboardRequest ->
            TiBasicDebugMetadata.message(
                TiBasicDebugMetadata.toolWindowKeyboardModeKey,
                keyboardRequest.mode,
                keyboardRequest.allowedCodesDisplay,
            )
        } ?: " "
        val requestedInput = request?.scanInput ?: ""
        if (keyboardInputField.text != requestedInput) {
            keyboardInputField.text = requestedInput
        }
    }

    private fun updateInspectResult(session: TiBasicDebugSession?) {
        val expressionText = inspectField.text.trim()
        inspectResultLabel.text = when {
            expressionText.isEmpty() -> TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowInspectEmptyKey)
            session == null -> TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowInspectEmptyKey)
            else -> inspectExpression(project, session, expressionText)?.displayText
                ?: TiBasicDebugMetadata.message(TiBasicDebugMetadata.incorrectStatementKey)
        }
    }
}

private const val EMPTY_CARD = "empty"
private const val LIST_CARD = "list"
private const val PROGRAM_COUNTER_PREFIX = "▶ "
private const val NO_PROGRAM_COUNTER_PREFIX = "  "
private const val LISTING_PANEL_WEIGHT = 0.75
private const val VARIABLES_PANEL_WEIGHT = 0.5
