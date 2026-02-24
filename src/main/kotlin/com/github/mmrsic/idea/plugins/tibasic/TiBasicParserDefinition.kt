package com.github.mmrsic.idea.plugins.tibasic

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicCommentLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicDeleteStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicExpression
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicInvalidLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLineNumberListStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicPrintStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicRemStatement
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicUnknownStatement
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

class TiBasicParserDefinition : ParserDefinition {

    override fun createLexer(project: Project): Lexer = TiBasicLexer()

    override fun createParser(project: Project): PsiParser = TiBasicParser()

    override fun getFileNodeType(): IFileElementType = TiBasicNodeTypes.FILE

    override fun getWhitespaceTokens(): TokenSet = TokenSet.create(TokenType.WHITE_SPACE)

    override fun getCommentTokens(): TokenSet = TokenSet.EMPTY

    override fun getStringLiteralElements(): TokenSet = TokenSet.create(TiBasicTokenTypes.STRING_LITERAL)

    override fun createElement(node: ASTNode): PsiElement =
        when (node.elementType) {
            TiBasicNodeTypes.LINE -> TiBasicLine(node)
            TiBasicNodeTypes.PRINT_STATEMENT -> TiBasicPrintStatement(node)
            TiBasicNodeTypes.LINE_NUMBER_LIST_STATEMENT -> TiBasicLineNumberListStatement(node)
            TiBasicNodeTypes.DELETE_STATEMENT -> TiBasicDeleteStatement(node)
            TiBasicNodeTypes.REM_STATEMENT -> TiBasicRemStatement(node)
            TiBasicNodeTypes.UNKNOWN_STATEMENT -> TiBasicUnknownStatement(node)
            TiBasicNodeTypes.INVALID_LINE -> TiBasicInvalidLine(node)
            TiBasicNodeTypes.EXPRESSION -> TiBasicExpression(node)
            TiBasicNodeTypes.VARIABLE_ACCESS -> TiBasicVariableAccess(node)
            TiBasicNodeTypes.COMMENT_LINE -> TiBasicCommentLine(node)
            else -> throw IllegalArgumentException("Unknown element type: ${node.elementType}")
        }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = TiBasicFile(viewProvider)
}



