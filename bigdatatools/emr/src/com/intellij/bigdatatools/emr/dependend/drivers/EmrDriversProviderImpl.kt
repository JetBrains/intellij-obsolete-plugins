package com.intellij.bigdatatools.emr.dependend.drivers

import com.intellij.bigdatatools.awsBase.connection.auth.AwsAuthUtil
import com.intellij.bigdatatools.emr.data.EmrDataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.rfs.driver.ActivitySource
import com.jetbrains.bigdatatools.common.rfs.driver.refreshConnectionLaunch
import com.jetbrains.bigdatatools.sftp.rfs.driver.SftpDriver
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.com.jetbrains.bigdatatools.aws.s3.S3Driver

class EmrDriversProviderImpl(override val project: Project,
                             private val dataManager: EmrDataManager,
                             private val clusterId: String?) : EmrDriversProvider, Disposable {

  private var cachedSftpDriver: SftpDriver? = null
  private var cachedS3Driver: S3Driver? = null
  val mutex = Mutex()

  override suspend fun getOrCreateSftpDriver(): SftpDriver? {
    mutex.withLock {
      cachedSftpDriver?.let { return it }

      if (clusterId == null)
        return null
      val sftpDriver = dataManager.dependsManager.createSftpDriver(project, dataManager, clusterId)
      if (sftpDriver != null) {
        Disposer.register(this, sftpDriver)
      }
      cachedSftpDriver = sftpDriver
      return sftpDriver
    }
  }

  override suspend fun getOrCreateS3Driver(): S3Driver {
    mutex.withLock {
      cachedS3Driver?.let { return it }

      val emrConnectionData = dataManager.connectionData
      val s3ConnData = dataManager.dependsManager.createS3ConnectionData("").apply {
        this.isBucketSourceCustom = false
      }
      val s3Driver = S3Driver(project = dataManager.project,
                              connectionData = s3ConnData,
                              region = emrConnectionData.region,
                              credentialsProvider = AwsAuthUtil.getPrimaryAuthentication(
                                emrConnectionData.getAwsInfo()).getCredentialsProvider())
      s3Driver.refreshConnectionLaunch(ActivitySource.EMR_DEPENDENT_USER)

      s3Driver.also {
        Disposer.register(this, it)
      }
      cachedS3Driver = s3Driver
      return s3Driver
    }
  }

  override fun dispose() {}
}