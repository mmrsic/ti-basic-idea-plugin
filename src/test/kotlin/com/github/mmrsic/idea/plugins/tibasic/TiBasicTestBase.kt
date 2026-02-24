package com.github.mmrsic.idea.plugins.tibasic

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

abstract class TiBasicTestBase : BasePlatformTestCase() {

    fun configureFile(text: String): TiBasicFile {
        myFixture.configureByText("test.tibasic", text)
        return myFixture.file as TiBasicFile
    }
}
