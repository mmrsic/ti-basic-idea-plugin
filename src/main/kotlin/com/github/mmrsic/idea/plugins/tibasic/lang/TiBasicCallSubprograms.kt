package com.github.mmrsic.idea.plugins.tibasic.lang

const val BAD_NAME_RUNTIME_ERROR = "Will cause run-time error 'BAD NAME'"
const val BAD_VALUE_RUNTIME_ERROR = "Will cause run-time error 'BAD VALUE'"
const val INCORRECT_STATEMENT_RUNTIME_ERROR = "Will cause run-time error 'INCORRECT STATEMENT'"

enum class CallArgType { NUMERIC, STRING }

data class CallSubprogramSignature(
    val validArgCounts: Set<Int>,
    val argTypes: List<CallArgType>,
    val syntaxViolationError: String? = null,
)

object TiBasicCallSubprograms {

    private val signatures: Map<String, CallSubprogramSignature> = mapOf(
        "CLEAR"  to CallSubprogramSignature(setOf(0), emptyList()),
        "SCREEN" to CallSubprogramSignature(setOf(1), listOf(CallArgType.NUMERIC), INCORRECT_STATEMENT_RUNTIME_ERROR),
        "COLOR"  to CallSubprogramSignature(setOf(3), listOf(CallArgType.NUMERIC, CallArgType.NUMERIC, CallArgType.NUMERIC), INCORRECT_STATEMENT_RUNTIME_ERROR),
        "HCHAR"  to CallSubprogramSignature(setOf(3, 4), listOf(CallArgType.NUMERIC, CallArgType.NUMERIC, CallArgType.NUMERIC, CallArgType.NUMERIC), INCORRECT_STATEMENT_RUNTIME_ERROR),
        "VCHAR"  to CallSubprogramSignature(setOf(3, 4), listOf(CallArgType.NUMERIC, CallArgType.NUMERIC, CallArgType.NUMERIC, CallArgType.NUMERIC), INCORRECT_STATEMENT_RUNTIME_ERROR),
        "GCHAR"  to CallSubprogramSignature(setOf(3), listOf(CallArgType.NUMERIC, CallArgType.NUMERIC, CallArgType.NUMERIC), INCORRECT_STATEMENT_RUNTIME_ERROR),
        "CHAR"   to CallSubprogramSignature(setOf(2), listOf(CallArgType.NUMERIC, CallArgType.STRING)),
        "KEY"    to CallSubprogramSignature(setOf(3), listOf(CallArgType.NUMERIC, CallArgType.NUMERIC, CallArgType.NUMERIC)),
        "JOYST"  to CallSubprogramSignature(setOf(3), listOf(CallArgType.NUMERIC, CallArgType.NUMERIC, CallArgType.NUMERIC)),
        "SOUND"  to CallSubprogramSignature(setOf(3, 5, 7, 9), listOf(CallArgType.NUMERIC, CallArgType.NUMERIC, CallArgType.NUMERIC)),
    )

    fun names(): Set<String> = signatures.keys
    fun byName(name: String?): CallSubprogramSignature? = if (name == null) null else signatures[name.uppercase()]
}
