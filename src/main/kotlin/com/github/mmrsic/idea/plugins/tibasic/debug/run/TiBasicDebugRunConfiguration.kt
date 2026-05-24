package com.github.mmrsic.idea.plugins.tibasic.debug.run

import com.github.mmrsic.idea.plugins.tibasic.debug.TiBasicDebugMetadata
import com.github.mmrsic.idea.plugins.tibasic.lang.fileTypeExtensions
import com.intellij.execution.Executor
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.LocatableRunConfigurationOptions
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.xmlb.annotations.OptionTag
import org.jetbrains.annotations.Nls
import java.io.File

class TiBasicDebugRunConfiguration(
    project: Project,
    factory: TiBasicDebugConfigurationFactory,
) : LocatableConfigurationBase<TiBasicDebugRunConfigurationOptions>(project, factory) {

    var filePath: String?
        get() = options.filePath
        set(value) {
            options.filePath = value
        }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? =
        if (executor.id == DefaultDebugExecutor.EXECUTOR_ID) {
            TiBasicDebugRunProfileState(project, this)
        } else {
            null
        }

    override fun getConfigurationEditor(): SettingsEditor<out com.intellij.execution.configurations.RunConfiguration> =
        TiBasicDebugRunConfigurationEditor(project)

    override fun checkConfiguration() {
        val currentFilePath = filePath ?: throw RuntimeConfigurationError(
            TiBasicDebugMetadata.message(TiBasicDebugMetadata.configurationEditorFileLabelKey),
        )
        val file = File(currentFilePath)
        if (!file.isFile) {
            throw RuntimeConfigurationError(currentFilePath)
        }
        if (fileTypeExtensions.none { extension -> file.name.endsWith(".$extension", ignoreCase = true) }) {
            throw RuntimeConfigurationError(currentFilePath)
        }
    }

    override fun suggestedName(): @Nls String? =
        filePath?.let(FileUtil::getNameWithoutExtension)

    private val options: TiBasicDebugRunConfigurationOptions
        get() = getOptions() as TiBasicDebugRunConfigurationOptions
}

class TiBasicDebugRunConfigurationOptions : LocatableRunConfigurationOptions() {
    @get:OptionTag("filePath")
    var filePath: String? by string()
}
