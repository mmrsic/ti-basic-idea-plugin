package com.github.mmrsic.idea.plugins.tibasic.findusages

import com.github.mmrsic.idea.plugins.tibasic.TiBasicTestBase
import com.github.mmrsic.idea.plugins.tibasic.psi.expression.TiBasicVariableAccess
import com.intellij.psi.util.PsiTreeUtil

class TiBasicFindUsagesTest : TiBasicTestBase() {

    fun `test getName returns uppercase variable name`() {
        val file = configureFile("100 LET abc=5")
        val varAccess = PsiTreeUtil.findChildrenOfType(file, TiBasicVariableAccess::class.java).first()
        assertEquals("ABC", varAccess.name)
    }

    fun `test getName returns string variable name including dollar sign`() {
        val file = configureFile("100 LET A\$=\"HELLO\"")
        val varAccess = PsiTreeUtil.findChildrenOfType(file, TiBasicVariableAccess::class.java).first()
        assertEquals("A\$", varAccess.name)
    }

    fun `test getName for array returns base name without subscript`() {
        val file = configureFile("100 LET A(1)=5")
        val varAccess = PsiTreeUtil.findChildrenOfType(file, TiBasicVariableAccess::class.java).first()
        assertEquals("A", varAccess.name)
    }

    fun `test variable access has a reference provided by contributor`() {
        val file = configureFile("100 LET A=5")
        val varAccess = PsiTreeUtil.findChildrenOfType(file, TiBasicVariableAccess::class.java).first()
        val refs = varAccess.references
        assertTrue("Reference contributor must provide a reference", refs.isNotEmpty())
        assertInstanceOf(refs.first(), TiBasicVariableReference::class.java)
    }

    fun `test reference always resolves to null`() {
        val file = configureFile("100 LET A=5\n200 PRINT A")
        val accesses = PsiTreeUtil.findChildrenOfType(file, TiBasicVariableAccess::class.java).toList()
        accesses.forEach { access ->
            assertNull("resolve() must always be null for ${access.text}", TiBasicVariableReference(access).resolve())
        }
    }

    fun `test sameVariable matches same numeric variable`() {
        val file = configureFile("100 LET A=1\n200 PRINT A")
        val accesses = PsiTreeUtil.findChildrenOfType(file, TiBasicVariableAccess::class.java).toList()
        assertTrue(TiBasicVariableReference.sameVariable(accesses[0], accesses[1]))
    }

    fun `test sameVariable does not match different names`() {
        val file = configureFile("100 LET A=1\n200 PRINT B")
        val accesses = PsiTreeUtil.findChildrenOfType(file, TiBasicVariableAccess::class.java).toList()
        assertFalse(TiBasicVariableReference.sameVariable(accesses[0], accesses[1]))
    }

    fun `test sameVariable does not match numeric and string variable with same base name`() {
        val file = configureFile("100 LET A=1\n200 LET A\$=\"X\"")
        val accesses = PsiTreeUtil.findChildrenOfType(file, TiBasicVariableAccess::class.java).toList()
        assertFalse(TiBasicVariableReference.sameVariable(accesses[0], accesses[1]))
    }

    fun `test sameVariable does not match scalar and array with same name`() {
        val file = configureFile("100 LET A=1\n200 LET A(1)=2")
        val accesses = PsiTreeUtil.findChildrenOfType(file, TiBasicVariableAccess::class.java).toList()
        assertFalse(TiBasicVariableReference.sameVariable(accesses[0], accesses[1]))
    }

    fun `test isReferenceTo returns true for all occurrences of same variable`() {
        val file = configureFile("100 LET A=5\n200 PRINT A\n300 LET B=A+1")
        val allA = PsiTreeUtil.findChildrenOfType(file, TiBasicVariableAccess::class.java)
            .filter { it.name == "A" }
        assertEquals(3, allA.size)
        allA.forEach { target ->
            allA.forEach { source ->
                assertTrue(
                    "Occurrence at ${source.textOffset} should reference target at ${target.textOffset}",
                    TiBasicVariableReference(source).isReferenceTo(target),
                )
            }
        }
    }

    fun `test isReferenceTo returns false for different variable`() {
        val file = configureFile("100 LET A=5\n200 PRINT B")
        val accesses = PsiTreeUtil.findChildrenOfType(file, TiBasicVariableAccess::class.java).toList()
        val refToA = TiBasicVariableReference(accesses[0])
        assertFalse(refToA.isReferenceTo(accesses[1]))
    }

    fun `test isReferenceTo returns false for string variable when target is numeric`() {
        val file = configureFile("100 LET A=1\n200 LET A\$=\"X\"")
        val accesses = PsiTreeUtil.findChildrenOfType(file, TiBasicVariableAccess::class.java).toList()
        val numRef = TiBasicVariableReference(accesses[0])
        assertFalse(numRef.isReferenceTo(accesses[1]))
    }

    fun `test isReferenceTo returns false for array when target is scalar`() {
        val file = configureFile("100 LET A=1\n200 LET A(1)=2")
        val accesses = PsiTreeUtil.findChildrenOfType(file, TiBasicVariableAccess::class.java).toList()
        val scalarRef = TiBasicVariableReference(accesses[0])
        assertFalse(scalarRef.isReferenceTo(accesses[1]))
    }

    fun `test isReferenceTo returns true for string variable occurrences`() {
        val file = configureFile("100 LET A\$=\"HI\"\n200 PRINT A\$")
        val accesses = PsiTreeUtil.findChildrenOfType(file, TiBasicVariableAccess::class.java).toList()
        val ref = TiBasicVariableReference(accesses[0])
        assertTrue(ref.isReferenceTo(accesses[1]))
    }
}
