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
            "BREAK",
            "CALL",
            "DELETE",
            "ELSE",
            "END",
            "GO TO",
            "GOTO",
            "FOR",
            "IF",
            "LET",
            "NEXT",
            "ON",
            "PRINT",
            "DISPLAY",
            "INPUT",
            "READ",
            "DATA",
            "RESTORE",
            "REM",
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
