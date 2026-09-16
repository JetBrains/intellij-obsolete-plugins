package com.jetbrains.bigdatatools.hivemetastore.settings

import com.intellij.bigdatatools.coreUi.fields.BrowseTextField
import com.intellij.bigdatatools.coreUi.fields.CheckBoxField
import com.intellij.bigdatatools.coreUi.fields.PropertiesFieldComponent
import com.intellij.bigdatatools.coreUi.fields.StringNamedField
import com.intellij.bigdatatools.coreUi.fields.WrappedComponent
import com.intellij.bigdatatools.coreUi.settings.CommonSettingsKeys
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.settings.withNotEmptyValidator
import com.intellij.bigdatatools.coreUi.settings.withValidator
import com.intellij.bigdatatools.coreUi.ui.block
import com.intellij.bigdatatools.coreUi.ui.nameRow
import com.intellij.bigdatatools.coreUi.ui.row
import com.intellij.bigdatatools.coreUi.ui.shortRow
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.ContextHelpLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.RowsRange
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import com.jetbrains.bigdatatools.common.monitoring.TunnelableSettingsCustomizer
import com.jetbrains.bigdatatools.common.settings.fields.RadioGroupField
import com.jetbrains.bigdatatools.common.settings.kerberos.KerberosSettingsDialog
import com.jetbrains.bigdatatools.hivemetastore.statistic.HiveMetastoreSettingsCollector
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMessagesBundle
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMetastorePropertiesUtils
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMetastoreUtils
import org.apache.hadoop.hive.metastore.conf.MetastoreConf.ConfVars
import org.com.jetbrains.bigdatatools.hdfs.settings.HdfsJavaSettingsCustomizer
import org.com.jetbrains.bigdatatools.hdfs.settings.HdfsSettingsConst
import org.com.jetbrains.bigdatatools.utils.HdfsMessagesBundle
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JRadioButton

class HiveMetastoreSettingsCustomizer(project: Project,
                                      connectionData: HiveMetastoreConnectionData,
                                      uiDisposable: Disposable) : TunnelableSettingsCustomizer<HiveMetastoreConnectionData>(
  connectionData,
  project,
  uiDisposable) {

  override val url = StringNamedField(ConnectionData::uri, CommonSettingsKeys.URL_KEY, connectionData).also {
    it.emptyText = HiveMessagesBundle.message("settings.uri.emptyText")
    it.getTextComponent().toolTipText = HiveMessagesBundle.message("settings.uri.emptyText")
  }.withValidator(uiDisposable) {
    HiveMetastoreUtils.validateHiveUrls(it)
  } as StringNamedField

  internal val sourceTypeChooser = RadioGroupField(HiveMetastoreConnectionData::propertySource,
                                                  HiveMetastoreSettingsIds.PROPERTIES_SOURCE_KEY, connectionData,
                                                   HiveMetastorePropertySource.entries)

  internal val configFolder = BrowseTextField(
    prop = HiveMetastoreConnectionData::configFolderPath,
    HiveMetastoreSettingsIds.PROPERTIES_FILE_KEY,
    FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle(HiveMessagesBundle.message("settings.properties.file.browse")),
    initSettings = connectionData
  ).also {
    it.emptyText = HiveMessagesBundle.message("settings.configFolder.emptyText")
    it.getTextComponent().toolTipText = HiveMessagesBundle.message("settings.configFolder.hint")
  }

  internal val propertiesEditor = PropertiesFieldComponent(project,
                                                           HiveMetastorePropertiesUtils.getConnectionProperties(),
                                                           HiveMetastoreConnectionData::properties,
                                                           HiveMetastoreSettingsIds.PROPERTIES_KEY, connectionData, uiDisposable)

  internal val databasePatternField = StringNamedField(HiveMetastoreConnectionData::databasePattern,
                                                      HiveMetastoreSettingsIds.DATABASE_PATTERN, connectionData).withNotEmptyValidator(
    uiDisposable)

  internal val tablePatternField = StringNamedField(HiveMetastoreConnectionData::tablePattern, HiveMetastoreSettingsIds.TABLE_PATTERN,
                                                   connectionData).withNotEmptyValidator(uiDisposable)

  internal val useKerberosTicketCache = CheckBoxField(HiveMetastoreConnectionData::useKerberosTicketCache, HdfsSettingsConst.KINIT,
                                                     connectionData).apply {
    getComponent().addChangeListener {
      updateVisibilityOfKerberosAndKinit()
    }
  }

  private var propertiesPanel = panel {
    collapsibleGroup(MessagesBundle.message("settings.connection.header")) {
      row(propertiesEditor.labelComponent)
      block(propertiesEditor.getComponent())
    }
  }


  internal lateinit var saslPrincipal: Cell<JBTextField>
  internal lateinit var saslKeytab: Cell<TextFieldWithBrowseButton>
  private lateinit var kinitGroup: RowsRange
  private lateinit var kerberosRows: RowsRange

  private lateinit var implicitRows: RowsRange
  private lateinit var authGroup: RowsRange
  private lateinit var kerberosGroup: RowsRange
  private lateinit var sourceFolderRows: Row

  private var kerberosEnabled = false

  internal lateinit var noneAuthType: Cell<JRadioButton>
  internal lateinit var kerberosAuthType: Cell<JRadioButton>
  init {
    sourceTypeChooser.addItemListener {
      updateVisibilitySourceConfig()
    }
  }

  override fun getDefaultFields(): List<WrappedComponent<in HiveMetastoreConnectionData>> = listOf(nameField, url,
                                                                                                   sourceTypeChooser, configFolder,
                                                                                                   databasePatternField, tablePatternField,
                                                                                                   useKerberosTicketCache,
                                                                                                   tunnelField)

  override fun getAdditionalFields() = listOf(propertiesEditor)


  override fun getAdditionalComponent(conn: HiveMetastoreConnectionData) = propertiesPanel

  @Suppress("DuplicatedCode")
  override fun getDefaultComponent(fields: List<WrappedComponent<in HiveMetastoreConnectionData>>,
                                   conn: HiveMetastoreConnectionData) = panel {
    nameRow(nameField)

    shortRow(sourceTypeChooser)

    indent {
      implicitRows = rowsRange {
        row(url)
      }

      sourceFolderRows = row(configFolder)

      authGroup = rowsRange {
        buttonsGroup {
          row(HdfsMessagesBundle.message("settings.kerberos.auth")) {
            noneAuthType = radioButton(HdfsMessagesBundle.message("settings.kerberos.auth.none"), false)
              .onChanged { kerberosEnabled = !it.isSelected; updateVisibilityOfKerberos() }
            kerberosAuthType = radioButton(HdfsMessagesBundle.message("settings.kerberos.auth.kerberos"), true)
          }
        }.bind(::kerberosEnabled)


        kerberosGroup = indent {
          kinitGroup = rowsRange {
            row {
              cell(useKerberosTicketCache.getComponent()).gap(RightGap.SMALL)
              cell(ContextHelpLabel.create(MessagesBundle.message("kerberos.settings.use.ticket.cache.tooltip")))
            }
          }

          kerberosRows = rowsRange {
            indent {
              row(MessagesBundle.message("kerberos.settings.principal.label")) {
                saslPrincipal = textField().onChanged {
                  updateUiPropertiesField()
                }.align(AlignX.FILL).apply {
                  component.emptyText.text = MessagesBundle.message("kerberos.settings.principal.empty")
                }
              }
              row(MessagesBundle.message("kerberos.connection.settings.keytab.label")) {
                saslKeytab = textFieldWithBrowseButton(browseDialogTitle = MessagesBundle.message("kerberos.connection.settings.keytab.select.dialog.title"), project).onChanged {
                  updateUiPropertiesField()
                }.align(AlignX.FILL)
              }
            }
          }

          row {
            link(MessagesBundle.message("kerberos.settings.open.button")) {
              KerberosSettingsDialog(project).showAndGet()
            }
          }
        }
      }
    }

    updateUiFromProperties()
    updateVisibilitySourceConfig()

    block(tunnelField.getComponent()).topGap(TopGap.MEDIUM)
    val filtersPanel = createFiltersPanel().apply {
      addComponentListener(object : ComponentAdapter() {
        override fun componentShown(e: ComponentEvent?) = setFiltersShown(true)
        override fun componentHidden(e: ComponentEvent?) = setFiltersShown(false)
      })
    }

    collapsibleGroup(HiveMessagesBundle.message("settings.filters.header"), isFiltersShown()) {
      block(filtersPanel)
    }

    HiveMetastoreSettingsCollector.Util.getInstance().initPanel(this@HiveMetastoreSettingsCustomizer)
  }

  private fun createFiltersPanel() = panel {
    row(databasePatternField)
    row(tablePatternField)
  }

  private fun isFiltersShown(): Boolean = PropertiesComponent.getInstance().getBoolean(FILTERS_SHOW_ID, false)

  private fun setFiltersShown(value: Boolean) {
    if (!value && !isFiltersShown()) {
      return
    }
    PropertiesComponent.getInstance().setValue(FILTERS_SHOW_ID, value)
  }

  private fun updateVisibilityOfKerberosAndKinit() {
    kerberosGroup.visible(kerberosEnabled)
    kerberosRows.visible(kerberosEnabled && !useKerberosTicketCache.getValue())
  }

  private fun updateVisibilityOfKerberos() {
    updateVisibilityOfKerberosAndKinit()
    updateUiPropertiesField()
  }

  private fun updateVisibilitySourceConfig() {
    val authType = sourceTypeChooser.getValue()
    implicitRows.visible(authType == HiveMetastorePropertySource.DIRECT)
    propertiesPanel.isVisible = authType == HiveMetastorePropertySource.DIRECT
    sourceFolderRows.visible(authType == HiveMetastorePropertySource.FILE)
    authGroup.visible(authType == HiveMetastorePropertySource.DIRECT)

    if (authType == HiveMetastorePropertySource.DIRECT) {
      updateVisibilityOfKerberos()
    }
  }

  private fun updateUiPropertiesField() {
    val result = mutableMapOf<String, String?>()
    if (kerberosEnabled) {
      result[HdfsJavaSettingsCustomizer.HADOOP_AUTH_CONF] = HdfsJavaSettingsCustomizer.HADOOP_AUTH_KERBEROS_KEY
      result[ConfVars.USE_THRIFT_SASL.hiveName] = "true"
      result[ConfVars.KERBEROS_PRINCIPAL.hiveName] = saslPrincipal.component.text
      result[ConfVars.KERBEROS_KEYTAB_FILE.hiveName] = saslKeytab.component.text
    }
    else {
      result[HdfsJavaSettingsCustomizer.HADOOP_AUTH_CONF] = null
      result[ConfVars.USE_THRIFT_SASL.hiveName] = null
      result[ConfVars.KERBEROS_PRINCIPAL.hiveName] = null
      result[ConfVars.KERBEROS_KEYTAB_FILE.hiveName] = null
    }
    val uiProps = result.entries.associate { it.key to it.value }
    propertiesEditor.mergeConfig(uiProps)
  }

  private fun updateUiFromProperties() {
    val properties = propertiesEditor.getProperties() ?: emptyMap()
    val isKerberosSetup = properties[HdfsJavaSettingsCustomizer.HADOOP_AUTH_CONF] == HdfsJavaSettingsCustomizer.HADOOP_AUTH_KERBEROS_KEY
    kerberosEnabled = isKerberosSetup
    saslPrincipal.text(properties[ConfVars.KERBEROS_PRINCIPAL.hiveName] ?: "")
    saslKeytab.text(properties[ConfVars.KERBEROS_KEYTAB_FILE.hiveName] ?: "")
  }

  companion object {
    private const val FILTERS_SHOW_ID = "com.jetbrains.bigdatatools.hivemetastore.settings.filters"
  }
}
