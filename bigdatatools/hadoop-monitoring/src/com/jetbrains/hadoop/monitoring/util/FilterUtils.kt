package com.jetbrains.hadoop.monitoring.util

import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterAdapter
import com.jetbrains.bigdatatools.common.monitoring.data.model.FilterModel
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.AppInfo
import com.jetbrains.hadoop.monitoring.settings.HadoopSettings

fun FilterModel.loadFilters(connectionId: String) {

  val settings = HadoopSettings.getInstance()
  val config = settings.getHadoopConfigOrDefault(connectionId)

  beginBatch()

  // Limit
  FilterAdapter.updateFilter(this, AppInfo.LIMIT_FILTER, config.applicationsLimit)

  // User query
  FilterAdapter.updateFilter(this, AppInfo.USER_FILTER, config.applicationsUser)

  // States
  FilterAdapter.updateFilter(this, AppInfo.STATES_FILTER, if (settings.applicationStates.isEmpty()) null
  else settings.applicationStates.joinToString(","))

  // Started
  FilterAdapter.updateFilter(this,
                             AppInfo.STARTED_BEGIN_FILTER,
                             AppInfo.STARTED_END_FILTER,
                             config.applicationsStartedBegin,
                             config.applicationsStartedEnd)

  // Finished
  FilterAdapter.updateFilter(this,
                             AppInfo.FINISHED_BEGIN_FILTER,
                             AppInfo.FINISHED_END_FILTER,
                             config.applicationsFinishedBegin,
                             config.applicationsFinishedEnd)

  endBatch()
}