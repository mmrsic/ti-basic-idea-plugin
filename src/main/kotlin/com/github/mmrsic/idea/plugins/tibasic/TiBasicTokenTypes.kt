package com.github.mmrsic.idea.plugins.tibasic

import com.intellij.psi.tree.IElementType

object TiBasicTokenTypes {
    val KEYWORD = TiBasicElementType("KEYWORD")
    val IDENTIFIER = TiBasicElementType("IDENTIFIER")
}

class TiBasicElementType(debugName: String) : IElementType(debugName, TiBasicLanguage)
