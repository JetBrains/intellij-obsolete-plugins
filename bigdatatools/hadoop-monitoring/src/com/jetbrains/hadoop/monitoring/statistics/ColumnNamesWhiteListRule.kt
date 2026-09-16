package com.jetbrains.hadoop.monitoring.statistics

import com.intellij.internal.statistic.eventLog.validator.ValidationResultType
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomValidationRule
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppAttemptInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppPriority
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ClusterInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ClusterMetricsInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.FinalApplicationStatus
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.LogAggregationStatus
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.NodeInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.NodeState
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.ResourceRequestInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.Service
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.StatisticsItemInfo
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.YarnApplicationState
import kotlin.reflect.full.declaredMemberProperties

open class ColumnNamesWhiteListRule : CustomValidationRule() {

  private val acceptableColumns = HashSet<String>()

  init {

    val classes = arrayOf(AppAttemptInfo::class,
                          AppInfo::class,
                          YarnApplicationState::class,
                          ClusterInfo::class,
                          ClusterMetricsInfo::class,
                          Service.STATE::class,
                          FinalApplicationStatus::class,
                          LogAggregationStatus::class,
                          NodeInfo::class,
                          NodeState::class,
                          AppPriority::class,
                          ResourceRequestInfo::class,
                          StatisticsItemInfo::class)

    classes.forEach { cls -> acceptableColumns.addAll(cls.declaredMemberProperties.map { it.name }) }
  }

  final override fun acceptRuleId(ruleId: String?): Boolean = "hadoop_monitoring_column_name" == ruleId

  final override fun doValidate(data: String, context: EventContext): ValidationResultType {
    return if (acceptableColumns.contains(data)) ValidationResultType.ACCEPTED else ValidationResultType.REJECTED
  }
}