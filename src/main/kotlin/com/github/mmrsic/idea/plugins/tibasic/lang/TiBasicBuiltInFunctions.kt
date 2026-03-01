package com.github.mmrsic.idea.plugins.tibasic.lang

enum class FunctionReturnType { NUMERIC, STRING }

data class BuiltInFunctionSignature(
    val argCount: Int,
    val argTypes: List<CallArgType>,
    val returnType: FunctionReturnType,
)

object TiBasicBuiltInFunctions {

    private val signatures: Map<String, BuiltInFunctionSignature> = mapOf(
        "ABS" to BuiltInFunctionSignature(1, listOf(CallArgType.NUMERIC), FunctionReturnType.NUMERIC),
        "ATN" to BuiltInFunctionSignature(1, listOf(CallArgType.NUMERIC), FunctionReturnType.NUMERIC),
        "COS" to BuiltInFunctionSignature(1, listOf(CallArgType.NUMERIC), FunctionReturnType.NUMERIC),
        "EXP" to BuiltInFunctionSignature(1, listOf(CallArgType.NUMERIC), FunctionReturnType.NUMERIC),
        "INT" to BuiltInFunctionSignature(1, listOf(CallArgType.NUMERIC), FunctionReturnType.NUMERIC),
        "LOG" to BuiltInFunctionSignature(1, listOf(CallArgType.NUMERIC), FunctionReturnType.NUMERIC),
        "RND" to BuiltInFunctionSignature(0, emptyList(), FunctionReturnType.NUMERIC),
        "SGN" to BuiltInFunctionSignature(1, listOf(CallArgType.NUMERIC), FunctionReturnType.NUMERIC),
        "SIN" to BuiltInFunctionSignature(1, listOf(CallArgType.NUMERIC), FunctionReturnType.NUMERIC),
        "SQR" to BuiltInFunctionSignature(1, listOf(CallArgType.NUMERIC), FunctionReturnType.NUMERIC),
        "TAN" to BuiltInFunctionSignature(1, listOf(CallArgType.NUMERIC), FunctionReturnType.NUMERIC),
    )

    fun numericFunctionNames(): Set<String> =
        signatures.filter { it.value.returnType == FunctionReturnType.NUMERIC }.keys

    fun stringFunctionNames(): Set<String> =
        signatures.filter { it.value.returnType == FunctionReturnType.STRING }.keys

    fun allNames(): Set<String> = signatures.keys

    fun byName(name: String?): BuiltInFunctionSignature? =
        if (name == null) null else signatures[name.uppercase()]
}
