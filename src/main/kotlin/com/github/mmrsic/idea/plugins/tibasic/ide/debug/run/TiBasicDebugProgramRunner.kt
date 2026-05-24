package com.github.mmrsic.idea.plugins.tibasic.ide.debug.run

import com.github.mmrsic.idea.plugins.tibasic.ide.debug.TiBasicDebugMetadata
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.diagnostic.thisLogger

class TiBasicDebugProgramRunner : ProgramRunner<com.intellij.execution.configurations.RunnerSettings> {

    override fun getRunnerId(): String = TiBasicDebugMetadata.programRunnerId

    override fun canRun(executorId: String, profile: RunProfile): Boolean =
        executorId == DefaultDebugExecutor.EXECUTOR_ID && profile is TiBasicDebugRunConfiguration

    override fun execute(environment: ExecutionEnvironment) {
        val state = environment.state ?: throw ExecutionException(environment.toString())
        try {
            state.execute(environment.executor, this)
        } catch (ex: ExecutionException) {
            throw ex
        } catch (ex: Exception) {
            thisLogger().error(ex)
            throw ExecutionException(ex.message ?: TiBasicDebugMetadata.programRunnerId, ex)
        }
    }
}
