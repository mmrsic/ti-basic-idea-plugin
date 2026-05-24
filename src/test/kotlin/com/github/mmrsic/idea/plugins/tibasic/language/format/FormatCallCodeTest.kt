package com.github.mmrsic.idea.plugins.tibasic.language.format

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase

class FormatCallCodeTest : TiBasicTestBase() {

    fun `test CALL CLEAR is formatted`() {
        val file = configureFile("100 CALL CLEAR")
        assertEquals("100 CALL CLEAR", formattedText(file))
    }

    fun `test lowercase call clear is uppercased`() {
        val file = configureFile("100 call clear")
        assertEquals("100 CALL CLEAR", formattedText(file))
    }

    fun `test CALL SCREEN with argument is formatted`() {
        val file = configureFile("100 CALL SCREEN( 2 )")
        assertEquals("100 CALL SCREEN(2)", formattedText(file))
    }

    fun `test CALL HCHAR with arguments removes spaces`() {
        val file = configureFile("100 CALL HCHAR( 1 , 2 , 42 )")
        assertEquals("100 CALL HCHAR(1,2,42)", formattedText(file))
    }

    fun `test CALL HCHAR with empty parentheses preserves parentheses`() {
        val file = configureFile("1110 call HCHAR()")
        assertEquals("1110 CALL HCHAR()", formattedText(file))
    }

    fun `test CALL CHAR with string argument preserves string`() {
        val file = configureFile("100 CALL CHAR(96,\"FFFFFFFFFFFFFFFF\")")
        assertEquals("100 CALL CHAR(96,\"FFFFFFFFFFFFFFFF\")", formattedText(file))
    }

    fun `test CALL SOUND is formatted`() {
        val file = configureFile("100 CALL SOUND(100,440,2)")
        assertEquals("100 CALL SOUND(100,440,2)", formattedText(file))
    }

    fun `test lowercase call arguments uppercased outside strings`() {
        val file = configureFile("100 call hchar(r,c,42)")
        assertEquals("100 CALL HCHAR(R,C,42)", formattedText(file))
    }
}
