package com.github.mmrsic.idea.plugins.tibasic.ide.findusages

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.common.ext.firstChildOfType
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.lexer.TiBasicTokenTypes
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicCallStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicFunctionCall
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.expression.TiBasicVariableAccess
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicDefStatement
import com.github.mmrsic.idea.plugins.tibasic.language.syntax.psi.statement.TiBasicPrintStatement
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo

class TiBasicFindUsagesHandlerTest : TiBasicTestBase() {

    fun `test find usages of PRINT statement finds all PRINT statements`() {
        val file = configureFile("100 PRINT \"A\"\n200 PRINT \"B\"\n300 LET A=1")
        val printStatement = PsiTreeUtil.findChildrenOfType(file, TiBasicPrintStatement::class.java).first()
        val printKeyword = printStatement.node.firstChildNode.psi
        val handler = TiBasicFindUsagesHandler(printKeyword)
        val usages = mutableListOf<UsageInfo>()
        handler.processElementUsages(printKeyword, { usages.add(it); true }, FindUsagesOptions(project))
        assertEquals(2, usages.size)
    }

    fun `test find usages of PRINT does not include other statement types`() {
        val file = configureFile("100 PRINT \"A\"\n200 INPUT A\n300 PRINT \"B\"")
        val printStatement = PsiTreeUtil.findChildrenOfType(file, TiBasicPrintStatement::class.java).first()
        val printKeyword = printStatement.node.firstChildNode.psi
        val handler = TiBasicFindUsagesHandler(printKeyword)
        val usages = mutableListOf<UsageInfo>()
        handler.processElementUsages(printKeyword, { usages.add(it); true }, FindUsagesOptions(project))
        assertEquals(2, usages.size)
    }

    fun `test find usages of CALL subprogram finds all same subprogram calls`() {
        val file = configureFile("100 CALL CLEAR\n200 CALL SOUND(100,440,30)\n300 CALL CLEAR")
        val callStatements = PsiTreeUtil.findChildrenOfType(file, TiBasicCallStatement::class.java)
        val clearStatement = callStatements.first { it.subprogramName() == "CLEAR" }
        val clearToken = clearStatement.node.firstChildOfType(TiBasicTokenTypes.CALL_SUBPROGRAM_NAME)!!.psi
        val handler = TiBasicFindUsagesHandler(clearToken)
        val usages = mutableListOf<UsageInfo>()
        handler.processElementUsages(clearToken, { usages.add(it); true }, FindUsagesOptions(project))
        assertEquals(2, usages.size)
    }

    fun `test find usages of CALL subprogram does not include other subprogram calls`() {
        val file = configureFile("100 CALL CLEAR\n200 CALL SCREEN(5)\n300 CALL CLEAR")
        val callStatements = PsiTreeUtil.findChildrenOfType(file, TiBasicCallStatement::class.java)
        val clearStatement = callStatements.first { it.subprogramName() == "CLEAR" }
        val clearToken = clearStatement.node.firstChildOfType(TiBasicTokenTypes.CALL_SUBPROGRAM_NAME)!!.psi
        val handler = TiBasicFindUsagesHandler(clearToken)
        val usages = mutableListOf<UsageInfo>()
        handler.processElementUsages(clearToken, { usages.add(it); true }, FindUsagesOptions(project))
        assertEquals(2, usages.size)
    }

    fun `test find usages of built-in function finds all calls`() {
        val file = configureFile("100 PRINT ABS(-1)\n200 LET A=ABS(5)\n300 PRINT SQR(4)")
        val functionCalls = PsiTreeUtil.findChildrenOfType(file, TiBasicFunctionCall::class.java)
        val absCall = functionCalls.first { it.functionName() == "ABS" }
        val handler = TiBasicFindUsagesHandler(absCall)
        val usages = mutableListOf<UsageInfo>()
        handler.processElementUsages(absCall, { usages.add(it); true }, FindUsagesOptions(project))
        assertEquals(2, usages.size)
    }

    fun `test find usages of built-in function does not include other functions`() {
        val file = configureFile("100 PRINT ABS(-1)\n200 PRINT SQR(4)\n300 PRINT ABS(0)")
        val functionCalls = PsiTreeUtil.findChildrenOfType(file, TiBasicFunctionCall::class.java)
        val absCall = functionCalls.first { it.functionName() == "ABS" }
        val handler = TiBasicFindUsagesHandler(absCall)
        val usages = mutableListOf<UsageInfo>()
        handler.processElementUsages(absCall, { usages.add(it); true }, FindUsagesOptions(project))
        assertEquals(2, usages.size)
    }

    fun `test find usages of user-defined function includes definition and all calls`() {
        val file = configureFile("100 DEF FNA(X)=X\n200 PRINT FNA(1)\n300 PRINT FNA(2)")
        val defStatement = PsiTreeUtil.findChildrenOfType(file, TiBasicDefStatement::class.java).first()
        val handler = TiBasicFindUsagesHandler(defStatement)
        val usages = mutableListOf<UsageInfo>()
        handler.processElementUsages(defStatement, { usages.add(it); true }, FindUsagesOptions(project))
        // 1 TiBasicDefStatement + 2 TiBasicVariableAccess call sites (FNA(1) and FNA(2))
        assertEquals(3, usages.size)
    }

    fun `test find usages of user-defined function call sites are variable accesses with subscripts`() {
        val file = configureFile("100 DEF FNA(X)=X\n200 PRINT FNA(1)\n300 PRINT FNA(2)")
        val defStatement = PsiTreeUtil.findChildrenOfType(file, TiBasicDefStatement::class.java).first()
        val handler = TiBasicFindUsagesHandler(defStatement)
        val usages = mutableListOf<UsageInfo>()
        handler.processElementUsages(defStatement, { usages.add(it); true }, FindUsagesOptions(project))
        val callSiteUsages = usages.filter { it.element is TiBasicVariableAccess }
        assertEquals(2, callSiteUsages.size)
        callSiteUsages.forEach { usage ->
            val va = usage.element as TiBasicVariableAccess
            assertEquals("FNA", va.name)
            assertTrue(va.hasSubscriptParens())
        }
    }

    fun `test find usages of user-defined function without any calls includes only definition`() {
        val file = configureFile("100 DEF FNA(X)=X\n200 PRINT \"NO CALLS\"")
        val defStatement = PsiTreeUtil.findChildrenOfType(file, TiBasicDefStatement::class.java).first()
        val handler = TiBasicFindUsagesHandler(defStatement)
        val usages = mutableListOf<UsageInfo>()
        handler.processElementUsages(defStatement, { usages.add(it); true }, FindUsagesOptions(project))
        assertEquals(1, usages.size)
    }
}
