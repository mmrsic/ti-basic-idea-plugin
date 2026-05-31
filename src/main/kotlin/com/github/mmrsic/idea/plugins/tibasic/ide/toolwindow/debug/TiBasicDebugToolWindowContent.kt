package com.github.mmrsic.idea.plugins.tibasic.ide.toolwindow.debug

import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugMetadata
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugLineSemantics
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugJoystickPosition
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugJoystickRequest
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugScreenContents
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugSession
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugSessionService
import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugSessionStatus
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.Font
import java.awt.GridLayout
import java.awt.Point
import javax.swing.DefaultListModel
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTextArea
import javax.swing.JToolBar
import javax.swing.JToggleButton
import javax.swing.border.TitledBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class TiBasicDebugToolWindowContent(
    private val project: Project,
) : JPanel(BorderLayout()), Disposable {

    internal val fileLabel = JLabel(" ")
    internal val statusLabel = JLabel(" ")
    internal val messageLabel = JLabel(" ")
    internal val stepButton = JButton(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowStepKey))
    internal val skipButton = JButton(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowSkipKey))
    internal val stopButton = JButton(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowStopKey))
    internal val argumentsPanel = JPanel(BorderLayout())
    internal val argumentsTextArea = JTextArea()
    internal val argumentPatternPreviewComponent = TiBasicDebugArgumentPatternPreviewComponent()
    internal val keyboardPanel = JPanel(BorderLayout())
    internal val keyboardUnitLabel = JLabel(" ")
    internal val keyboardInputLabel = JLabel(" ")
    internal val keyboardInputField = JBTextField()
    internal val keyboardStatusLabel = JLabel(" ")
    internal val joystickPanel = JPanel(BorderLayout())
    internal val joystickUnitLabel = JLabel(" ")
    internal val joystickPositionLabel = JLabel(" ")
    internal val joystickXLabel = JLabel(" ")
    internal val joystickYLabel = JLabel(" ")
    internal val joystickButtonsByInput = linkedMapOf<String, JToggleButton>()
    internal val listModel = DefaultListModel<TiBasicDebugListingRow>()
    internal val listing = JBList(listModel)
    internal val listingScrollPane = JBScrollPane(listing)
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
    private val inputPanels = JPanel(GridLayout(0, 1, 0, INPUT_PANELS_GAP))
    private val emptyLabel = JLabel(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowEmptyKey))
    private val sessionService = project.getService(TiBasicDebugSessionService::class.java)
    private val joystickButtonGroup = ButtonGroup()
    private var currentSourceLineIndex: Int? = null
    private var pendingListingScrollIndex: Int? = null

    init {
        stepButton.addActionListener {
            if (keyboardPanel.isVisible) {
                sessionService.updateKeyboardScanInput(keyboardInputField.text)
            }
            sessionService.step()
        }
        skipButton.addActionListener { sessionService.skip() }
        stopButton.addActionListener { sessionService.stop() }
        keyboardInputField.addActionListener {
            sessionService.updateKeyboardScanInput(keyboardInputField.text)
            sessionService.step()
        }
        keyboardInputField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent?) = updateKeyboardPreview()
            override fun removeUpdate(event: DocumentEvent?) = updateKeyboardPreview()
            override fun changedUpdate(event: DocumentEvent?) = updateKeyboardPreview()
        })
        listing.font = Font(Font.MONOSPACED, Font.PLAIN, listing.font.size)
        numericVariablesList.font = Font(Font.MONOSPACED, Font.PLAIN, numericVariablesList.font.size)
        stringVariablesList.font = Font(Font.MONOSPACED, Font.PLAIN, stringVariablesList.font.size)
        listing.cellRenderer = TiBasicDebugListingRenderer { currentSourceLineIndex }
        listingScrollPane.viewport.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent?) {
                pendingListingScrollIndex?.let { listingIndex ->
                    pendingListingScrollIndex = null
                    scrollListingTo(listingIndex)
                }
            }
        })
        keepAspectRatioCheckBox.addActionListener {
            screenComponent.keepAspectRatio = keepAspectRatioCheckBox.isSelected
            characterSetPreviewComponent.keepAspectRatio = keepAspectRatioCheckBox.isSelected
        }
        add(createToolbar(), BorderLayout.NORTH)
        centerPanel.add(emptyLabel, EMPTY_CARD)
        centerPanel.add(
            JPanel(BorderLayout()).also { panel ->
                panel.add(createMainContentPanel(), BorderLayout.CENTER)
            },
            LIST_CARD,
        )
        add(centerPanel, BorderLayout.CENTER)
        add(createFooterPanel(), BorderLayout.SOUTH)
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
            toolbar.add(skipButton)
            toolbar.add(stopButton)
        }

    private fun render(session: TiBasicDebugSession?) {
        if (session == null) {
            fileLabel.text = " "
            statusLabel.text = " "
            messageLabel.text = " "
            stepButton.isEnabled = false
            skipButton.isEnabled = false
            stopButton.isEnabled = false
            currentSourceLineIndex = null
            listModel.clear()
            numericVariablesModel.clear()
            numericVariablesModel.addElement(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowNoNumbersKey))
            stringVariablesModel.clear()
            stringVariablesModel.addElement(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowNoStringsKey))
            keyboardPanel.isVisible = false
            keyboardUnitLabel.text = " "
            keyboardInputLabel.text = " "
            keyboardInputField.text = ""
            keyboardStatusLabel.text = " "
            joystickPanel.isVisible = false
            joystickUnitLabel.text = " "
            joystickPositionLabel.text = " "
            joystickXLabel.text = " "
            joystickYLabel.text = " "
            joystickButtonGroup.clearSelection()
            argumentsPanel.isVisible = false
            argumentsTextArea.text = ""
            argumentPatternPreviewComponent.hexPattern = null
            refreshInputPanels()
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
        skipButton.isEnabled = session.status == TiBasicDebugSessionStatus.Paused &&
                session.currentProgramLine?.semantics is TiBasicDebugLineSemantics.Next
        stopButton.isEnabled = session.status != TiBasicDebugSessionStatus.Stopped
        val listingRows = buildDebugListingRows(session.snapshot.sourceLines)
        if (listModel.size != listingRows.size || listingRows.indices.any { listModel.get(it) != listingRows[it] }) {
            listModel.clear()
            listingRows.forEach(listModel::addElement)
        }
        updateNumericVariables(session)
        updateStringVariables(session)
        updateKeyboardRequest(session)
        updateJoystickRequest(session)
        refreshInputPanels()
        updateArguments(session)
        layout.show(centerPanel, LIST_CARD)
        currentSourceLineIndex?.let { sourceLineIndex ->
            (0 until listModel.size)
                .firstOrNull { index -> listModel.get(index).sourceLineIndex == sourceLineIndex }
                ?.let { listingIndex ->
                    listing.selectedIndex = listingIndex
                    scrollListingTo(listingIndex)
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
            panel.add(listingScrollPane, BorderLayout.CENTER)
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

    private fun createFooterPanel(): JComponent =
        JPanel(BorderLayout()).also { panel ->
            panel.add(createArgumentsPanel(), BorderLayout.NORTH)
            createKeyboardPanel()
            createJoystickPanel()
            refreshInputPanels()
            panel.add(inputPanels, BorderLayout.CENTER)
            panel.add(messageLabel, BorderLayout.SOUTH)
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

    private fun createArgumentsPanel(): JComponent =
        argumentsPanel.also { panel ->
            panel.border = TitledBorder(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowArgumentsTitleKey))
            argumentsTextArea.font = Font(Font.MONOSPACED, Font.PLAIN, argumentsTextArea.font.size)
            argumentsTextArea.isEditable = false
            argumentsTextArea.isFocusable = false
            argumentsTextArea.isOpaque = false
            argumentsTextArea.lineWrap = false
            argumentsTextArea.wrapStyleWord = false
            argumentsTextArea.border = null
            panel.add(
                JPanel(BorderLayout()).also { contentPanel ->
                    contentPanel.add(argumentsTextArea, BorderLayout.NORTH)
                    contentPanel.add(argumentPatternPreviewComponent, BorderLayout.CENTER)
                },
                BorderLayout.CENTER,
            )
            panel.isVisible = false
        }

    private fun createKeyboardPanel(): JComponent =
        keyboardPanel.also { panel ->
            panel.border = TitledBorder(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowKeyboardTitleKey))
            panel.add(keyboardUnitLabel, BorderLayout.NORTH)
            panel.add(
                JPanel(GridLayout(KEYBOARD_PANEL_ROW_COUNT, 1, 0, KEYBOARD_PANEL_ROW_GAP)).also { inputPanel ->
                    inputPanel.add(
                        JPanel(BorderLayout()).also { returnVariablePanel ->
                            returnVariablePanel.add(keyboardInputLabel, BorderLayout.WEST)
                            returnVariablePanel.add(keyboardInputField, BorderLayout.CENTER)
                        },
                    )
                    inputPanel.add(keyboardStatusLabel)
                },
                BorderLayout.CENTER,
            )
            panel.isVisible = false
        }

    private fun createJoystickPanel(): JComponent =
        joystickPanel.also { panel ->
            panel.border = TitledBorder(TiBasicDebugMetadata.message(TiBasicDebugMetadata.toolWindowJoystickTitleKey))
            panel.add(joystickUnitLabel, BorderLayout.NORTH)
            panel.add(
                JPanel(GridLayout(JOYSTICK_GRID_ROWS, JOYSTICK_GRID_COLUMNS, JOYSTICK_GRID_GAP, JOYSTICK_GRID_GAP)).also { grid ->
                    TiBasicDebugToolWindowJoystickPositions.forEach { position ->
                        val button = JToggleButton(position.gridLabel)
                        button.addActionListener {
                            sessionService.updateKeyboardScanInput(position.input)
                        }
                        joystickButtonGroup.add(button)
                        joystickButtonsByInput[position.input] = button
                        grid.add(button)
                    }
                },
                BorderLayout.CENTER,
            )
            panel.add(
                JPanel(GridLayout(JOYSTICK_INFO_ROW_COUNT, 1, 0, JOYSTICK_INFO_ROW_GAP)).also { infoPanel ->
                    infoPanel.add(joystickPositionLabel)
                    infoPanel.add(joystickXLabel)
                    infoPanel.add(joystickYLabel)
                },
                BorderLayout.SOUTH,
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
        keyboardUnitLabel.text = request?.let { keyboardRequest ->
            TiBasicDebugMetadata.message(
                TiBasicDebugMetadata.toolWindowKeyboardUnitKey,
                keyboardRequest.keyUnit,
                keyboardRequest.allowedCodesDisplay,
            )
        } ?: " "
        keyboardInputLabel.text = request?.let { keyboardRequest ->
            TiBasicDebugMetadata.message(
                TiBasicDebugMetadata.toolWindowKeyboardReturnVariableKey,
                keyboardRequest.keyCodeVariableName,
            )
        } ?: " "
        keyboardStatusLabel.text = request?.let { keyboardRequest ->
            TiBasicDebugMetadata.message(
                TiBasicDebugMetadata.toolWindowKeyboardStatusVariableKey,
                keyboardRequest.statusVariableName,
                keyboardRequest.statusValueDisplay,
            )
        } ?: " "
        val requestedInput = request?.scanInput ?: ""
        if (keyboardInputField.text != requestedInput) {
            keyboardInputField.text = requestedInput
        }
    }

    private fun updateJoystickRequest(session: TiBasicDebugSession) {
        val request = session.joystickRequest
        joystickPanel.isVisible = request != null
        joystickUnitLabel.text = request?.let { joystickRequest ->
            TiBasicDebugMetadata.message(
                TiBasicDebugMetadata.toolWindowJoystickUnitKey,
                joystickRequest.keyUnit,
            )
        } ?: " "
        joystickPositionLabel.text = request?.let { joystickRequest ->
            TiBasicDebugMetadata.message(
                TiBasicDebugMetadata.toolWindowJoystickPositionKey,
                joystickRequest.position.compactDisplay,
                joystickRequest.position.x,
                joystickRequest.position.y,
            )
        } ?: " "
        joystickXLabel.text = request?.let { joystickRequest ->
            TiBasicDebugMetadata.message(
                TiBasicDebugMetadata.toolWindowJoystickXVariableKey,
                joystickRequest.xVariableName,
                joystickRequest.position.x,
            )
        } ?: " "
        joystickYLabel.text = request?.let { joystickRequest ->
            TiBasicDebugMetadata.message(
                TiBasicDebugMetadata.toolWindowJoystickYVariableKey,
                joystickRequest.yVariableName,
                joystickRequest.position.y,
            )
        } ?: " "
        val selectedButton = request?.input?.let(joystickButtonsByInput::get)
        joystickButtonsByInput.values.forEach { button -> button.isSelected = button == selectedButton }
    }

    private fun refreshInputPanels() {
        inputPanels.removeAll()
        if (keyboardPanel.isVisible) {
            inputPanels.add(keyboardPanel)
        }
        if (joystickPanel.isVisible) {
            inputPanels.add(joystickPanel)
        }
        inputPanels.isVisible = inputPanels.componentCount > 0
        inputPanels.revalidate()
        inputPanels.repaint()
    }

    private fun updateKeyboardPreview() {
        val request = sessionService.currentSession()?.keyboardRequestForInput(keyboardInputField.text) ?: return
        keyboardStatusLabel.text = TiBasicDebugMetadata.message(
            TiBasicDebugMetadata.toolWindowKeyboardStatusVariableKey,
            request.statusVariableName,
            request.statusValueDisplay,
        )
    }

    private fun updateArguments(session: TiBasicDebugSession) {
        val argumentDisplays = session.currentArgumentDisplays
        argumentsPanel.isVisible = argumentDisplays.isNotEmpty()
        argumentsTextArea.text = argumentDisplays.joinToString(separator = "\n")
        argumentsTextArea.rows = argumentDisplays.size.coerceAtLeast(1)
        argumentPatternPreviewComponent.hexPattern = session.currentArgumentPatternPreview
        argumentsPanel.revalidate()
    }

    private fun scrollListingTo(listingIndex: Int) {
        val viewport = listingScrollPane.viewport
        val viewportHeight = viewport.extentSize.height
        if (viewportHeight <= 0) {
            pendingListingScrollIndex = listingIndex
            return
        }
        val cellBounds = listing.getCellBounds(listingIndex, listingIndex) ?: return
        val totalBounds = listing.getCellBounds(0, listModel.size - 1) ?: return
        val targetY = (cellBounds.centerY - viewportHeight / 2.0)
            .toInt()
            .coerceIn(0, (totalBounds.height - viewportHeight).coerceAtLeast(0))
        viewport.viewPosition = Point(0, targetY)
    }
}

private const val EMPTY_CARD = "empty"
private const val LIST_CARD = "list"
private const val MAIN_CONTENT_PANEL_WEIGHT = 0.55
private const val SCREEN_CONTENT_GAP = 8
private const val SCREEN_PANEL_WEIGHT = 0.42
private const val VARIABLES_PANEL_WEIGHT = 0.5
private const val KEYBOARD_PANEL_ROW_COUNT = 2
private const val KEYBOARD_PANEL_ROW_GAP = 4
private const val INPUT_PANELS_GAP = 4
private const val JOYSTICK_GRID_ROWS = 3
private const val JOYSTICK_GRID_COLUMNS = 3
private const val JOYSTICK_GRID_GAP = 4
private const val JOYSTICK_INFO_ROW_COUNT = 3
private const val JOYSTICK_INFO_ROW_GAP = 2

private val TiBasicDebugToolWindowJoystickPositions = listOf(
    TiBasicDebugJoystickPosition(x = -4, y = 4, compactDisplay = "up-left", gridLabel = "NW", input = "-4,4"),
    TiBasicDebugJoystickPosition(x = 0, y = 4, compactDisplay = "up", gridLabel = "N", input = "0,4"),
    TiBasicDebugJoystickPosition(x = 4, y = 4, compactDisplay = "up-right", gridLabel = "NE", input = "4,4"),
    TiBasicDebugJoystickPosition(x = -4, y = 0, compactDisplay = "left", gridLabel = "W", input = "-4,0"),
    TiBasicDebugJoystickPosition(x = 0, y = 0, compactDisplay = "center", gridLabel = "C", input = "0,0"),
    TiBasicDebugJoystickPosition(x = 4, y = 0, compactDisplay = "right", gridLabel = "E", input = "4,0"),
    TiBasicDebugJoystickPosition(x = -4, y = -4, compactDisplay = "down-left", gridLabel = "SW", input = "-4,-4"),
    TiBasicDebugJoystickPosition(x = 0, y = -4, compactDisplay = "down", gridLabel = "S", input = "0,-4"),
    TiBasicDebugJoystickPosition(x = 4, y = -4, compactDisplay = "down-right", gridLabel = "SE", input = "4,-4"),
)
