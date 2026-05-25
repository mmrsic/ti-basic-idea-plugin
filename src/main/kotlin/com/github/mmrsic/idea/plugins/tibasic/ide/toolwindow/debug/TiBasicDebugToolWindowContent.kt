package com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.debug

import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugMetadata
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugScreenContents
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugSession
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugSessionService
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugSessionStatus
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.inspectExpression
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Font
import java.awt.GridLayout
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
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
    internal val listModel = DefaultListModel<TiBasicDebugListingRow>()
    internal val listing = JBList(listModel)
    internal val screenComponent = TiBasicDebugScreenComponent()
    internal val characterSetPreviewComponent = TiBasicDebugCharacterSetPreviewComponent(
        TiBasicDebugCharacterSetPreviewState.fromScreenContents(TiBasicDebugScreenContents()),
    )
    internal val keepAspectRatioCheckBox = JCheckBox(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowScreenKeepRatioKey), true)
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
        listing.cellRenderer = TiBasicDebugListingRenderer { currentSourceLineIndex }
        keepAspectRatioCheckBox.addActionListener {
            screenComponent.keepAspectRatio = keepAspectRatioCheckBox.isSelected
            characterSetPreviewComponent.keepAspectRatio = keepAspectRatioCheckBox.isSelected
        }
        add(createToolbar(), BorderLayout.NORTH)
        centerPanel.add(emptyLabel, EMPTY_CARD)
        centerPanel.add(
            JPanel(BorderLayout()).also { panel ->
                panel.add(createInteractionPanel(), BorderLayout.NORTH)
                panel.add(createMainContentPanel(), BorderLayout.CENTER)
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
        screenComponent.state = screenComponent.state.copy(
            screenBackground = session.screenContents.screenBackground,
            characterSetColors = session.screenContents.characterSetColors,
            characterCodes = session.screenContents.characterCodes,
            characterPatterns = session.screenContents.characterPatterns,
        )
        characterSetPreviewComponent.state = TiBasicDebugCharacterSetPreviewState.fromScreenContents(session.screenContents)
        stepButton.isEnabled = session.status != TiBasicDebugSessionStatus.Stopped
        stopButton.isEnabled = session.status != TiBasicDebugSessionStatus.Stopped
        inspectButton.isEnabled = true
        val listingRows = buildDebugListingRows(session.snapshot.sourceLines)
        if (listModel.size != listingRows.size || listingRows.indices.any { listModel.get(it) != listingRows[it] }) {
            listModel.clear()
            listingRows.forEach(listModel::addElement)
        }
        updateNumericVariables(session)
        updateStringVariables(session)
        updateKeyboardRequest(session)
        updateInspectResult(session)
        layout.show(centerPanel, LIST_CARD)
        currentSourceLineIndex?.let { sourceLineIndex ->
            (0 until listModel.size)
                .firstOrNull { index -> listModel.get(index).sourceLineIndex == sourceLineIndex }
                ?.let { listingIndex ->
                    listing.selectedIndex = listingIndex
                    listing.ensureIndexIsVisible(listingIndex)
                }
                ?: listing.clearSelection()
        } ?: listing.clearSelection()
        listing.repaint()
        screenComponent.repaint()
        characterSetPreviewComponent.repaint()
    }

    private fun createVariablesPanel(): JComponent =
        JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createNumericVariablesPanel(), createStringVariablesPanel()).also { splitPane ->
            splitPane.resizeWeight = VARIABLES_PANEL_WEIGHT
        }

    private fun createMainContentPanel(): JComponent =
        JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createListingPanel(), createScreenAndVariablesPanel()).also { splitPane ->
            splitPane.resizeWeight = MAIN_CONTENT_PANEL_WEIGHT
        }

    private fun createListingPanel(): JComponent =
        JPanel(BorderLayout()).also { panel ->
            panel.border = TitledBorder(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowProgramTitleKey))
            panel.add(JBScrollPane(listing), BorderLayout.CENTER)
        }

    private fun createScreenAndVariablesPanel(): JComponent =
        JSplitPane(JSplitPane.VERTICAL_SPLIT, createScreenPanel(), createVariablesPanel()).also { splitPane ->
            splitPane.resizeWeight = SCREEN_PANEL_WEIGHT
        }

    private fun createScreenPanel(): JComponent =
        JPanel(BorderLayout()).also { panel ->
            panel.border = TitledBorder(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowScreenTitleKey))
            panel.add(
                JPanel(BorderLayout()).also { optionsPanel ->
                    optionsPanel.add(keepAspectRatioCheckBox, BorderLayout.WEST)
                },
                BorderLayout.NORTH,
            )
            panel.add(
                JPanel(GridLayout(1, 2, SCREEN_CONTENT_GAP, 0)).also { contentPanel ->
                    contentPanel.add(screenComponent)
                    contentPanel.add(characterSetPreviewComponent)
                },
                BorderLayout.CENTER,
            )
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
private const val MAIN_CONTENT_PANEL_WEIGHT = 0.55
private const val SCREEN_CONTENT_GAP = 8
private const val SCREEN_PANEL_WEIGHT = 0.42
private const val VARIABLES_PANEL_WEIGHT = 0.5
