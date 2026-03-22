package com.github.mmrsic.idea.plugins.tibasic.lang

object TiBasicKeywords {

    fun getKeywords(): Set<String> = keywords

    fun getCommands(): Set<String> = commands

    private val commands =
        setOf(
            "BYE",
            "CON",
            "CONTINUE",
            "EDIT",
            "LIST",
            "NEW",
            "NUM",
            "NUMBER",
            "OLD",
            "RES",
            "RESEQUENCE",
            "RUN",
            "SAVE",
        )

    private val keywords =
        commands + setOf(
            "ABS",
            "ATN",
            "BREAK",
            "CALL",
            "COS",
            "DEF",
            "DELETE",
            "DIM",
            "ELSE",
            "END",
            "EXP",
            "GO TO",
            "GOTO",
            "GOSUB",
            "GO SUB",
            "FOR",
            "IF",
            "INT",
            "LET",
            "LOG",
            "NEXT",
            "ON",
            "OPTION BASE",
            "PRINT",
            "RANDOMIZE",
            "RND",
            "DISPLAY",
            "INPUT",
            "READ",
            "DATA",
            "RESTORE",
            "SIN",
            "SGN",
            "SQR",
            "TAN",
            "REM",
            "RETURN",
            "STOP",
            "STEP",
            "TAB",
            "THEN",
            "TO",
            "TRACE",
            "UNBREAK",
            "UNTRACE",
        )
}
