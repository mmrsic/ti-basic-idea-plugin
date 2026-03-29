package com.github.mmrsic.idea.plugins.tibasic.toolwindow

enum class AccessType { READ, WRITE, NONE }

data class TiBasicVariableOccurrence(
    val lineNumber: Int,
    val offset: Int,
    val accessType: AccessType,
)
