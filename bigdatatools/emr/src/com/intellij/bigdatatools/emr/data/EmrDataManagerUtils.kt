package com.intellij.bigdatatools.emr.data

import com.intellij.bigdatatools.emr.model.EmrClusterDetails
import com.intellij.bigdatatools.emr.model.EmrClusterStepInfo
import com.intellij.bigdatatools.emr.util.EmrMessagesBundle
import com.jetbrains.bigdatatools.common.monitoring.data.model.FieldsDataModel
import com.jetbrains.bigdatatools.common.rfs.util.withSlash
import software.amazon.awssdk.services.emr.model.HadoopStepConfig

object EmrDataManagerUtils {
  fun createInfoForStep(stepInfo: EmrClusterStepInfo, clusterInfo: EmrClusterDetails?): FieldsDataModel {
    val originalObject = stepInfo.originalObject
    val config: HadoopStepConfig? = originalObject.config()

    val failureDetails = originalObject.status()?.failureDetails()

    val failureMessage = failureDetails?.message()
    val failureReason = failureDetails?.reason()

    val clusterLogFolder = clusterInfo?.cluster?.logUri()
    val logFolder = clusterLogFolder?.let { it + clusterInfo.cluster.id() + "/steps/" + stepInfo.id }


    return FieldsDataModel.createForList(listOfNotNull(
      EmrMessagesBundle.message("step.info.status") to stepInfo.state.name,
      failureReason?.let { EmrMessagesBundle.message("step.info.reason") to it },
      failureMessage?.let { EmrMessagesBundle.message("step.info.details") to it },
      EmrMessagesBundle.message("step.info.jar") to (config?.jar() ?: ""),
      EmrMessagesBundle.message("step.info.class") to (config?.mainClass()?.ifBlank { EmrMessagesBundle.message("step.info.value.none") }
                                                       ?: EmrMessagesBundle.message("step.info.value.none")),
      EmrMessagesBundle.message("step.info.args") to (config?.args()?.joinToString(separator = " ") { it }
                                                        ?.ifBlank { EmrMessagesBundle.message("step.info.value.none") }
                                                      ?: EmrMessagesBundle.message("step.info.value.none")),
      logFolder?.let { EmrMessagesBundle.message("step.info.logs") to it },
      logFolder?.let { EmrMessagesBundle.message("step.info.log.controller") to it.withSlash() + "controller.gz" },
      logFolder?.let { EmrMessagesBundle.message("step.info.log.syslog") to it.withSlash() + "syslog.gz" },
      logFolder?.let { EmrMessagesBundle.message("step.info.log.stderr") to it.withSlash() + "stderr.gz" },
      logFolder?.let { EmrMessagesBundle.message("step.info.log.stdout") to it.withSlash() + "stdout.gz" },
    ))
  }
}