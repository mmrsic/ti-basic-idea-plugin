package com.github.mmrsic.idea.plugins.tibasic.editor

import com.intellij.testFramework.fixtures.BasePlatformTestCase

abstract class TiBasicAutoLineNumberTestBase : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        resetAutoLineNumberSettings()
    }

    override fun tearDown() {
        try {
            resetAutoLineNumberSettings()
        } finally {
            super.tearDown()
        }
    }

    protected fun resetAutoLineNumberSettings() {
        TiBasicAutoLineNumberSettings.getInstance().autoLineNumberDelta = DEFAULT_AUTO_LINE_NUMBER_DELTA
        TiBasicAutoLineNumberSettings.getInstance().roundToTens = false
    }
}
