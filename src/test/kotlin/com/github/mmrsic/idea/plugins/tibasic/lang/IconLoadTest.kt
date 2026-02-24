package com.github.mmrsic.idea.plugins.tibasic.lang

import com.intellij.openapi.util.IconLoader
import org.junit.Test
import kotlin.test.assertNotNull

class IconLoadTest {
    @Test
    fun testIconLoads() {
        val icon = IconLoader.getIcon("/icons/ti99_4a_icon.svg", IconLoadTest::class.java)
        assertNotNull(icon, "Icon should load from resources")
    }
}

