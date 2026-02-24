package com.github.mmrsic.idea.plugins.tibasic

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
            "PRINT",
            "TRACE",
            "UNBREAK",
            "UNTRACE",
        )
}

