package com.jetbrains.spark.submit.run.common.ui

import com.intellij.bigdatatools.coreUi.settings.ConnectionSettingsListener
import com.intellij.bigdatatools.coreUi.settings.ModificationKey
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.settings.revalidateComponent
import com.intellij.bigdatatools.coreUi.ui.WHEN_FOCUS_LOST
import com.intellij.bigdatatools.coreUi.ui.block
import com.intellij.bigdatatools.coreUi.ui.components.ConnectionPropertiesEditor
import com.intellij.bigdatatools.coreUi.ui.doOnChange
import com.intellij.bigdatatools.coreUi.ui.row
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.CheckboxAction
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.ui.FixedSizeButton
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.validation.WHEN_TEXT_CHANGED
import com.intellij.openapi.ui.validation.and
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.CheckBox
import com.intellij.ui.components.DropDownLink
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RowsRange
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.and
import com.intellij.ui.layout.enteredTextSatisfies
import com.intellij.ui.layout.not
import com.intellij.ui.layout.or
import com.intellij.ui.layout.selectedValueMatches
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.applyIf
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.integration.MonitoringServiceProvider
import com.jetbrains.bigdatatools.common.rfs.ui.BdtMessages
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager
import com.jetbrains.bigdatatools.common.table.ClipboardUtils
import com.jetbrains.spark.submit.model.ClusterManagerType
import com.jetbrains.spark.submit.model.DeployModeType
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.run.common.AbstractSparkCommandLineModel
import com.jetbrains.spark.submit.run.common.AbstractSparkJobRunConfiguration
import com.jetbrains.spark.submit.run.common.SparkSubmitCommandParser
import com.jetbrains.spark.submit.run.ui.CommonSparkEditorUtil
import com.jetbrains.spark.submit.run.ui.EmptyIntegerField
import com.jetbrains.spark.submit.run.ui.MasterComboBox
import com.jetbrains.spark.submit.settings.RunConfigurationBlockType
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import com.jetbrains.spark.submit.util.SparkPropertiesUtils
import java.awt.event.ItemEvent
import java.util.EnumMap
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JTextField

abstract class SparkSubmitConfigurationEditor<SparkJobRunConfType : AbstractSparkJobRunConfiguration<*>>(val project: Project) :
  SettingsEditor<SparkJobRunConfType>() {
  private lateinit var principalRow: RowsRange
  private lateinit var keytabRow: RowsRange
  protected lateinit var archivesRow: RowsRange
  private lateinit var numExecutorsRow: RowsRange
  private lateinit var executorCoresRow: RowsRange
  private lateinit var totalExecutorCoresRow: RowsRange
  protected lateinit var queueRow: RowsRange
  protected lateinit var superviseRow: RowsRange
  private lateinit var driverCoreRow: RowsRange
  private lateinit var masterRow: RowsRange
  private lateinit var deployModeRow: RowsRange
  internal lateinit var loadSparkCommand: FixedSizeButton

  private val finalCommandComponent = RawCommandLineEditor().apply {
    textField.isEditable = false
    editorField.isEditable = false
    editorField.addExtension(ExtendableTextComponent.Extension.create(AllIcons.General.InlineCopy,
                                                                      AllIcons.General.InlineCopyHover,
                                                                      SparkMessagesBundle.message("row.final.command.copy")) {
      ClipboardUtils.setStringContent(editorField.text)
    })
  }

  protected abstract fun createTemporaryConfiguration(): SparkJobRunConfType

  protected open fun loadSparkCommand(sparkCommand: @NlsSafe String) {
    val runConfiguration = createTemporaryConfiguration()
    val parser = SparkSubmitCommandParser(sparkCommand, runConfiguration)
    parser.parse()

    val currentBlockVisibility = blocksVisibility.map { it.key to it.value.get() }.toMap()
    resetEditorFrom(runConfiguration)
    blocksVisibility.entries.forEach {
      it.value.set(currentBlockVisibility[it.key] ?: it.key.visibleByDefault)
    }
  }

  var targetDependentSettingsDefault: Boolean = true
  fun setTargetDependentSettings(body: () -> Unit) {
    if (targetDependentSettingsDefault) {
      body()
      targetDependentSettingsDefault = true
    }
  }

  val clusterManagerField = ComboBox(ClusterManagerType.entries.toTypedArray(), MINIMUM_COMBO_BOX_WIDTH).apply {
    this.toolTipText = SparkMessagesBundle.message("cluster.manager.tooltip")

    addItemListener { event ->
      if (event.stateChange == ItemEvent.SELECTED) {
        targetDependentSettingsDefault = false
        updateFieldsForManagerType()
      }
    }
    setRenderer(object : SimpleListCellRenderer<ClusterManagerType>() {
      override fun customize(list: JList<out ClusterManagerType>,
                             value: ClusterManagerType,
                             index: Int,
                             selected: Boolean,
                             hasFocus: Boolean) {
        text = value.value
      }
    })
  }

  val deployModeField = ComboBox(DeployModeType.entries.toTypedArray(), MINIMUM_COMBO_BOX_WIDTH).apply {
    addItemListener { event ->
      if (event.stateChange == ItemEvent.SELECTED) {
        targetDependentSettingsDefault = false
        updateFieldsForManagerType()
      }
    }

    renderer = object : SimpleListCellRenderer<DeployModeType>() {
      override fun customize(list: JList<out DeployModeType>,
                             value: DeployModeType,
                             index: Int,
                             selected: Boolean,
                             hasFocus: Boolean) {
        text = value.value
      }
    }

    toolTipText = SparkMessagesBundle.message("settings.deploy.mode.hint")
  }

  protected abstract val sparkHomeField: FileSelector?

  protected abstract val workDirectoryField: FileSelector?

  abstract val artifactPathField: FileSelector

  internal val classNameField = ComponentWithBrowseButton(JTextField(TEXT_FIELD_COLUMNS), null).apply {
    this.childComponent.toolTipText = SparkMessagesBundle.message("settings.application.class.hint")
    addActionListener {
      onChooseClassNameField()
    }
  }

  internal val artifactArgsField = RawCommandLineEditor().also {
    it.textField.toolTipText = SparkMessagesBundle.message("settings.application.hint")
  }
  val verboseField = CheckBox(SparkMessagesBundle.message("settings.additional.verbose"))
  internal val masterField = MasterComboBox().apply {
    toolTipText = SparkMessagesBundle.message("settings.master.hint")
    addItemListener { event ->
      if (event.stateChange == ItemEvent.SELECTED) {
        targetDependentSettingsDefault = false
      }
    }
  }

  //DependencySettings
  abstract val dependencyFields: List<FileMultiSelector>

  //Maven Settings
  internal val packagesField = RawCommandLineEditor().apply {
    textField.toolTipText = SparkMessagesBundle.message("settings.maven.packages.hint")
  }
  internal val excludePackagesField = RawCommandLineEditor().apply {
    textField.toolTipText = SparkMessagesBundle.message("settings.maven.exclude.packages.hint")
  }
  internal val repositoriesField = RawCommandLineEditor().apply {
    textField.toolTipText = SparkMessagesBundle.message("settings.maven.repositories.hint")
  }

  //Driver settings
  internal val driverMemoryField = JBTextField(SHORT_TEXT_FIELD_COLUMNS).apply {
    emptyText.text = "1024M"
    toolTipText = SparkMessagesBundle.message("settings.driver.memory.hint")
  }
  val driverJavaOptionsField = RawCommandLineEditor().apply {
    textField.toolTipText = SparkMessagesBundle.message("settings.driver.java.options.hint")
  }
  protected abstract val driverLibraryPathField: FileMultiSelector
  protected abstract val driverClassPathField: FileMultiSelector
  internal val driverCoresField = EmptyIntegerField().apply {
    emptyText.text = "1"
    toolTipText = SparkMessagesBundle.message("settings.driver.cores.hint")
  }

  //Executor settings
  internal val executorMemoryField = JBTextField(SHORT_TEXT_FIELD_COLUMNS).apply {
    emptyText.text = SparkMessagesBundle.message("settings.executor.memory.default")
    toolTipText = SparkMessagesBundle.message("settings.executor.memory.hint")
  }
  internal val totalExecutorCoresField = EmptyIntegerField().apply {
    emptyText.text = "1"
    toolTipText = SparkMessagesBundle.message("settings.executor.cores.total.hint")
  }
  internal val numExecutorsField = EmptyIntegerField().apply {
    emptyText.text = "2"
    toolTipText = SparkMessagesBundle.message("settings.executor.number.hint")
  }
  internal val executorCoresField = EmptyIntegerField().apply {
    emptyText.text = "1"
    toolTipText = SparkMessagesBundle.message("settings.executor.cores.hint")
  }
  abstract val archivesFields: List<FileMultiSelector>

  //Kerberos settings
  internal val principalField = JBTextField(TEXT_FIELD_COLUMNS).apply {
    emptyText.text = MessagesBundle.message("kerberos.settings.principal.empty")
    toolTipText = SparkMessagesBundle.message("settings.kerberos.principal.hint")
  }

  protected abstract val keytabField: FileSelector

  //Spark conf settings
  protected abstract val propertiesFileField: FileSelector

  //Other settings
  val queueField = JBTextField(TEXT_FIELD_COLUMNS).apply {
    toolTipText = SparkMessagesBundle.message("settings.cluster.manager.queue.hint")
    emptyText.text = "default"
  }
  val proxyUserField = JBTextField(TEXT_FIELD_COLUMNS).apply {
    toolTipText = SparkMessagesBundle.message("settings.cluster.manager.proxy.user.hint")
  }
  val superviseField = CheckBox(SparkMessagesBundle.message("settings.cluster.manager.supervise"))

  //Shell options
  open val isInteractiveField: JCheckBox? = createIsInteractiveField()

  private val executorLabel = JLabel(SparkMessagesBundle.message("settings.shellExecutor")).apply {
    toolTipText = SparkMessagesBundle.message("settings.shellExecutor.hint")
  }
  internal val shellExecutorField = RawCommandLineEditor().apply {
    toolTipText = SparkMessagesBundle.message("settings.shellExecutor.hint")
  }

  internal val beforeShellScriptField = JBTextField(TEXT_FIELD_COLUMNS).apply {
    toolTipText = SparkMessagesBundle.message("settings.beforeShellScript.hint")
  }

  internal val envParamsField = RawCommandLineEditor().apply {
    textField.toolTipText = SparkMessagesBundle.message("settings.envParams.hint")
  }

  //TODO: We should use comboBox with ConnectionData type
  val sparkMonitoringField = ComboBox<String>(MINIMUM_COMBO_BOX_WIDTH)

  private var foundClasses = listOf<String>()

  protected val blocks = mutableMapOf<RunConfigurationBlockType, RowsRange>()
  protected val blocksVisibility = mutableMapOf<RunConfigurationBlockType, AtomicBooleanProperty>()

  private val sparkMonitoringSettingsListener = object : ConnectionSettingsListener {
    override fun onConnectionAdded(project: Project?, newConnectionData: ConnectionData) = initSparkMonitoringComboBox(null)

    override fun onConnectionRemoved(project: Project?, removedConnectionData: ConnectionData) = initSparkMonitoringComboBox(null)

    override fun onConnectionModified(project: Project?,
                                      connectionData: ConnectionData,
                                      modified: Collection<ModificationKey>) = initSparkMonitoringComboBox(null)
  }

  init {
    RfsConnectionDataManager.instance?.addListener(sparkMonitoringSettingsListener)
  }

  override fun disposeEditor() {
    super.disposeEditor()
    RfsConnectionDataManager.instance?.removeListener(sparkMonitoringSettingsListener)
  }

  override fun resetEditorFrom(s: SparkJobRunConfType) {
    blocksVisibility.entries.forEach {
      it.value.set(s.visibleBlocks[it.key] ?: it.key.visibleByDefault)
    }
    clusterManagerField.selectedItem = s.clusterManager

    artifactPathField.path = s.artifactPath
    // TODO too much listeners ?
    artifactPathField.interactiveComponent.doOnChange {
      classNameField.revalidateComponent()
    }

    isInteractiveField?.isSelected = s.isInteractive
    shellExecutorField.text = s.shellExecutor
    beforeShellScriptField.text = s.beforeShellScript
    workDirectoryField?.path = s.workDirectoryPath
    envParamsField.text = s.envParams

    queueField.text = s.queue
    proxyUserField.text = s.proxyUser

    verboseField.isSelected = s.verbose
    superviseField.isSelected = s.supervise

    packagesField.text = s.packages
    excludePackagesField.text = s.excludePackages
    repositoriesField.text = s.repositories

    masterField.selectedItem = s.master
    classNameField.childComponent.text = s.className
    artifactArgsField.text = s.artifactArgs

    deployModeField.selectedItem = s.deployMode
    targetDependentSettingsDefault = s.clusterManagerDeployModeDefault

    driverClassPathField.files = s.driverClassPath
    driverCoresField.text = s.driverCores
    driverLibraryPathField.files = s.driverLibraryPath
    driverJavaOptionsField.text = s.driverJavaOptions
    driverMemoryField.text = s.driverMemory

    executorCoresField.text = s.executorCores
    executorMemoryField.text = s.executorMemory
    totalExecutorCoresField.text = s.totalExecutorCores
    numExecutorsField.text = s.numExecutors

    keytabField.path = s.keytab
    principalField.text = s.principal

    sparkPropertiesEditor.getComponent().setTextWithoutScroll(s.conf)
    propertiesFileField.path = s.propertiesFile

    updateFieldsForManagerType()
  }

  override fun applyEditorTo(s: SparkJobRunConfType) {
    s.visibleBlocks = EnumMap<RunConfigurationBlockType, Boolean>(RunConfigurationBlockType::class.java).also {
      blocksVisibility.forEach { (key, value) ->
        it.put(key, value.get())
      }
    }
    s.clusterManager = clusterManagerField.selectedItem as? ClusterManagerType ?: ClusterManagerType.LOCAL
    s.sparkHome = sparkHomeField?.path?.path ?: ""
    s.artifactPath = artifactPathField.path
    s.artifactArgs = artifactArgsField.text

    isInteractiveField?.let {
      s.isInteractive = it.isSelected
    }

    s.shellExecutor = shellExecutorField.text
    s.beforeShellScript = beforeShellScriptField.text
    s.workDirectoryPath = workDirectoryField?.path ?: FilePath()
    s.envParams = envParamsField.text

    s.packages = ParametersListUtil.parse(packagesField.text).joinToString(separator = ",")
    s.excludePackages = ParametersListUtil.parse(excludePackagesField.text).joinToString(separator = ",")
    s.repositories = ParametersListUtil.parse(repositoriesField.text).joinToString(separator = ",")

    s.master = masterField.selectedItem as? String ?: ""
    s.className = classNameField.childComponent.text

    s.queue = queueField.text
    s.proxyUser = proxyUserField.text

    s.verbose = verboseField.isSelected
    s.supervise = superviseField.isSelected

    s.deployMode = deployModeField.selectedItem as? DeployModeType ?: DeployModeType.CLIENT
    s.clusterManagerDeployModeDefault = targetDependentSettingsDefault

    s.driverJavaOptions = driverJavaOptionsField.text
    s.driverLibraryPath = driverLibraryPathField.files
    s.driverClassPath = driverClassPathField.files
    s.driverMemory = driverMemoryField.text
    s.driverCores = driverCoresField.text

    s.executorCores = executorCoresField.text
    s.executorMemory = executorMemoryField.text
    s.numExecutors = numExecutorsField.text
    s.totalExecutorCores = totalExecutorCoresField.text

    s.keytab = keytabField.path
    s.principal = principalField.text

    s.propertiesFile = propertiesFileField.path
    s.conf = sparkPropertiesEditor.getComponent().text

    val params = (s.state as? AbstractSparkCommandLineModel<*>)?.createCommandLineParams(isDebugMode = false) ?: emptyList()
    finalCommandComponent.text = ParametersListUtil.join(params)
  }

  internal val sparkPropertiesEditor = ConnectionPropertiesEditor(project, SparkPropertiesUtils.loadSparkProperties())
    .apply {
      getComponent().toolTipText = SparkMessagesBundle.message("settings.spark.config.hint")
    }

  protected val optionsLink = DropDownLink(SparkMessagesBundle.message("configuration.options.add")) {
    createOptionsPopup()
  }

  private fun createOptionsPopup(): JBPopup {
    val actionsGroup = DefaultActionGroup()

    blocks.keys.forEach {
      val visibility = requireNotNull(blocksVisibility[it])
      actionsGroup.add(
        object : CheckboxAction(it.title) {
          override fun isSelected(e: AnActionEvent): Boolean {
            return visibility.get()
          }
          override fun getActionUpdateThread() = ActionUpdateThread.BGT
          override fun setSelected(e: AnActionEvent, state: Boolean) {
            visibility.set(state)
            updateFieldsForManagerType()
          }
        }
      )
    }

    return JBPopupFactory.getInstance().createActionGroupPopup(SparkMessagesBundle.message("configuration.options.add.title"), actionsGroup,
                                                               DataManager.getInstance().getDataContext(optionsLink),
                                                               JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false)
  }

  protected open fun showShellExecutor(): Boolean = true

  protected open fun Panel.mainSparkSettingsRow() {
    sparkHomeRow()

    artifactRows()
    clusterManagerField(needGap = true)
    deployMainRows()
  }

  protected fun Panel.sparkHomeRow() {
    sparkHomeField?.let {
      row(SparkMessagesBundle.message("settings.spark.home"), it.component).bottomGap(BottomGap.MEDIUM)
      it.toolTipText = SparkMessagesBundle.message("sparkhome.tooltip")
    }
  }

  protected fun Panel.optionBlock(blockType: RunConfigurationBlockType, body: Panel.() -> Unit) {
    val visibility = AtomicBooleanProperty(blockType.visibleByDefault)
    val block = groupRowsRange(blockType.title) {
      body()
    }
    if (blockType == RunConfigurationBlockType.KERBEROS) {
      val visibleByClusterManager = clusterManagerField.selectedValueMatches {
        it in listOf(ClusterManagerType.YARN, ClusterManagerType.KUBERNETES)
      }
      val kerberosVisibility = visibleByClusterManager and ComponentPredicate.fromObservableProperty(visibility)
      block.visibleIf(kerberosVisibility)
    }
    else {
      block.visibleIf(visibility)
    }
    blocksVisibility[blockType] = visibility
    blocks[blockType] = block
  }

  protected fun Panel.optionBlocks() {
    optionBlock(RunConfigurationBlockType.SPARK_CONFIG) { sparkConfSettingsRow() }
    optionBlock(RunConfigurationBlockType.DEPENDENCIES) { dependencySettingsRow() }
    optionBlock(RunConfigurationBlockType.MAVEN_DEPENDENCIES) { mavenSettingsRow() }
    optionBlock(RunConfigurationBlockType.DRIVER) { driverSettingsRow() }
    optionBlock(RunConfigurationBlockType.EXECUTOR) { executorSettingsRow() }
    optionBlock(RunConfigurationBlockType.KERBEROS) { kerberosSettingsRow() }
    optionBlock(RunConfigurationBlockType.INTEGRATION) { integrationSettingsRow() }
    optionBlock(RunConfigurationBlockType.SHELL_OPTIONS) { shellOptionSettingsRow() }
    optionBlock(RunConfigurationBlockType.ADDITIONAL) { additionalSparkSettingsRow() }
  }

  protected fun Panel.clusterManagerField(needGap: Boolean = false) {
    val linkLabel = LinkLabel(null, AllIcons.General.ContextHelp,
                              LinkListener<Unit> { _, _ ->
                                BrowserUtil.browse("https://spark.apache.org/docs/latest/cluster-overview.html")
                              })
    linkLabel.toolTipText = SparkMessagesBundle.message("open.cluster.info.description")

    row(SparkMessagesBundle.message("settings.cluster.manager")) {
      cell(clusterManagerField).align(AlignX.FILL).resizableColumn()
      cell(linkLabel)
    }.applyIf(needGap) {
      topGap(TopGap.MEDIUM)
    }
  }

  protected fun Panel.sparkConfSettingsRow() {
    row(SparkMessagesBundle.message("settings.spark.config"), sparkPropertiesEditor.getComponent())

    propertiesFileField.toolTipText = SparkMessagesBundle.message("settings.spark.properties.file.hint")
    row(SparkMessagesBundle.message("settings.spark.properties.file"), propertiesFileField.component)
  }

  protected open fun Panel.dependencySettingsRow() {
    dependencyFields.forEach {
      row(it)
    }
  }

  protected fun Panel.mavenSettingsRow() {
    row(SparkMessagesBundle.message("settings.maven.packages"), packagesField)
    row(SparkMessagesBundle.message("settings.maven.exclude.packages"), excludePackagesField)
    row(SparkMessagesBundle.message("settings.maven.repositories"), repositoriesField)
  }

  protected fun Panel.driverSettingsRow() {
    row(SparkMessagesBundle.message("settings.driver.memory"), driverMemoryField)

    driverCoreRow = rowsRange {
      row(SparkMessagesBundle.message("settings.driver.cores"), driverCoresField)
    }
  }

  protected fun Panel.executorSettingsRow() {
    row(SparkMessagesBundle.message("settings.executor.memory"), executorMemoryField)
    numExecutorsRow = rowsRange {
      row(SparkMessagesBundle.message("settings.executor.number"), numExecutorsField)
    }
    executorCoresRow = rowsRange {
      row(SparkMessagesBundle.message("settings.executor.cores"), executorCoresField)
    }
    totalExecutorCoresRow = rowsRange {
      row(SparkMessagesBundle.message("settings.executor.cores.total"), totalExecutorCoresField)
    }
  }

  private fun Panel.integrationSettingsRow() {
    row(SparkMessagesBundle.message("settings.integration.spark.monitoring")) {
      cell(sparkMonitoringField).align(Align.FILL).resizableColumn()
      link(SparkMessagesBundle.message("settings.integration.spark.monitoring.add")) {
        MonitoringServiceProvider.createNewMonitoringConnection(BdtConnectionType.SPARK_MONITORING.id, project, true)
      }
    }
  }

  protected fun Panel.kerberosSettingsRow() {
    principalRow = rowsRange {
      row(SparkMessagesBundle.message("settings.kerberos.principal"), principalField)
    }

    keytabRow = rowsRange {
      keytabField.toolTipText = SparkMessagesBundle.message("settings.kerberos.keytab.hint")
      row(SparkMessagesBundle.message("settings.kerberos.keytab"), keytabField.component)
    }
  }

  @Suppress("DuplicatedCode")
  protected open fun Panel.additionalSparkSettingsRow() {
    row(SparkMessagesBundle.message("settings.cluster.manager.proxy.user"), proxyUserField)
    row(SparkMessagesBundle.message("settings.driver.java.options"), driverJavaOptionsField)

    driverLibraryPathField.toolTipText = SparkMessagesBundle.message("settings.driver.library.path.hint")
    row(driverLibraryPathField)

    driverClassPathField.toolTipText = SparkMessagesBundle.message("settings.driver.class.path.hint")
    row(driverClassPathField)

    archivesRow = rowsRange {
      archivesFields.forEach {
        it.toolTipText = SparkMessagesBundle.message("settings.executor.archives.hint")
        row(it)
      }
    }

    superviseRow = rowsRange {
      row {
        cell(superviseField)
        comment(SparkMessagesBundle.message("settings.cluster.manager.supervise.hint"))
      }
    }

    queueRow = rowsRange {
      row(SparkMessagesBundle.message("settings.cluster.manager.queue"), queueField)
    }

    block(verboseField)
  }

  protected open fun Panel.shellOptionSettingsRow() {
    if (showShellExecutor()) {
      row(executorLabel.text, shellExecutorField)
      isInteractiveField?.let {
        row(it)
      }
      row(SparkMessagesBundle.message("settings.beforeShellScript"), beforeShellScriptField)

      workDirectoryField?.let {
        row(SparkMessagesBundle.message("settings.workingDirectory"), it.component)
        it.toolTipText = SparkMessagesBundle.message("work.directory.tooltip")
      }

      row(SparkMessagesBundle.message("settings.envParams"), envParamsField)
    }
  }

  protected fun Panel.finalCommandRow() {
    loadSparkCommand = FixedSizeButton(finalCommandComponent)
    loadSparkCommand.icon = AllIcons.Actions.MenuOpen
    loadSparkCommand.toolTipText = SparkMessagesBundle.message("load.command.string")
    loadSparkCommand.addActionListener {
      BdtMessages.showInputDialogWithDescription(project,
                                                 SparkMessagesBundle.message("dialog.input.spark.command.title"),
                                                 SparkMessagesBundle.message("dialog.input.spark.command.label"),
                                                 SparkMessagesBundle.message("dialog.input.spark.command.description"), "", null)?.let {
        loadSparkCommand(it)
      }
    }

    groupRowsRange(SparkMessagesBundle.message("row.final.command")) {
      row {
        cell(finalCommandComponent).align(Align.FILL).resizableColumn()
        cell(loadSparkCommand)
        contextHelp(SparkMessagesBundle.message("row.final.command.hint"))
      }
    }
  }

  @Suppress("HardCodedStringLiteral")
  protected open fun updateFieldsForManagerType() {
    val clusterManagerType = clusterManagerField.selectedItem as ClusterManagerType
    val deployModeType = deployModeField.selectedItem as DeployModeType

    queueRow.visible(clusterManagerType == ClusterManagerType.YARN)
    numExecutorsRow.visible(clusterManagerType in listOf(ClusterManagerType.YARN, ClusterManagerType.KUBERNETES))

    executorCoresRow.visible(clusterManagerType in listOf(ClusterManagerType.YARN, ClusterManagerType.STANDALONE,
                                                          ClusterManagerType.KUBERNETES))
    totalExecutorCoresRow.visible(clusterManagerType in listOf(ClusterManagerType.MESOS, ClusterManagerType.STANDALONE,
                                                               ClusterManagerType.KUBERNETES))
    superviseRow.visible(clusterManagerType in listOf(ClusterManagerType.MESOS, ClusterManagerType.STANDALONE) &&
                         deployModeType == DeployModeType.CLUSTER)
    driverCoreRow.visible(clusterManagerType != ClusterManagerType.LOCAL && deployModeType == DeployModeType.CLUSTER)
    masterRow.visible(clusterManagerType.masterFixed == null)
    deployModeRow.visible(clusterManagerType != ClusterManagerType.LOCAL)

    val masterText = masterField.text
    val currentTargetDependentSettingsDefault = targetDependentSettingsDefault
    masterField.removeAllItems()
    clusterManagerType.masterOptions.forEach {
      masterField.addItem(it)
    }
    masterField.text = masterText
    targetDependentSettingsDefault = currentTargetDependentSettingsDefault
  }

  protected abstract fun resolveArtifact(indicator: ProgressIndicator): String?

  protected fun createIsInteractiveField(): JCheckBox = CheckBox(SparkMessagesBundle.message("settings.isInteractive")).apply {
    toolTipText = SparkMessagesBundle.message("settings.isInteractive.hint")
  }

  protected fun Panel.deployMainRows() {
    masterRow = rowsRange {
      row(SparkMessagesBundle.message("settings.master"), masterField)
    }

    deployModeRow = rowsRange {
      row(SparkMessagesBundle.message("settings.deploy.mode"), deployModeField)
    }
  }

  private val isPySparkDefault
    get() = artifactPathField.interactiveComponent.enteredTextSatisfies {
      it.endsWith(".py") || it.endsWith(".zip")
    }

  protected fun Panel.artifactRows(needGap: Boolean = false, isPySpark: ComponentPredicate = isPySparkDefault) {
    row(SparkMessagesBundle.message("settings.application")) {
      cell(artifactPathField.component)
        .align(AlignX.FILL)
        .validationRequestor(WHEN_TEXT_CHANGED(artifactPathField.interactiveComponent) and WHEN_FOCUS_LOST(artifactPathField.component))
        .validationOnInput {
          if (artifactPathField.path.isBlank()) {
            error(SparkMessagesBundle.message("dialog.message.specify.application"))
          }
          else null
        }
    }.topGap(TopGap.MEDIUM)
    artifactPathField.toolTipText = SparkMessagesBundle.message("artifact.tooltip")

    row(SparkMessagesBundle.message("settings.application.class.name"), classNameField).visibleIf(
      !isPySpark or classNameField.childComponent.enteredTextSatisfies { it.isNotEmpty() })
    row(SparkMessagesBundle.message("settings.application.arguments"), artifactArgsField).applyIf(needGap) {
      bottomGap(BottomGap.MEDIUM)
    }
  }

  protected fun sparkMonitoringId(): String {
    val selectedName = sparkMonitoringField.selectedItem as? String ?: ""
    val connection = RfsConnectionDataManager.instance
      ?.getConnectionsByGroupId(BdtConnectionType.SPARK_MONITORING.id, project)
      ?.firstOrNull { it.name == selectedName }
    return connection?.innerId ?: ""
  }

  private fun onChooseClassNameField() {
    if (artifactPathField.path.path.isBlank()) {
      Messages.showErrorDialog(project,
                               SparkMessagesBundle.message("settings.application.class.name.error.msg"),
                               SparkMessagesBundle.message("settings.application.class.name.error.title"))
    }
    else {
      refreshFoundClasses()
      val selected = classNameField.childComponent.text
      val newName = CommonSparkEditorUtil.selectClassName(project, foundClasses, selected) ?: return
      classNameField.childComponent.text = newName
    }
  }

  private fun refreshFoundClasses() {
    foundClasses = CommonSparkEditorUtil.getFoundClasses(project, artifactPathField.path, ::resolveArtifact)
  }

  protected fun initSparkMonitoringComboBox(connectionId: String?) {
    val connections = RfsConnectionDataManager.instance?.getConnectionsByGroupId(BdtConnectionType.SPARK_MONITORING.id, project)
                      ?: emptyList()
    val connectionsNames = connections.map { it.name }
    sparkMonitoringField.removeAllItems()
    connectionsNames.forEach {
      @Suppress("HardCodedStringLiteral") // connectionsNames is user defined.
      sparkMonitoringField.addItem(it)
    }

    if (connectionId.isNullOrBlank()) return
    val selectedConnection = connections.firstOrNull { it.innerId == connectionId } ?: return
    sparkMonitoringField.selectedItem = selectedConnection.name
  }

  companion object {
    const val SHORT_TEXT_FIELD_COLUMNS = 6
    const val TEXT_FIELD_COLUMNS = 15
    val MINIMUM_COMBO_BOX_WIDTH = JBUIScale.scale(195)
  }
}