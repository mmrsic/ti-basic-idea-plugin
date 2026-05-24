package com.github.mmrsic.idea.plugins.tibasic.debug.run

import com.github.mmrsic.idea.plugins.tibasic.psi.TiBasicFile
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class TiBasicDebugRunConfigurationProducer :
    LazyRunConfigurationProducer<TiBasicDebugRunConfiguration>(),
    DumbAware {

    override fun getConfigurationFactory(): ConfigurationFactory =
        instance().configurationFactories.single()

    override fun setupConfigurationFromContext(
        configuration: TiBasicDebugRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val file = context.tiBasicFile() ?: return false
        val virtualFile = file.virtualFile ?: return false
        configuration.filePath = virtualFile.path
        configuration.setGeneratedName()
        sourceElement.set(file)
        return true
    }

    override fun isConfigurationFromContext(
        configuration: TiBasicDebugRunConfiguration,
        context: ConfigurationContext,
    ): Boolean =
        configuration.filePath == context.tiBasicFile()?.virtualFile?.path
}

private fun ConfigurationContext.tiBasicFile(): TiBasicFile? =
    when (val psiLocation = psiLocation) {
        is TiBasicFile -> psiLocation
        else -> psiLocation?.containingFile as? TiBasicFile
    }
