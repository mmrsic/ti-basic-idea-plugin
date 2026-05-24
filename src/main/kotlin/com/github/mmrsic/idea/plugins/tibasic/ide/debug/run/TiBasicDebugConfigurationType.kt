package com.github.mmrsic.idea.plugins.tibasic.ide.debug.run

import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugMetadata
import com.github.mmrsic.idea.plugins.tibasic.ide.language.TiBasicFileType
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import javax.swing.Icon

class TiBasicDebugConfigurationType : ConfigurationType, DumbAware {

    private val factory = TiBasicDebugConfigurationFactory(this)

    override fun getDisplayName(): String =
        TiBasicDebugMetadata.message(TiBasicDebugMetadata.configurationTypeDisplayNameKey)

    override fun getConfigurationTypeDescription(): String =
        TiBasicDebugMetadata.message(TiBasicDebugMetadata.configurationTypeDescriptionKey)

    override fun getIcon(): Icon = TiBasicFileType.icon

    override fun getId(): String = TiBasicDebugMetadata.configurationTypeId

    override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(factory)

}

class TiBasicDebugConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        TiBasicDebugRunConfiguration(project, this)

    override fun getId(): String = TiBasicDebugMetadata.configurationFactoryId

    override fun getOptionsClass(): Class<TiBasicDebugRunConfigurationOptions> =
        TiBasicDebugRunConfigurationOptions::class.java
}

fun instance(): TiBasicDebugConfigurationType =
    ConfigurationTypeUtil.findConfigurationType(TiBasicDebugConfigurationType::class.java)