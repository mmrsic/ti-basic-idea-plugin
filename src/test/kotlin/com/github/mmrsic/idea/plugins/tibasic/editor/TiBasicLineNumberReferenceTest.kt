package com.github.mmrsic.idea.plugins.tibasic.editor

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.ext.resolveReferencedLine
import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicLine
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import com.intellij.psi.PsiElement

class TiBasicLineNumberReferenceTest : TiBasicTestBase() {

    fun `test GOTO line number literal resolves to target line`() {
        configureFile("100 GOTO <caret>200\n200 PRINT \"OK\"")

        assertEquals(200, elementAtCaret().resolveReferencedLine()?.lineNumber())
    }

    fun `test GOSUB line number literal resolves to target line`() {
        configureFile("100 GOSUB <caret>300\n200 END\n300 RETURN")

        assertEquals(300, elementAtCaret().resolveReferencedLine()?.lineNumber())
    }

    fun `test IF THEN ELSE line number literals resolve to target lines`() {
        configureFile("100 IF X>0 THEN <caret>200 ELSE 300\n200 PRINT \"A\"\n300 PRINT \"B\"")
        assertEquals(200, elementAtCaret().resolveReferencedLine()?.lineNumber())

        configureFile("100 IF X>0 THEN 200 ELSE <caret>300\n200 PRINT \"A\"\n300 PRINT \"B\"")
        assertEquals(300, elementAtCaret().resolveReferencedLine()?.lineNumber())
    }

    fun `test ON GOTO line number literal resolves to target line`() {
        configureFile("100 ON X GOTO <caret>200,300\n200 PRINT \"A\"\n300 PRINT \"B\"")

        assertEquals(200, elementAtCaret().resolveReferencedLine()?.lineNumber())
    }

    fun `test numeric literal outside line reference context does not resolve to line`() {
        configureFile("100 LET X=<caret>200\n200 PRINT \"OK\"")

        assertNull(elementAtCaret().resolveReferencedLine())
        assertNull(GotoDeclarationAction.findTargetElement(project, myFixture.editor, myFixture.editor.caretModel.offset))
    }

    fun `test unresolved line number reference has no goto target`() {
        configureFile("100 GOTO <caret>200\n300 PRINT \"OK\"")

        assertNull(elementAtCaret().resolveReferencedLine())
        assertNull(GotoDeclarationAction.findTargetElement(project, myFixture.editor, myFixture.editor.caretModel.offset))
    }

    fun `test out of range line number reference has no goto target`() {
        configureFile("100 GOTO <caret>40000\n40000 PRINT \"OK\"")

        assertNull(elementAtCaret().resolveReferencedLine())
        assertNull(GotoDeclarationAction.findTargetElement(project, myFixture.editor, myFixture.editor.caretModel.offset))
    }

    fun `test goto declaration navigates from line number reference to target line`() {
        configureFile("100 GOTO <caret>200\n200 PRINT \"OK\"")

        val target = GotoDeclarationAction.findTargetElement(project, myFixture.editor, myFixture.editor.caretModel.offset)

        assertEquals(200, target.containingLine()?.lineNumber())
    }

    private fun elementAtCaret(): PsiElement =
        myFixture.file.findElementAt(myFixture.editor.caretModel.offset)
            ?: error("No PSI element at caret")

    private fun PsiElement?.containingLine(): TiBasicLine? = when (this) {
        null -> null
        is TiBasicLine -> this
        else -> parent as? TiBasicLine
    }
}
