package com.github.mmrsic.idea.plugins.tibasic.language.values

import junit.framework.TestCase
import java.math.BigDecimal

class TiBasicRadix100Test : TestCase() {

    fun `test encode zero uses special zero encoding`() {
        val number = tiBasicRadix100Number(BigDecimal.ZERO)
        assertNotNull(number)
        val storedNumber = number!!
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 0, 0), storedNumber.bytes)
        assertEquals("0 (special zero encoding)", storedNumber.radixNotation())
        assertEquals("0", tiBasicDecimalString(storedNumber.value))
    }

    fun `test encode positive integer example from TI documentation`() {
        val number = tiBasicRadix100Number(BigDecimal("115162923"))
        assertNotNull(number)
        val storedNumber = number!!
        assertEquals(listOf(0x44, 0x01, 0x0F, 0x10, 0x1D, 0x17, 0x00, 0x00), storedNumber.bytes)
        assertEquals("1.15162923 x 100^4", storedNumber.radixNotation())
        assertEquals("115162923", tiBasicDecimalString(storedNumber.value))
    }

    fun `test encode negative integer example from TI documentation`() {
        val number = tiBasicRadix100Number(BigDecimal("-115162923"))
        assertNotNull(number)
        val storedNumber = number!!
        assertEquals(listOf(0xBB, 0xFF, 0x0F, 0x10, 0x1D, 0x17, 0x00, 0x00), storedNumber.bytes)
        assertEquals("-1.15162923 x 100^4", storedNumber.radixNotation())
        assertEquals("-115162923", tiBasicDecimalString(storedNumber.value))
    }

    fun `test decode byte example from TI documentation`() {
        val number = tiBasicRadix100Number(listOf(0x43, 0x5E, 0x38, 0x52, 0x3A, 0x00, 0x00, 0x00))
        assertNotNull(number)
        val storedNumber = number!!
        assertEquals("94.568258 x 100^3", storedNumber.radixNotation())
        assertEquals("94568258", tiBasicDecimalString(storedNumber.value))
    }

    fun `test decode negative single digit example from TI documentation`() {
        val number = tiBasicRadix100Number(listOf(0xBF, 0xFB, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
        assertNotNull(number)
        val storedNumber = number!!
        assertEquals("-5 x 100^0", storedNumber.radixNotation())
        assertEquals("-5", tiBasicDecimalString(storedNumber.value))
    }

    fun `test encode rounds to fourteen decimal digits`() {
        val number = tiBasicRadix100Number(BigDecimal("0.1234567890123456"))
        assertNotNull(number)
        assertEquals("0.12345678901235", tiBasicDecimalString(number!!.value))
    }

    fun `test parse numeric literal accepts signed scientific notation`() {
        assertEquals(0, BigDecimal("-1500").compareTo(parseTiBasicDecimalLiteral("-.15E+4")))
    }
}
