package com.jetbrains.bigdatatools.dataproc.data

import com.google.cloud.dataproc.v1.HadoopJob
import com.google.cloud.dataproc.v1.HiveJob
import com.google.cloud.dataproc.v1.InstanceGroupConfig
import com.google.cloud.dataproc.v1.Job
import com.google.cloud.dataproc.v1.PigJob
import com.google.cloud.dataproc.v1.PrestoJob
import com.google.cloud.dataproc.v1.PySparkJob
import com.google.cloud.dataproc.v1.SparkJob
import com.google.cloud.dataproc.v1.SparkRJob
import com.google.cloud.dataproc.v1.SparkSqlJob
import com.intellij.bigdatatools.coreUi.util.NotificationUtils
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldGroupsData
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsDataModel
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.bigdatatools.dataproc.model.DataprocClusterInfo
import com.jetbrains.bigdatatools.dataproc.model.DataprocJobInfo
import com.jetbrains.bigdatatools.dataproc.settings.DataprocConnectionData
import com.jetbrains.bigdatatools.dataproc.util.DataprocMessagesBundle
import com.jetbrains.bigdatatools.gcloud.auth.GcloudAuthType

object DataprocDataManagerUtils {
  fun createJobInfo(jobInfo: DataprocJobInfo): FieldGroupsData<DataprocJobInfo> {
    val job = jobInfo.job

    val details = job.status?.details

    val mainFields: List<Pair<String, Any?>> = listOfNotNull(
      DataprocMessagesBundle.message("job.info.jobId") to jobInfo.id,
      DataprocMessagesBundle.message("job.info.jobUuid") to job.jobUuid,
      DataprocMessagesBundle.message("job.info.status") to job.status?.state?.name,
      DataprocMessagesBundle.message("job.info.status.details") to details,
      DataprocMessagesBundle.message("job.info.start.date") to jobInfo.startTime,
      DataprocMessagesBundle.message("job.info.elapsed.time") to jobInfo.elapsedTime,
      DataprocMessagesBundle.message("job.info.cluster") to job.placement?.clusterName,
      DataprocMessagesBundle.message("job.info.type") to jobInfo.type
    ).filter { it.second?.toString()?.isNotBlank() == true }


    val jobFields = when (job.typeJobCase) {
      Job.TypeJobCase.HADOOP_JOB -> getHadoopJobFields(job.hadoopJob)
      Job.TypeJobCase.SPARK_JOB -> getSparkJobFields(job.sparkJob)
      Job.TypeJobCase.PYSPARK_JOB -> getPySparkJobFields(job.pysparkJob)
      Job.TypeJobCase.HIVE_JOB -> getHiveJobFields(job.hiveJob)
      Job.TypeJobCase.PIG_JOB -> getPigJobFields(job.pigJob)
      Job.TypeJobCase.SPARK_R_JOB -> getSparkRJobFields(job.sparkRJob)
      Job.TypeJobCase.SPARK_SQL_JOB -> getSparkSqlJobFields(job.sparkSqlJob)
      Job.TypeJobCase.PRESTO_JOB -> getPrestoJobFields(job.prestoJob)
      null, Job.TypeJobCase.TYPEJOB_NOT_SET -> emptyList()
      Job.TypeJobCase.TRINO_JOB -> emptyList()
    }.filter { it.second?.toString()?.isNotBlank() == true }

    val jobLabels = (job.labelsMap ?: emptyMap()).map { it.key to it.value }
    val groups = listOfNotNull(
      DataprocMessagesBundle.message("datamanager.summary") to FieldsDataModel.createForList(mainFields),
      DataprocMessagesBundle.message("datamanager.job.info") to FieldsDataModel.createForList(jobFields),
      DataprocMessagesBundle.message("datamanager.labels") to FieldsDataModel.createForList(jobLabels)
    )

    return FieldGroupsData(jobInfo, groups.filter { it.second.data?.isNotEmpty() == true })
  }

  private fun getHadoopJobFields(job: HadoopJob): List<Pair<String, Any?>> {
    val mainClassOrJar = job.mainClass ?: job.mainJarFileUri ?: ""
    val jars = job.jarFileUrisList?.toList()?.joinToString(separator = ", ") ?: ""
    val archives = job.archiveUrisList?.toList()?.joinToString(separator = ", ") ?: ""
    val files = job.fileUrisList?.toList()?.joinToString(separator = ", ") ?: ""
    val args = job.argsList?.toList()?.joinToString(separator = " ") ?: ""
    return listOf(
      DataprocMessagesBundle.message("job.info.spark.main.class") to mainClassOrJar,
      DataprocMessagesBundle.message("job.info.spark.jars") to jars,
      DataprocMessagesBundle.message("job.info.spark.archives") to archives,
      DataprocMessagesBundle.message("job.info.spark.files") to files,
      DataprocMessagesBundle.message("job.info.spark.args") to args,
      DataprocMessagesBundle.message("job.info.properties") to job.propertiesMap.entries.joinToString { it.key + "=" + it.value },
    )
  }

  private fun getPrestoJobFields(job: PrestoJob): List<Pair<String, Any?>> {
    val query = job.queryFileUri

    val queryFields: List<Pair<String, Any?>> = when (job.queriesCase) {
      PrestoJob.QueriesCase.QUERY_FILE_URI -> listOf(
        DataprocMessagesBundle.message("job.info.query.type") to DataprocMessagesBundle.message("job.info.query.file.value"),
        DataprocMessagesBundle.message("job.info.query.file") to job.queryFileUri
      )
      PrestoJob.QueriesCase.QUERY_LIST -> listOf(
        DataprocMessagesBundle.message("job.info.query.type") to DataprocMessagesBundle.message("job.info.query.text.value"),
        DataprocMessagesBundle.message("job.info.query.file") to (job.queryList.queriesList?.toList()?.joinToString(separator = ", "))
      )

      null, PrestoJob.QueriesCase.QUERIES_NOT_SET -> emptyList()
    }
    return listOf(
      DataprocMessagesBundle.message("job.info.query.file") to query,
      DataprocMessagesBundle.message("job.info.client.tags") to job.clientTagsList.joinToString(separator = ", "),
      DataprocMessagesBundle.message("job.info.properties") to job.propertiesMap.entries.joinToString { it.key + "=" + it.value },
      DataprocMessagesBundle.message("job.info.continue.on.failure") + ":" to job.continueOnFailure,
    ) + queryFields
  }


  private fun getPigJobFields(job: PigJob): List<Pair<String, Any?>> {
    val query = job.queryFileUri ?: job.queryFileUri ?: ""
    val jars = job.jarFileUrisList?.toList()?.joinToString(separator = ", ") ?: ""

    val queryFields: List<Pair<String, Any?>> = when (job.queriesCase) {
      PigJob.QueriesCase.QUERY_FILE_URI -> listOf(
        DataprocMessagesBundle.message("job.info.query.type") to DataprocMessagesBundle.message("job.info.query.file.value"),
        DataprocMessagesBundle.message("job.info.query.file") to job.queryFileUri
      )
      PigJob.QueriesCase.QUERY_LIST -> listOf(
        DataprocMessagesBundle.message("job.info.query.type") to DataprocMessagesBundle.message("job.info.query.text.value"),
        DataprocMessagesBundle.message("job.info.query.file") to (job.queryList.queriesList?.toList()?.joinToString(separator = ", "))
      )

      null, PigJob.QueriesCase.QUERIES_NOT_SET -> emptyList()
    }
    return listOf(
      DataprocMessagesBundle.message("job.info.query.file") to query,
      DataprocMessagesBundle.message("job.info.spark.jars") to jars,
      DataprocMessagesBundle.message("job.info.properties") to job.propertiesMap.entries.joinToString { it.key + "=" + it.value },
      DataprocMessagesBundle.message("job.info.continue.on.failure") + ":" to job.continueOnFailure,
    ) + queryFields
  }


  private fun getSparkSqlJobFields(job: SparkSqlJob): List<Pair<String, Any?>> {
    val query = job.queryFileUri ?: job.queryFileUri ?: ""
    val jars = job.jarFileUrisList?.toList()?.joinToString(separator = ", ") ?: ""

    val queryFields: List<Pair<String, Any?>> = when (job.queriesCase) {
      SparkSqlJob.QueriesCase.QUERY_FILE_URI -> listOf(
        DataprocMessagesBundle.message("job.info.query.type") to DataprocMessagesBundle.message("job.info.query.file.value"),
        DataprocMessagesBundle.message("job.info.query.file") to job.queryFileUri
      )
      SparkSqlJob.QueriesCase.QUERY_LIST -> listOf(
        DataprocMessagesBundle.message("job.info.query.type") to DataprocMessagesBundle.message("job.info.query.text.value"),
        DataprocMessagesBundle.message("job.info.query.file") to (job.queryList.queriesList?.toList()?.joinToString(separator = ", "))
      )

      null, SparkSqlJob.QueriesCase.QUERIES_NOT_SET -> emptyList()
    }
    return listOf(
      DataprocMessagesBundle.message("job.info.query.file") to query,
      DataprocMessagesBundle.message("job.info.spark.jars") to jars,
      DataprocMessagesBundle.message("job.info.properties") to job.propertiesMap.entries.joinToString { it.key + "=" + it.value },
    ) + queryFields
  }


  private fun getHiveJobFields(job: HiveJob): List<Pair<String, Any?>> {
    val query = job.queryFileUri ?: job.queryFileUri ?: ""
    val jars = job.jarFileUrisList?.toList()?.joinToString(separator = ", ") ?: ""

    val queryFields: List<Pair<String, Any?>> = when (job.queriesCase) {
      HiveJob.QueriesCase.QUERY_FILE_URI -> listOf(
        DataprocMessagesBundle.message("job.info.query.type") to DataprocMessagesBundle.message("job.info.query.file.value"),
        DataprocMessagesBundle.message("job.info.query.file") to job.queryFileUri
      )
      HiveJob.QueriesCase.QUERY_LIST -> listOf(
        DataprocMessagesBundle.message("job.info.query.type") to DataprocMessagesBundle.message("job.info.query.text.value"),
        DataprocMessagesBundle.message("job.info.query.file") to (job.queryList.queriesList?.toList()?.joinToString(separator = ", "))
      )

      null, HiveJob.QueriesCase.QUERIES_NOT_SET -> emptyList()
    }
    return listOf(
      DataprocMessagesBundle.message("job.info.query.file") to query,
      DataprocMessagesBundle.message("job.info.spark.jars") to jars,
      DataprocMessagesBundle.message("job.info.properties") to job.propertiesMap.entries.joinToString { it.key + "=" + it.value },
      DataprocMessagesBundle.message("job.info.continue.on.failure") + ":" to job.continueOnFailure
    ) + queryFields
  }


  private fun getSparkJobFields(job: SparkJob): List<Pair<String, Any?>> {
    val mainClassOrJar = job.mainClass ?: job.mainJarFileUri ?: ""
    val jars = job.jarFileUrisList?.toList()?.joinToString(separator = ", ") ?: ""
    val archives = job.archiveUrisList?.toList()?.joinToString(separator = ", ") ?: ""
    val files = job.fileUrisList?.toList()?.joinToString(separator = ", ") ?: ""
    val args = job.argsList?.toList()?.joinToString(separator = " ") ?: ""
    return listOf(
      DataprocMessagesBundle.message("job.info.spark.main.class") to mainClassOrJar,
      DataprocMessagesBundle.message("job.info.spark.jars") to jars,
      DataprocMessagesBundle.message("job.info.spark.archives") to archives,
      DataprocMessagesBundle.message("job.info.spark.files") to files,
      DataprocMessagesBundle.message("job.info.spark.args") to args,
      DataprocMessagesBundle.message("job.info.properties") to job.propertiesMap.entries.joinToString { it.key + "=" + it.value },
    )
  }

  private fun getPySparkJobFields(job: PySparkJob): List<Pair<String, Any?>> {
    val mainPyFile = job.mainPythonFileUri ?: ""
    val jars = job.jarFileUrisList?.toList()?.joinToString(separator = ", ") ?: ""
    val archives = job.archiveUrisList?.toList()?.joinToString(separator = ", ") ?: ""
    val files = job.fileUrisList?.toList()?.joinToString(separator = ", ") ?: ""
    val args = job.argsList?.toList()?.joinToString(separator = " ") ?: ""
    return listOf(
      DataprocMessagesBundle.message("job.info.spark.main.pyfile") to mainPyFile,
      DataprocMessagesBundle.message("job.info.spark.jars") to jars,
      DataprocMessagesBundle.message("job.info.spark.archives") to archives,
      DataprocMessagesBundle.message("job.info.spark.files") to files,
      DataprocMessagesBundle.message("job.info.spark.args") to args,
      DataprocMessagesBundle.message("job.info.properties") to job.propertiesMap.entries.joinToString { it.key + "=" + it.value },
    )
  }

  private fun getSparkRJobFields(job: SparkRJob): List<Pair<String, Any?>> {
    val mainPyFile = job.mainRFileUri ?: ""
    val archives = job.archiveUrisList?.toList()?.joinToString(separator = ", ") ?: ""
    val files = job.fileUrisList?.toList()?.joinToString(separator = ", ") ?: ""
    val args = job.argsList?.toList()?.joinToString(separator = " ") ?: ""
    return listOf(
      DataprocMessagesBundle.message("job.info.spark.main.r.file") to mainPyFile,
      DataprocMessagesBundle.message("job.info.spark.archives") to archives,
      DataprocMessagesBundle.message("job.info.spark.files") to files,
      DataprocMessagesBundle.message("job.info.spark.args") to args,
      DataprocMessagesBundle.message("job.info.properties") to job.propertiesMap.entries.joinToString { it.key + "=" + it.value },
    )
  }

  fun getClusterInfo(clusterInfo: DataprocClusterInfo): FieldGroupsData<DataprocClusterInfo> {
    val summaryInfo = createClusterSummaryModel(clusterInfo)
    val configInfo = createClusterConfigurationModel(clusterInfo)

    val labelsMap = clusterInfo.cluster.labelsMap.entries.map { it.key to it.value }
    return FieldGroupsData(clusterInfo, listOf(
      DataprocMessagesBundle.message("datamanager.summary") to summaryInfo,
      DataprocMessagesBundle.message("datamanager.configuration") to configInfo,
      DataprocMessagesBundle.message("datamanager.labels") to FieldsDataModel.createForList(labelsMap),
      DataprocMessagesBundle.message("datamanager.properties") to getCLusterProperties(clusterInfo),
    ).filter { it.second.data?.isEmpty() != true })
  }

  private fun getCLusterProperties(clusterInfo: DataprocClusterInfo): FieldsDataModel {
    val config = clusterInfo.cluster.config?.softwareConfig?.propertiesMap?.entries?.map { it.key to it.value } ?: emptyList()
    return FieldsDataModel.createForList(config)
  }

  private fun createClusterSummaryModel(clusterInfo: DataprocClusterInfo): FieldsDataModel {
    val originalObject = clusterInfo.cluster
    val details = originalObject.status?.detail
    val fields = listOfNotNull(
      DataprocMessagesBundle.message("cluster.info.summary.name") to originalObject.clusterName,
      DataprocMessagesBundle.message("cluster.info.summary.uiid") to originalObject.clusterUuid,
      DataprocMessagesBundle.message("cluster.info.summary.type") to "Dataproc Cluster",
      DataprocMessagesBundle.message("cluster.info.summary.state") to originalObject.status?.state?.name,
      DataprocMessagesBundle.message("cluster.info.summary.state.details") to details,
    ).filter { it.second.toString().isNotBlank() }

    return FieldsDataModel.createForList(fields)
  }

  private fun createClusterConfigurationModel(clusterInfo: DataprocClusterInfo): FieldsDataModel {
    val fields = getClusterDescriptionFields(clusterInfo).filter { it.second.toString().isNotBlank() }
    return FieldsDataModel.createForList(fields)
  }

  fun getClusterDescriptionFields(clusterInfo: DataprocClusterInfo): MutableList<Pair<String, Any?>> {
    val originalObject = clusterInfo.cluster

    val config = originalObject.config
    val gceClusterConfig = config.gceClusterConfig

    val zone = clusterInfo.zone
    val region = clusterInfo.region

    val off = DataprocMessagesBundle.message("info.value.off")


    val masterConfig = config.masterConfig
    val workerConfig = config.workerConfig

    val masterNum = masterConfig.numInstances
    val workerNum = clusterInfo.totalWorkers
    val masterDesc = when {
      masterNum == 1 && workerNum == 0 -> "Single Node (1 master, 0 workers)"
      masterNum == 1 && workerNum > 0 -> "Standard (${masterNum} master, ${workerNum} workers)"
      else -> "${masterNum} master, ${workerNum} workers"
    }


    //Common
    val fields: MutableList<Pair<String, Any?>> = mutableListOf(
      DataprocMessagesBundle.message("cluster.info.config.region") to region,
      DataprocMessagesBundle.message("cluster.info.config.zone") to zone,
      DataprocMessagesBundle.message("cluster.info.config.autoscaling") to off,
      DataprocMessagesBundle.message("cluster.info.config.metastore") to "Dataproc Metastore",
      DataprocMessagesBundle.message("cluster.info.config.scheduled.deletion") to clusterInfo.scheduledDeletion)

    //Master
    fields.add(DataprocMessagesBundle.message("cluster.info.config.master.node.desc") to masterDesc)
    fields.addAll(getFieldsForInstanceGroup(masterConfig).map { "  " + it.first to it.second })

    //Workers
    fields.add(DataprocMessagesBundle.message("cluster.info.config.worker.node.desc") to workerNum)
    fields.addAll(getFieldsForInstanceGroup(workerConfig).map { "  " + it.first to it.second })


    val shieldedInstanceConfig = gceClusterConfig.shieldedInstanceConfig
    fields.addAll(listOf(
      DataprocMessagesBundle.message("cluster.info.config.secure.boot") to shieldedInstanceConfig?.enableSecureBoot,
      DataprocMessagesBundle.message("cluster.info.config.vtpm") to shieldedInstanceConfig?.enableVtpm,
      DataprocMessagesBundle.message("cluster.info.config.monitoring") to shieldedInstanceConfig?.enableIntegrityMonitoring,
      DataprocMessagesBundle.message("cluster.info.config.network") to gceClusterConfig?.networkUri?.takeLastWhile { it != '/' },
      DataprocMessagesBundle.message("cluster.info.internal.ip") to gceClusterConfig?.internalIpOnly,
      DataprocMessagesBundle.message("cluster.info.image.version") to config.softwareConfig?.imageVersion,
      DataprocMessagesBundle.message("cluster.info.image.created") to clusterInfo.created,
      DataprocMessagesBundle.message("cluster.info.optional.components") to config.softwareConfig?.optionalComponentsList?.joinToString(
        separator = ", ") { it.name },
    ))
    return fields
  }

  private fun getFieldsForInstanceGroup(config: InstanceGroupConfig): List<Pair<String, Any?>> {
    val diskSize = config.diskConfig?.bootDiskSizeGb?.toString()?.let { it + "GB" }
    return listOf(
      DataprocMessagesBundle.message("instance.config.machineType") to config.machineTypeUri?.takeLastWhile { it != '/' },
      DataprocMessagesBundle.message("instance.config.gpu.number") to null,
      DataprocMessagesBundle.message("instance.config.primary.disk.type") to config.diskConfig?.bootDiskType,
      DataprocMessagesBundle.message("instance.config.primary.disk.size") to diskSize,
      DataprocMessagesBundle.message("instance.config.local.ssd") to config.diskConfig?.numLocalSsds,
    )
  }

  fun checkIsCliOperationsAllowed(project: Project?, connectionData: DataprocConnectionData): Boolean {
    val isCli = connectionData.authType == GcloudAuthType.ACCOUNT.id
    if (!isCli) invokeLater {
      NotificationUtils.showInfoMessage(
        project,
        DataprocMessagesBundle.message("error.json.auth.limited.msg"),
        DataprocMessagesBundle.message("error.json.auth.limited.title"),
      )
    }
    return isCli
  }
}