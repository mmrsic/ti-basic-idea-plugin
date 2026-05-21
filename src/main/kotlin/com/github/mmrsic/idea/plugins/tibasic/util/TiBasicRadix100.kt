package com.github.mmrsic.idea.plugins.tibasic.util

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

private const val STORED_BYTE_COUNT = 8
private const val MANTISSA_BYTE_COUNT = STORED_BYTE_COUNT - 1
private const val DECIMAL_DIGITS_PER_MANTISSA_BYTE = 2
private const val STORED_DECIMAL_DIGITS = MANTISSA_BYTE_COUNT * DECIMAL_DIGITS_PER_MANTISSA_BYTE
private const val FRACTIONAL_DIGITS_IN_MANTISSA = (MANTISSA_BYTE_COUNT - 1) * DECIMAL_DIGITS_PER_MANTISSA_BYTE
private const val EXPONENT_OFFSET = 64
private const val MIN_EXPONENT = -64
private const val MAX_EXPONENT = 63
private const val HEX_RADIX = 16
private const val BYTE_MASK = 0xFF
private const val WORD_MASK = 0xFFFF
private const val SIGN_BIT_MASK = 0x80
private val ZERO_BYTES = List(STORED_BYTE_COUNT) { 0 }
private val MANTISSA_OVERFLOW_THRESHOLD = BigDecimal.TEN.pow(STORED_DECIMAL_DIGITS)

data class TiBasicRadix100Number(
    val bytes: List<Int>,
    val exponent: Int?,
    val mantissaBytes: List<Int>,
    val value: BigDecimal,
    val isNegative: Boolean,
) {
    init {
        require(bytes.size == STORED_BYTE_COUNT)
        require(bytes.all { it in 0..BYTE_MASK })
        require(mantissaBytes.size == MANTISSA_BYTE_COUNT)
        require(mantissaBytes.all { it in 0..99 })
    }

    val isZero: Boolean
        get() = bytes[0] == 0 && bytes[1] == 0

    fun radixNotation(): String =
        if (isZero) {
            "0 (special zero encoding)"
        } else {
            val mantissa = mantissaBytes
                .joinToString(separator = "") { it.toString().padStart(DECIMAL_DIGITS_PER_MANTISSA_BYTE, '0') }
            val integerPart = mantissa
                .take(DECIMAL_DIGITS_PER_MANTISSA_BYTE)
                .trimStart('0')
                .ifEmpty { "0" }
            val fractionalPart = mantissa
                .drop(DECIMAL_DIGITS_PER_MANTISSA_BYTE)
                .trimEnd('0')
            buildString {
                if (isNegative) append('-')
                append(integerPart)
                if (fractionalPart.isNotEmpty()) {
                    append('.')
                    append(fractionalPart)
                }
                append(" x 100^")
                append(exponent)
            }
        }

    fun hexBytes(): String =
        bytes.joinToString(" ") { byte ->
            ">${byte.toString(HEX_RADIX).uppercase().padStart(DECIMAL_DIGITS_PER_MANTISSA_BYTE, '0')}"
        }

    fun decimalBytes(): String =
        bytes.joinToString(" ") { it.toString().padStart(DECIMAL_DIGITS_PER_MANTISSA_BYTE, '0') }
}

fun parseTiBasicDecimalLiteral(text: String): BigDecimal? {
    if (text.isBlank()) return null
    var sign = 1
    var index = 0
    while (index < text.length && (text[index] == '+' || text[index] == '-')) {
        if (text[index] == '-') {
            sign *= -1
        }
        index++
    }
    val unsignedText = text.substring(index)
        .takeIf(String::isNotEmpty)
        ?.let { if (it.startsWith('.')) "0$it" else it }
        ?: return null
    val value = runCatching { BigDecimal(unsignedText) }.getOrNull() ?: return null
    return if (sign < 0) value.negate() else value
}

fun tiBasicRadix100Number(value: BigDecimal): TiBasicRadix100Number? {
    if (value.signum() == 0) {
        return TiBasicRadix100Number(
            bytes = ZERO_BYTES,
            exponent = null,
            mantissaBytes = List(MANTISSA_BYTE_COUNT) { 0 },
            value = BigDecimal.ZERO,
            isNegative = false,
        )
    }
    val absoluteValue = value.abs().stripTrailingZeros()
    var exponent = Math.floorDiv(absoluteValue.precision() - absoluteValue.scale() - 1, DECIMAL_DIGITS_PER_MANTISSA_BYTE)
    var mantissaValue = absoluteValue
        .movePointLeft(exponent * DECIMAL_DIGITS_PER_MANTISSA_BYTE)
        .movePointRight(FRACTIONAL_DIGITS_IN_MANTISSA)
        .setScale(0, RoundingMode.HALF_EVEN)
    if (mantissaValue >= MANTISSA_OVERFLOW_THRESHOLD) {
        mantissaValue = mantissaValue.movePointLeft(DECIMAL_DIGITS_PER_MANTISSA_BYTE)
        exponent++
    }
    if (exponent !in MIN_EXPONENT..MAX_EXPONENT) return null
    val mantissaBytes = mantissaValue
        .toBigIntegerExact()
        .toString()
        .padStart(STORED_DECIMAL_DIGITS, '0')
        .chunked(DECIMAL_DIGITS_PER_MANTISSA_BYTE)
        .map(String::toInt)
    val exponentByte = exponent + EXPONENT_OFFSET
    val positiveFirstWord = (exponentByte shl 8) or mantissaBytes.first()
    val storedFirstWord =
        if (value.signum() < 0) {
            ((WORD_MASK + 1) - positiveFirstWord) and WORD_MASK
        } else {
            positiveFirstWord
        }
    val bytes = buildList {
        add((storedFirstWord shr 8) and BYTE_MASK)
        add(storedFirstWord and BYTE_MASK)
        addAll(mantissaBytes.drop(1))
    }
    return tiBasicRadix100Number(bytes)
}

fun tiBasicRadix100Number(bytes: List<Int>): TiBasicRadix100Number? {
    if (bytes.size != STORED_BYTE_COUNT || bytes.any { it !in 0..BYTE_MASK }) return null
    if (bytes[0] == 0 && bytes[1] == 0) {
        return TiBasicRadix100Number(
            bytes = bytes,
            exponent = null,
            mantissaBytes = bytes.drop(1),
            value = BigDecimal.ZERO,
            isNegative = false,
        )
    }
    val isNegative = bytes[0] >= SIGN_BIT_MASK
    val storedFirstWord = (bytes[0] shl 8) or bytes[1]
    val positiveFirstWord =
        if (isNegative) {
            ((WORD_MASK + 1) - storedFirstWord) and WORD_MASK
        } else {
            storedFirstWord
        }
    val exponentByte = (positiveFirstWord shr 8) and BYTE_MASK
    val exponent = exponentByte - EXPONENT_OFFSET
    if (exponent !in MIN_EXPONENT..MAX_EXPONENT) return null
    val mantissaBytes = buildList {
        add(positiveFirstWord and BYTE_MASK)
        addAll(bytes.drop(2))
    }
    if (mantissaBytes.any { it !in 0..99 }) return null
    val mantissaDigits = mantissaBytes
        .joinToString(separator = "") { it.toString().padStart(DECIMAL_DIGITS_PER_MANTISSA_BYTE, '0') }
    val magnitude = BigDecimal(BigInteger(mantissaDigits))
        .scaleByPowerOfTen((exponent * DECIMAL_DIGITS_PER_MANTISSA_BYTE) - FRACTIONAL_DIGITS_IN_MANTISSA)
    return TiBasicRadix100Number(
        bytes = bytes,
        exponent = exponent,
        mantissaBytes = mantissaBytes,
        value = if (isNegative) magnitude.negate() else magnitude,
        isNegative = isNegative,
    )
}

fun tiBasicDecimalString(value: BigDecimal): String =
    value.stripTrailingZeros()
        .toPlainString()
