package com.jetbrains.bigdatatools.dataproc.dependend.drivers

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.dataproc.data.DataprocDataManager
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import org.com.jetbrains.bigdatatools.gcs.GcsDriver
import org.com.jetbrains.bigdatatools.gcs.settings.GCSConnectionGroup

class DataprocDriversProvider(val project: Project,
                              private val dataManager: DataprocDataManager) : Disposable {
  val gcDriver by lazy {
    createGcsDriver().also {
      Disposer.register(this, it)
    }
  }


  override fun dispose() {}


  private fun createGcsDriver(): GcsDriver {
    val dataprocConnectionData = dataManager.connectionData

    val gcsConnectionData = GCSConnectionGroup().createBlankData()
    gcsConnectionData.jsonLocation = dataprocConnectionData.jsonLocation
    gcsConnectionData.projectId = dataprocConnectionData.projectId
    gcsConnectionData.authType = dataprocConnectionData.authType
    gcsConnectionData.isBucketSourceCustom = false
    gcsConnectionData.name = DataprocMessagesBundle.message("default.gcs.connection.name")

    return GcsDriver(project, gcsConnectionData).also {
      it.initDriverUpdater()
    }
  }
}