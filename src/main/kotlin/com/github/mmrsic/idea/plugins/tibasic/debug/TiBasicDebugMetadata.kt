package com.github.mmrsic.idea.plugins.tibasic.debug

import com.github.mmrsic.idea.plugins.tibasic.lang.TiBasicBundle

internal object TiBasicDebugMetadata {
    const val toolWindowId = "TI Basic Debug"
    const val configurationTypeId = "TiBasicDebugConfiguration"
    const val configurationFactoryId = "TiBasicDebugConfigurationFactory"
    const val programRunnerId = "TiBasicDebugProgramRunner"

    const val configurationTypeDisplayNameKey = "debug.configuration.type.display.name"
    const val configurationTypeDescriptionKey = "debug.configuration.type.description"
    const val configurationEditorFileLabelKey = "debug.configuration.editor.file"
    const val toolWindowEmptyKey = "debug.tool.window.empty"
    const val toolWindowStepKey = "debug.tool.window.step"
    const val toolWindowStopKey = "debug.tool.window.stop"
    const val toolWindowInspectTitleKey = "debug.tool.window.inspect.title"
    const val toolWindowInspectActionKey = "debug.tool.window.inspect.action"
    const val toolWindowInspectEmptyKey = "debug.tool.window.inspect.empty"
    const val toolWindowKeyboardTitleKey = "debug.tool.window.keyboard.title"
    const val toolWindowKeyboardModeKey = "debug.tool.window.keyboard.mode"
    const val toolWindowKeyboardInputKey = "debug.tool.window.keyboard.input"
    const val toolWindowNumbersTitleKey = "debug.tool.window.numbers.title"
    const val toolWindowNoNumbersKey = "debug.tool.window.numbers.empty"
    const val toolWindowStringsTitleKey = "debug.tool.window.strings.title"
    const val toolWindowNoStringsKey = "debug.tool.window.strings.empty"
    const val toolWindowStatusPausedKey = "debug.tool.window.status.paused"
    const val toolWindowStatusPendingStopKey = "debug.tool.window.status.pending.stop"
    const val toolWindowStatusStoppedKey = "debug.tool.window.status.stopped"
    const val noExecutableLineKey = "debug.runtime.no.executable.line"
    const val badLineNumberKey = "debug.runtime.bad.line.number"
    const val badValueKey = "debug.runtime.bad.value"
    const val cantDoThatKey = "debug.runtime.cant.do.that"
    const val incorrectStatementKey = "debug.runtime.incorrect.statement"
    const val stringCutTo255CharactersKey = "debug.runtime.string.cut.to.255.characters"

    fun message(key: String, vararg params: Any): String = TiBasicBundle.message(key, *params)
}
