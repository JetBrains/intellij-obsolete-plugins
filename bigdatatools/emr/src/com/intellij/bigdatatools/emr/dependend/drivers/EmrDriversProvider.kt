package com.intellij.bigdatatools.emr.dependend.drivers

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.sftp.rfs.driver.SftpDriver
import org.com.jetbrains.bigdatatools.aws.s3.S3Driver

interface EmrDriversProvider {
  val project: Project
  suspend fun getOrCreateSftpDriver(): SftpDriver?
  suspend fun getOrCreateS3Driver(): S3Driver
}