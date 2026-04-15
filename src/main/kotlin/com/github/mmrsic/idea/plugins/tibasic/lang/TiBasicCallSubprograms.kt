package com.github.mmrsic.idea.plugins.tibasic.lang

const val BAD_NAME_RUNTIME_ERROR = "Will cause run-time error 'BAD NAME'"
const val BAD_VALUE_RUNTIME_ERROR = "Will cause run-time error 'BAD VALUE'"
const val INCORRECT_STATEMENT_RUNTIME_ERROR = "Will cause run-time error 'INCORRECT STATEMENT'"

enum class CallArgType { NUMERIC, STRING }
enum class CallArgAccess { READ, WRITE }

data class CallArgRule(
    val type: CallArgType,
    val access: CallArgAccess = CallArgAccess.READ,
    val requiresNumericVariableTarget: Boolean = false,
)

data class CallSubprogramSignature(
    val validArgCounts: Set<Int>,
    val argRules: List<CallArgRule>,
    val syntaxViolationError: String? = null,
) {
    fun argRuleAt(index: Int): CallArgRule = argRules[index % argRules.size]
}

private fun numericArg(
    access: CallArgAccess = CallArgAccess.READ,
    requiresNumericVariableTarget: Boolean = false,
): CallArgRule = CallArgRule(CallArgType.NUMERIC, access, requiresNumericVariableTarget)

private fun stringArg(
    access: CallArgAccess = CallArgAccess.READ,
): CallArgRule = CallArgRule(CallArgType.STRING, access)

object TiBasicCallSubprograms {

    private val signatures: Map<String, CallSubprogramSignature> = mapOf(
        "CLEAR" to CallSubprogramSignature(setOf(0), emptyList()),
        "SCREEN" to CallSubprogramSignature(setOf(1), listOf(numericArg()), INCORRECT_STATEMENT_RUNTIME_ERROR),
        "COLOR" to CallSubprogramSignature(
            setOf(3),
            listOf(numericArg(), numericArg(), numericArg()),
            INCORRECT_STATEMENT_RUNTIME_ERROR,
        ),
        "HCHAR" to CallSubprogramSignature(
            setOf(3, 4),
            listOf(numericArg(), numericArg(), numericArg(), numericArg()),
            INCORRECT_STATEMENT_RUNTIME_ERROR,
        ),
        "VCHAR" to CallSubprogramSignature(
            setOf(3, 4),
            listOf(numericArg(), numericArg(), numericArg(), numericArg()),
            INCORRECT_STATEMENT_RUNTIME_ERROR,
        ),
        "GCHAR" to CallSubprogramSignature(
            setOf(3),
            listOf(
                numericArg(),
                numericArg(),
                numericArg(
                    access = CallArgAccess.WRITE,
                    requiresNumericVariableTarget = true,
                ),
            ),
            INCORRECT_STATEMENT_RUNTIME_ERROR,
        ),
        "CHAR" to CallSubprogramSignature(setOf(2), listOf(numericArg(), stringArg())),
        "KEY" to CallSubprogramSignature(
            setOf(3),
            listOf(numericArg(), numericArg(access = CallArgAccess.WRITE), numericArg(access = CallArgAccess.WRITE)),
        ),
        "JOYST" to CallSubprogramSignature(
            setOf(3),
            listOf(numericArg(), numericArg(access = CallArgAccess.WRITE), numericArg(access = CallArgAccess.WRITE)),
        ),
        "SOUND" to CallSubprogramSignature(setOf(3, 5, 7, 9), listOf(numericArg(), numericArg(), numericArg())),
    )

    fun names(): Set<String> = signatures.keys
    fun byName(name: String?): CallSubprogramSignature? = if (name == null) null else signatures[name.uppercase()]
}
