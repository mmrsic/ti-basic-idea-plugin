package com.github.mmrsic.idea.plugins.tibasic.ext

import com.intellij.lang.ASTNode

val ASTNode.allChildren: Array<ASTNode> get() = getChildren(null)
