package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key

private const val DISPLAY_COLUMN_GUIDE_HIGHLIGHTER_LAYER = HighlighterLayer.ADDITIONAL_SYNTAX + 1
private val DISPLAY_COLUMN_GUIDE_CONTROLLER_KEY =
    Key.create<TiBasicDisplayColumnGuideController>("tibasic.displayColumnGuide.controller")

internal class TiBasicDisplayColumnGuideController private constructor(private val editor: Editor) : Disposable {

    private val renderer = TiBasicDisplayColumnGuideRenderer()
    private var highlighter: RangeHighlighter? = null
    private val documentListener = object : DocumentListener {
        override fun documentChanged(event: DocumentEvent) {
            refresh()
        }
    }

    init {
        editor.document.addDocumentListener(documentListener, this)
        refresh()
    }

    fun refresh() {
        renderer.guideColumns = guideColumns()
        if (renderer.guideColumns.isEmpty()) {
            removeHighlighter()
        } else {
            ensureHighlighter()
        }
        editor.contentComponent.repaint()
    }

    override fun dispose() {
        removeHighlighter()
        editor.putUserData(DISPLAY_COLUMN_GUIDE_CONTROLLER_KEY, null)
    }

    private fun ensureHighlighter() {
        val documentLength = editor.document.textLength
        val existingHighlighter = highlighter
        if (existingHighlighter != null && existingHighlighter.isValid && existingHighlighter.startOffset == 0 && existingHighlighter.endOffset == documentLength) {
            return
        }
        removeHighlighter()
        highlighter = editor.markupModel.addRangeHighlighter(
            0,
            documentLength,
            DISPLAY_COLUMN_GUIDE_HIGHLIGHTER_LAYER,
            null,
            HighlighterTargetArea.EXACT_RANGE,
        ).also {
            it.setCustomRenderer(renderer)
            it.isGreedyToLeft = true
            it.isGreedyToRight = true
        }
    }

    private fun removeHighlighter() {
        highlighter?.let(editor.markupModel::removeHighlighter)
        highlighter = null
    }

    private fun guideColumns(): List<Int> {
        val settings = TiBasicColumnHintSettings.getInstance()
        if (!settings.guidesEnabled) {
            return emptyList()
        }
        return displayColumnGuideColumns(
            longestLineLength = longestLineLength(editor.document),
            columnWidth = TI99_4A_DISPLAY_COLUMNS,
            previewDistance = settings.guidePreviewDistance,
        )
    }

    companion object {

        fun install(editor: Editor): TiBasicDisplayColumnGuideController? {
            if (!isTiBasicEditor(editor)) {
                return null
            }
            editor.getUserData(DISPLAY_COLUMN_GUIDE_CONTROLLER_KEY)?.let { return it }
            return TiBasicDisplayColumnGuideController(editor)
                .also { editor.putUserData(DISPLAY_COLUMN_GUIDE_CONTROLLER_KEY, it) }
        }

        fun uninstall(editor: Editor) {
            editor.getUserData(DISPLAY_COLUMN_GUIDE_CONTROLLER_KEY)?.let(Disposer::dispose)
        }

        fun refreshAllEditors() {
            EditorFactory.getInstance().allEditors.forEach { editor ->
                if (isTiBasicEditor(editor)) {
                    install(editor)?.refresh()
                } else {
                    uninstall(editor)
                }
            }
        }
    }
}
