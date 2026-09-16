package com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.inlay

import com.intellij.bigdatatools.coreUi.settings.ConnectionSettingsListener
import com.intellij.bigdatatools.coreUi.settings.ModificationKey
import com.intellij.bigdatatools.coreUi.settings.connections.ConnectionData
import com.intellij.bigdatatools.coreUi.util.MessagesBundle.message
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.ParseUtil.parseMeta
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.ParseUtil.schemaToDdlString
import com.intellij.bigdatatools.plugin.spark.assistance.util.SAMessagesBundle
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Segment
import com.intellij.openapi.util.TextRange
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.util.progress.withProgressText
import com.intellij.psi.SmartPsiElementPointer
import com.jetbrains.bigdatatools.common.constants.BdtConnectionType
import com.jetbrains.bigdatatools.common.constants.BdtPluginType
import com.jetbrains.bigdatatools.common.rfs.client.SchemaInfoPart
import com.jetbrains.bigdatatools.common.rfs.driver.Driver
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager.Companion.getDrivers
import com.jetbrains.bigdatatools.common.rfs.ui.RfsChooserDescriptor
import com.jetbrains.bigdatatools.common.rfs.ui.RfsFileChooser
import com.jetbrains.bigdatatools.common.settings.connections.FileSystemConnectionGroup
import com.jetbrains.bigdatatools.common.settings.connections.StorageConnectionGroup
import com.jetbrains.bigdatatools.common.settings.manager.RfsConnectionDataManager

class SchemaSampleFileData(val anchorElement: SmartPsiElementPointer<*>?,
                           val anchorFileUrl: String,
                           val anchorRange: Segment,
                           val driverId: String,
                           val path: String,
                           val externalPath: String,
                           val schemaDdlString: String) {
  fun copy(anchorElement: SmartPsiElementPointer<*>?): SchemaSampleFileData {
    return SchemaSampleFileData(anchorElement, anchorFileUrl, anchorElement?.range ?: anchorRange, driverId, path, externalPath, schemaDdlString)
  }
}

@State(name = "DataframeSampleFileService", storages = [(Storage(StoragePathMacros.WORKSPACE_FILE))])
@Service(Service.Level.PROJECT)
class DataframeSampleFileService(val project: Project) : PersistentStateComponent<DataframeSampleFileService.MyState> {

  override fun getState(): MyState {
    return MyState(attached.mapValues { allInlaysForFile ->
      val filterOurOutdated = allInlaysForFile.value.any { it.anchorElement != null }
      allInlaysForFile.value.filter {
        !filterOurOutdated || it.anchorElement != null
      }.map {
        val textRange = it.anchorElement?.range ?: it.anchorRange
        SchemaSampleFileDataState(anchorRangeFrom = textRange.startOffset,
                                  anchorRangeTo = textRange.endOffset,
                                  driverId = it.driverId,
                                  path = it.path,
                                  externalPath = it.externalPath,
                                  schemaDdlString = it.schemaDdlString)
      }
    })
  }

  override fun loadState(state: MyState) {
    attached.clear()
    attached.putAll(state.attached.mapValues { l ->
      l.value.mapNotNull {
        SchemaSampleFileData(anchorElement = null,
                             anchorFileUrl = l.key,
                             anchorRange = TextRange.create(it.anchorRangeFrom ?: return@mapNotNull null,
                                                            it.anchorRangeTo ?: return@mapNotNull null),
                             driverId = it.driverId ?: return@mapNotNull null,
                             path = it.path ?: return@mapNotNull null,
                             externalPath = it.externalPath ?: return@mapNotNull null,
                             schemaDdlString = it.schemaDdlString ?: return@mapNotNull null)
      }
    })
  }

  class MyState(var attached: Map<String, List<SchemaSampleFileDataState>> = emptyMap())
  class SchemaSampleFileDataState(var anchorRangeFrom: Int? = null,
                                  var anchorRangeTo: Int? = null,
                                  var driverId: String? = null,
                                  var path: String? = null,
                                  var externalPath: String? = null,
                                  var schema: List<SchemaInfoPart>? = null,
                                  var schemaDdlString: String? = null)


  private val attached = mutableMapOf<String, List<SchemaSampleFileData>>()

  private fun getSuitableDrivers(project: Project): List<Driver> {
    return getDrivers(project).filter {
      val connType = BdtConnectionType.getForId(it.connectionData.groupId)
      connType?.pluginType == BdtPluginType.RFS || connType?.pluginType == BdtPluginType.METASTORE_CORE
    }
  }

  private fun selectSampleFile(key: SmartPsiElementPointer<*>): FileInfo? {
    // Drivers list should be updated on connection settings update.
    val drivers: MutableList<Driver> = getSuitableDrivers(project).toMutableList()

    val groups = listOf(FileSystemConnectionGroup(), StorageConnectionGroup())

    val chooser = RfsFileChooser(mainTitle = message("file.chooser.source.file.selector.title"),
                                 project = project,
                                 descriptor = RfsChooserDescriptor(),
                                 preselectedDriver = null,
                                 preselectedPath = "",
                                 drivers = drivers,
                                 groups = groups)

    val connectionSettingsListener = object : ConnectionSettingsListener {
      override fun onConnectionAdded(project: Project?, newConnectionData: ConnectionData) = updateDrivers()
      override fun onConnectionRemoved(project: Project?, removedConnectionData: ConnectionData) = updateDrivers()
      override fun onConnectionModified(project: Project?,
                                        connectionData: ConnectionData,
                                        modified: Collection<ModificationKey>) = updateDrivers()

      fun updateDrivers() {
        drivers.clear()
        drivers.addAll(getSuitableDrivers(project))
        chooser.updateRoots()
      }
    }

    RfsConnectionDataManager.instance?.addListener(connectionSettingsListener)

    Disposer.register(chooser.disposable,
                      Disposable { RfsConnectionDataManager.instance?.removeListener(connectionSettingsListener) })

    val fileInfo = chooser.showAndGetResult()?.firstOrNull() ?: return null
    return fileInfo
  }

  fun getData(key: SmartPsiElementPointer<*>, fileInfo: FileInfo): SchemaSampleFileData? {
    val metaInfo = runWithModalProgressBlocking(project, SAMessagesBundle.message("progress.title.downloading.file.to.read.schema")) {
      withProgressText(SAMessagesBundle.message("progress.text.downloading.file.to.read.schema", fileInfo.name)) {
        fileInfo.driver.fileInfoManager.getMetaFileInfo(fileInfo, project).result
      }
    } ?: return null
    val range = key.range ?: return null
    val schema = parseMeta(metaInfo) ?: return null
    return SchemaSampleFileData(
      anchorElement = key,
      anchorFileUrl = key.virtualFile.url,
      anchorRange = range,
      driverId = fileInfo.driver.getExternalId(),
      path = fileInfo.path.stringRepresentation(),
      externalPath = fileInfo.externalPath,
      schemaDdlString = schemaToDdlString(schema)
    )
  }

  fun selectAndAttach(key: SmartPsiElementPointer<*>) {
    val fileInfo = selectSampleFile(key) ?: return
    val data = getData(key, fileInfo) ?: return
    attach(key, data)
  }

  fun attach(key: SmartPsiElementPointer<*>, data: SchemaSampleFileData) {
    // to ensure that key is attached
    getAttached(key)
    val containingFile = key.containingFile ?: return
    attached.compute(data.anchorFileUrl) { _, existingInlaysForFile ->
      existingInlaysForFile.orEmpty().filter { it.anchorElement != key }.plus(data)
    }
    DaemonCodeAnalyzer.getInstance(project).restart(containingFile, this)
  }

  fun getAttached(key: SmartPsiElementPointer<*>): SchemaSampleFileData? {
    val keyRange = key.range ?: return null
    val results = attached[key.virtualFile.url]?.filter {
      it.anchorElement == key || TextRange.create(it.anchorRange).intersects(keyRange)
    }.orEmpty().toMutableList()
    return attached.compute(key.virtualFile.url) { _, allInlaysForFile ->
      allInlaysForFile?.map { existingInlay ->
        val i = results.indexOf(existingInlay)
        if (i > 0) {
          existingInlay.copy(anchorElement = key).also {
            results[i] = it
          }
        } else existingInlay
      }
    }?.firstOrNull()
  }

  companion object {
    val SUPPORTED_READ_METHODS = setOf("csv", "parquet", "orc", /*, "json"*/ "load")
  }
}