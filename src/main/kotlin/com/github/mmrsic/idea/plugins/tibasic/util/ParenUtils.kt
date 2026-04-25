package com.github.mmrsic.idea.plugins.tibasic.util

private val REM_LINE_PATTERN = Regex("""^\s*\d+\s+REM\b.*""", RegexOption.IGNORE_CASE)

fun isRemLine(lineText: String): Boolean = REM_LINE_PATTERN.matches(lineText)

fun countUnclosedParens(lineText: String): Int {
    if (isRemLine(lineText)) return 0
    var open = 0
    var i = 0
    while (i < lineText.length) {
        when (lineText[i]) {
            '"' -> {
                i++
                while (i < lineText.length) {
                    if (lineText[i] == '"') {
                        i++
                        if (i < lineText.length && lineText[i] == '"') i++ else break
                    } else {
                        i++
                    }
                }
            }

            '(' -> {
                open++; i++
            }

            ')' -> {
                open = maxOf(0, open - 1); i++
            }

            else -> i++
        }
    }
    return open
}
