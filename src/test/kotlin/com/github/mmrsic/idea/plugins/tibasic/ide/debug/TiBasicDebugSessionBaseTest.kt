package com.github.mmrsic.idea.plugins.tibasic.ide.debug

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

internal abstract class TiBasicDebugSessionBaseTest : TiBasicTestBase() {

    protected fun startSession(programText: String): TiBasicDebugSession {
        val file = configureFile(programText)
        val snapshot = TiBasicDebugProgramSnapshot.create(file, myFixture.editor.document)
        return snapshot.initialSession()
    }


    protected fun screenText(session: TiBasicDebugSession, row: Int, column: Int, length: Int): String =
        (column - 1 until column - 1 + length)
            .map { index -> session.screenContents.characterCodes[row - 1][index].toChar() }
            .joinToString(separator = "")
}
