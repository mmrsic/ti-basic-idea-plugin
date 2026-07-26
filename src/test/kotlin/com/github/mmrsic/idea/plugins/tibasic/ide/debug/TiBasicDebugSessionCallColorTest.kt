package com.github.mmrsic.idea.plugins.tibasic.ide.debug

import com.github.mmrsic.idea.plugins.tibasic.language.model.TiColor

internal class TiBasicDebugSessionCallColorTest : TiBasicDebugSessionBaseTest() {

    fun `test CALL COLOR updates rounded character set colors`() {
        var session = startSession(
            """
            100 CALL COLOR(5.4,3.2,1.2)
            110 PRINT "A"
            """.trimIndent(),
        )

        session = session.step()

        assertEquals(TiColor.MediumGreen, session.screenContents.characterSetColors[5]?.fg)
        assertEquals(TiColor.Transparent, session.screenContents.characterSetColors[5]?.bg)
        assertEquals(110, session.currentProgramLine?.lineNumber)
    }

    fun `test CALL COLOR with invalid rounded character set shows bad value`() {
        var session = startSession("100 CALL COLOR(16.6,2,1)")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.badValueKey, "character set=17"), session.statusMessage)
    }

    fun `test CALL COLOR with string argument shows string number mismatch`() {
        var session = startSession("100 CALL COLOR(\"A\",2,1)")

        session = session.step()

        assertEquals(TiBasicDebugSessionStatus.PendingStop, session.status)
        assertEquals(TiBasicDebugMetadata.message(TiBasicDebugMetadata.stringNumberMismatchKey), session.statusMessage)
    }
}