package com.jetbrains.spark.submit.run.ssh

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.filters.UrlFilter
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.remote.SshConnectionConfigPatch
import com.intellij.ssh.ProcessBuilder
import com.intellij.ssh.SshException
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ssh.process.CapturingSshProcessHandler
import com.intellij.ssh.processBuilder
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.integration.MonitoringServiceProvider
import com.jetbrains.bigdatatools.common.rfs.util.withSlash
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.run.cluster.RemoteTargetId
import com.jetbrains.spark.submit.run.cluster.RemoteTargetProvider
import com.jetbrains.spark.submit.run.common.AbstractSparkCommandLineModel
import com.jetbrains.spark.submit.run.ssh.runner.SshSparkSubmitRunner
import com.jetbrains.spark.submit.run.ssh.util.SshUtils
import com.jetbrains.spark.submit.util.SparkAppIdUtils
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import java.io.File
import java.time.Duration

class SshAwareSparkCommandLineModel(
  project: Project,
  configuration: SshAwareSparkJobRunConfiguration
) : AbstractSparkCommandLineModel<SshAwareSparkJobRunConfiguration>(project, configuration) {
  override val isLocal: Boolean get() = false

  public override fun getSparkSubmitPath() = configuration.sparkHome + "/bin/spark-submit"

  override fun getRealPath(filePath: FilePath): FilePath? {
    val origin = super.getRealPath(filePath) ?: return null
    return when (origin.type) {
      FileType.FILE, FileType.UPLOAD -> {
        val remoteFolder = SshUtils.prepareTargetDir(configuration.targetDirectory.path.withSlash())
        val ioFile = File(origin.path)
        val extension = ioFile.extension
        val name = ioFile.nameWithoutExtension
        FilePath(FileType.FILE, "$remoteFolder${name}.$extension")
      }
      FileType.SERVER -> FilePath(FileType.FILE, origin.path)
      else -> origin
    }
  }

}

class SshAwareSparkRunConfigurationProfileState(
  val myProfileState: SshAwareSparkCommandLineModel,
  val environment: ExecutionEnvironment
) : RunProfileState {
  val configuration get() = myProfileState.configuration
  val project get() = myProfileState.project

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
    require(runner is SshSparkSubmitRunner)
    val console = runner.createConsoleView(project)
    val isDebug = environment.executor.id == DefaultDebugExecutor.EXECUTOR_ID
    val processHandler = createProcessHandler(myProfileState.createCommandLineWithEnv(isDebug)).also { handler ->
      ProcessTerminatedListener.attach(handler)
    }
    val remoteTargetId = configuration.remoteTargetId
    if (remoteTargetId != null) {

      initRemoteTarget(remoteTargetId, console, processHandler)
    }
    console.attachToProcess(processHandler)

    return DefaultExecutionResult(console, processHandler)
  }

  private fun initRemoteTarget(remoteTargetId: RemoteTargetId, console: ConsoleView, processHandler: ProcessHandler) {
    val remoteTarget = runBlockingMaybeCancellable {
      RemoteTargetProvider.getTarget(project, remoteTargetId)
    } ?: return
    console.addMessageFilter(object : UrlFilter() {
      override fun buildHyperlinkInfo(url: String): HyperlinkInfo = HyperlinkInfo { project ->
        val monitoringServiceProvider = MonitoringServiceProvider.getProviders().firstOrNull {
          it.isSupport(BdtConnectionType.SPARK_MONITORING.id)
        } ?: return@HyperlinkInfo

        val appId = monitoringServiceProvider.extractApplicationId(url) ?: return@HyperlinkInfo
        executeOnPooledThread {
          val connData = remoteTarget.getOrCreateSparkConnection() ?: return@executeOnPooledThread
          invokeLater {
            monitoringServiceProvider.focusOn(project, connData, appId, ignoreIfError = false)
          }
        }
      }
    })


    console.addMessageFilter(object : Filter {
      override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
        if (!SparkAppIdUtils.lineHasAppId(line))
          return null

        val matchResult = SparkAppIdUtils.findByRegex(line)
        val appId = matchResult?.value ?: return null

        val start = entireLength - line.length + matchResult.range.first

        return Filter.Result(start, start + appId.length, HyperlinkInfo {
          val monitoringServiceProvider = MonitoringServiceProvider.getProviders().firstOrNull {
            it.isSupport(BdtConnectionType.SPARK_MONITORING.id)
          } ?: return@HyperlinkInfo

          executeOnPooledThread {
            val connData = remoteTarget.getOrCreateSparkConnection() ?: return@executeOnPooledThread
            invokeLater {
              monitoringServiceProvider.focusOn(project, connData, appId, false)
            }
          }
        })
      }
    })


    executeOnPooledThread {
      remoteTarget.addApplicationToMonitoring(processHandler, configuration.name, !AbstractSparkCommandLineModel.isDebug(environment))
    }
  }

  fun createProcessHandler(commandLine: GeneralCommandLine): ProcessHandler {
    val sshConfig = runBlockingMaybeCancellable {
      myProfileState.configuration.getSshConfig() ?: error(SparkMessagesBundle.message("setup.ssh.config"))
    }
    val newConf = withKeepAlive(sshConfig)
    val connectionBuilder = SshUtils.getConnectionBuilder(myProfileState.project, newConf)

    try {
      val processBuilder: ProcessBuilder = connectionBuilder.processBuilder(commandLine)
        .withChangeDir(true)
        .withAllocatePty(myProfileState.configuration.isInteractive)

      val process = processBuilder.start()
      return CapturingSshProcessHandler(process, null, commandLine.commandLineString)
    }
    catch (ex: SshException) {
      throw ExecutionException(SparkMessagesBundle.message("dialog.message.failed.to.create.ssh.process"), ex)
    }
  }

  private fun withKeepAlive(sshConfig: SshConfig): SshConfig {
    if (sshConfig.connectionConfigPatch?.serverAliveInterval != null)
      return sshConfig

    val newConf = sshConfig.clone()
    newConf.id = "tempSparkSubmit"

    val connectionConfigPatch = newConf.connectionConfigPatch ?: SshConnectionConfigPatch(null, null, null)
    newConf.connectionConfigPatch = connectionConfigPatch.copy(serverAliveInterval = Duration.ofSeconds(5))

    return newConf
  }

}