package com.jetbrains.spark.submit.run.cluster.ui

import com.intellij.bigdatatools.coreUi.ui.bind
import com.intellij.bigdatatools.coreUi.ui.bindIcon
import com.intellij.bigdatatools.coreUi.ui.doOnChange
import com.intellij.bigdatatools.coreUi.ui.row
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper
import com.intellij.ide.DataManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.util.and
import com.intellij.openapi.observable.util.bind
import com.intellij.openapi.observable.util.not
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.NlsSafe
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.ui.UIBundle
import com.intellij.ui.components.ActionLink
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.not
import com.intellij.ui.layout.or
import com.intellij.util.ui.NamedColorUtil
import com.jetbrains.spark.submit.checker.SshConnectionChecker
import com.jetbrains.spark.submit.model.ClusterManagerType
import com.jetbrains.spark.submit.model.DeployModeType
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.model.SchemeFilePathSerializer
import com.jetbrains.spark.submit.model.inferSelectedArtifactInfo
import com.jetbrains.spark.submit.run.cluster.ClusterSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.cluster.ClusterSparkSubmitConfigurationFactory
import com.jetbrains.spark.submit.run.cluster.RemoteTarget
import com.jetbrains.spark.submit.run.common.ui.row
import com.jetbrains.spark.submit.run.ssh.ui.SshAwareSparkSubmitConfigurationEditor
import com.jetbrains.spark.submit.run.ssh.ui.SshFileSelectorContextImpl
import com.jetbrains.spark.submit.settings.RunConfigurationBlockType
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel

class ClusterSparkSubmitConfigurationEditor(
  project: Project,
  val factory: ClusterSparkSubmitConfigurationFactory,
  private val coroutineScope: CoroutineScope
) : SshAwareSparkSubmitConfigurationEditor<ClusterSparkJobRunConfiguration>(project) {
  private val isPySpark = factory.isPySpark

  private lateinit var mainPanel: DialogPanel

  val clusterComboBox = ClustersComboBox(project, coroutineScope)

  private var sshConnectionChecker: SshConnectionChecker? = null

  override suspend fun getSshConfig(): SshConfig? {
    return withContext(Dispatchers.EDT) {
      clusterComboBox.getSelected()
    }?.getOrDefaultInitSshConfig(project)
  }

  lateinit var sshLink: Cell<ActionLink>
  private val sshLinkText = AtomicProperty("")
  private val statusText = AtomicProperty("")
  private val statusIcon = AtomicProperty<Icon?>(null)
  val sshRowVisible = AtomicBooleanProperty(false)
  var debugSupported = AtomicBooleanProperty(true)

  override val artifactPathField =
    ExtendableTextFieldFileSelector(
      FileSelectorType.CLUSTER_JAR,
      SchemeFilePathSerializer,
      SshFileSelectorContextImpl(project, SparkMessagesBundle.message("dialog.artifactPath.title"), { getSshConfig() })
    ).also {
      it.component.doOnChange { adjustBeforeRunTasks() }
    }

  private var sparkDebugEnable = AtomicBooleanProperty(true)
  private var sparkDebugDriverPort: Int? = 0
  private var sparkDebugDriverSuspendDebugMode = AtomicBooleanProperty(true)
  private var sparkDebugDriverSuspendRunMode = AtomicBooleanProperty(false)

  init {
    clusterComboBox.changeListener = { selectedTarget ->
      if (selectedTarget != null) {
        sshRowVisible.set(true)
        selectedTarget.updateEditorFields(this)

        checkConnection(selectedTarget)
      }
    }

    clusterComboBox.deselectListener = {
      sshRowVisible.set(false)

      sshConnectionChecker?.cancel()

    }
  }

  override fun updateFieldsForManagerType() {
    val clusterManagerType = clusterManagerField.selectedItem as ClusterManagerType
    val deployModeType = deployModeField.selectedItem as DeployModeType
    debugSupported.set(clusterManagerType == ClusterManagerType.LOCAL || deployModeType == DeployModeType.CLIENT)
    super.updateFieldsForManagerType()
  }

  override fun Panel.mainSparkSettingsRow() {
    throw UnsupportedOperationException()
  }

  override fun createEditor(): JComponent {
    mainPanel = panel {
      rowsRange {
        row(SparkMessagesBundle.message("settings.run.target.config"), clusterComboBox)
        row(SparkMessagesBundle.message("settings.ssh.config")) {
          panel {
            row {
              label(SparkMessagesBundle.message("settings.run.target.tooltip")).applyToComponent {
                foreground = NamedColorUtil.getInactiveTextColor()
              }.visibleIf(!sshRowVisible)
              sshLink = link("") {
                clusterComboBox.getSelected()?.let {
                  val sshConfig = runWithModalProgressBlocking(project, MessagesBundle.message("ssh.state.init")) {
                    it.openSshConfigDialog(this@ClusterSparkSubmitConfigurationEditor)
                  }
                  if (sshConfig != null) {
                    checkConnection(it)
                  }
                }
              }.applyToComponent {
                bind(sshLinkText)
                toolTipText = SparkMessagesBundle.message("edit.ssh.configuration")
              }.visibleIf(sshRowVisible)
              label("").applyToComponent {
                bind(statusText)
                bindIcon(statusIcon)
              }.visibleIf(sshRowVisible)
            }
          }
        }
      }

      artifactRows(isPySpark = ComponentPredicate.fromValue(isPySpark))
      row { cell(optionsLink).align(AlignX.RIGHT) }
      optionBlock(RunConfigurationBlockType.SPARK_CONFIG) { sparkConfSettingsRowEx() }
      optionBlock(RunConfigurationBlockType.DEPENDENCIES) { dependencySettingsRow() }
      optionBlock(RunConfigurationBlockType.MAVEN_DEPENDENCIES) { mavenSettingsRow() }
      optionBlock(RunConfigurationBlockType.DRIVER) { driverSettingsRow() }
      optionBlock(RunConfigurationBlockType.EXECUTOR) { executorSettingsRow() }
      optionBlock(RunConfigurationBlockType.KERBEROS) { kerberosSettingsRow() }
      optionBlock(RunConfigurationBlockType.SHELL_OPTIONS) { shellOptionSettingsRow() }
      optionBlock(RunConfigurationBlockType.ADDITIONAL) { additionalSparkSettingsRow() }
      finalCommandRow()
      updateFieldsForManagerType()
    }
    mainPanel.registerValidators(this)

    return mainPanel
  }

  private fun Panel.sparkConfSettingsRowEx() {
    clusterManagerField(needGap = true)
    deployMainRows()
    row(SparkMessagesBundle.message("settings.ssh.target.dir"), targetDirectory.component)
    sparkHomeRow()

    sparkConfSettingsRow()

    if (!isPySpark) {
      group(SparkMessagesBundle.message("settings.debug.title")) {
        sparkDebugSettingsRow()
      }
    }
  }

  override fun Panel.dependencySettingsRow() {
    val pyArgumentsVisible = ComponentPredicate.fromValue(isPySpark) or !pyFilesField.isEmpty()
    row(pyFilesField).visibleIf(pyArgumentsVisible)
    row(jarsField)
    row(filesField)
  }

  private fun Panel.sparkDebugSettingsRow() {
    row {
      checkBox(SparkMessagesBundle.message("settings.debug.driver.java.enable"))
        .bindSelected(sparkDebugEnable)
        .enabledIf(debugSupported)
        .applyToComponent {
          toolTipText = SparkMessagesBundle.message("settings.debug.driver.java.tooltip")
        }
    }
    indent {
      row(SparkMessagesBundle.message("settings.debug.driver.java.port")) {
        val portRange = 0..65536
        textField()
          .validationOnInput {
            when {
              it.text.isEmpty() -> null
              it.text.toIntOrNull() in portRange -> null
              else -> error(UIBundle.message("please.enter.a.number.from.0.to.1", portRange.first, portRange.last))
            }
          }
          .applyToComponent { emptyText.text = SparkMessagesBundle.message("settings.debug.driver.java.port.dynamic") }
          .bindText({ sparkDebugDriverPort?.toString().orEmpty() }, { sparkDebugDriverPort = it.toIntOrNull() })
          .gap(RightGap.COLUMNS)

      }.enabledIf(sparkDebugEnable.and(debugSupported)).visibleIf(debugSupported)
      row(JLabel(SparkMessagesBundle.message("settings.debug.driver.java.suspend")).apply { toolTipText = SparkMessagesBundle.message("settings.debug.driver.java.suspend.tooltip") }) {
        checkBox(SparkMessagesBundle.message("settings.debug.driver.in.debug.mode")).applyToComponent {
          toolTipText = SparkMessagesBundle.message("settings.debug.driver.java.suspend.tooltip")
        }.bindSelected(sparkDebugDriverSuspendDebugMode)
      }.enabledIf(sparkDebugEnable.and(debugSupported)).visibleIf(debugSupported)
      row {
        @Suppress("HardCodedStringLiteral")
        text("<icon src='AllIcons.General.Warning'>&nbsp;${SparkMessagesBundle.message("settings.debug.driver.java.not.supported")}")
      }.visibleIf(debugSupported.not())
    }
  }

  override fun createTemporaryConfiguration() =
    factory.createTemplateConfiguration(project)

  override fun loadSparkCommand(sparkCommand: @NlsSafe String) {
    val remoteTarget = clusterComboBox.getSelected()
    super.loadSparkCommand(sparkCommand)
    clusterComboBox.setSelected(remoteTarget?.id)
  }

  override fun resetEditorFrom(s: ClusterSparkJobRunConfiguration) {
    clusterComboBox.setIsTemplate(s.isTemplate)
    clusterComboBox.setSelected(s.remoteTargetId)
    artifactPathField.selectedArtifactInfo = s.selectedArtifactInfo

    sparkDebugEnable.set(s.debugDriverEnable)
    sparkDebugDriverPort = s.debugDriverPort
    sparkDebugDriverSuspendDebugMode.set(s.debugDriverSuspend ?: true)
    sparkDebugDriverSuspendRunMode.set(s.debugDriverSuspend ?: false)

    mainPanel.reset()
    super.resetEditorFrom(s)
  }

  override fun applyEditorTo(s: ClusterSparkJobRunConfiguration) {
    mainPanel.apply()
    s.remoteTargetId = clusterComboBox.getSelectedOrPrevId()
    s.remoteTargetValid = clusterComboBox.isSelectedValid()
    s.selectedArtifactInfo = artifactPathField.selectedArtifactInfo

    s.debugDriverEnable = sparkDebugEnable.get()
    s.debugDriverPort = sparkDebugDriverPort
    s.debugDriverSuspend = sparkDebugDriverSuspendDebugMode.get().takeIf { it == sparkDebugDriverSuspendRunMode.get() }

    super.applyEditorTo(s)
  }

  private fun adjustBeforeRunTasks() {
    val selectedArtifactInfo = artifactPathField.selectedArtifactInfo
    val dataContext = DataManager.getInstance().getDataContext(component)
    val editorWrapper = ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(dataContext)
    if (selectedArtifactInfo != null && editorWrapper != null) {
      val beforeTasks = if (artifactPathField.path == selectedArtifactInfo.filePath) {
        selectedArtifactInfo.createBeforeTasks(editorWrapper.stepsBeforeLaunch)
      }
      else {
        val typedArtifactInfo = artifactPathField.path.inferSelectedArtifactInfo()
        val stepsToRemove = selectedArtifactInfo.createBeforeTasks(emptyList()).toSet()
        typedArtifactInfo.createBeforeTasks(editorWrapper.stepsBeforeLaunch - stepsToRemove)
      }
      if (beforeTasks != editorWrapper.stepsBeforeLaunch) {
        editorWrapper.replaceBeforeLaunchSteps(beforeTasks)
      }
    }
  }

  private fun checkConnection(selectedTarget: RemoteTarget) {
    sshConnectionChecker?.cancel()
    sshConnectionChecker = object : SshConnectionChecker(coroutineScope, project) {
      override suspend fun getSshConfig(): SshConfig = selectedTarget.getOrDefaultInitSshConfig(project)
      override fun updateStatusText(text: String) = statusText.set(text)
      override fun updateStatusIcon(icon: Icon?) = statusIcon.set(icon)
      override fun updateLinkText(linkText: String) = sshLinkText.set(linkText)
    }
    sshConnectionChecker?.invoke()
  }
}