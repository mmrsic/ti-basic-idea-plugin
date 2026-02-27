package com.github.mmrsic.idea.plugins.tibasic.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

val ASTNode.allChildren: Array<ASTNode> get() = getChildren(null)

val ASTNode.childSequence: Sequence<ASTNode> get() = generateSequence(firstChildNode) { it.treeNext }

val ASTNode.nonWhitespaceChildren: List<ASTNode> get() = allChildren.filter { it.elementType != TokenType.WHITE_SPACE }

val ASTNode.firstChildType: IElementType? get() = firstChildNode?.elementType

fun ASTNode.childrenOfType(type: IElementType): List<ASTNode> = allChildren.filter { it.elementType == type }

fun ASTNode.firstChildOfType(type: IElementType): ASTNode? = allChildren.firstOrNull { it.elementType == type }

fun ASTNode.childrenAfter(type: IElementType): List<ASTNode> =
    allChildren.dropWhile { it.elementType != type }.drop(1)
