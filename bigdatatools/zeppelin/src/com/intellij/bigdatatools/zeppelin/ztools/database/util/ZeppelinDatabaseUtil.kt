package com.intellij.bigdatatools.zeppelin.ztools.database.util

import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriverManager
import com.intellij.database.dataSource.DatabaseDriverManager
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.jetbrains.bigdatatools.common.updater.BDTPluginUtil

object ZeppelinDatabaseUtil {
  //We do not use special ztools drivers, so we remove just created in old versions of the plugins
  fun clearObsoleteDataSourcesAndDriver(project: Project) = StartupManager.getInstance(project).runAfterOpened {
    if (!BDTPluginUtil.isDatabaseEnabled())
      return@runAfterOpened

    invokeAndWaitIfNeeded {
      val dataSourceManager = LocalDataSourceManager.getInstance(project)
      val zepDataSources = dataSourceManager.dataSources.filter { it.url?.startsWith("Zeppelin") ?: false }
      val currentIds = ZeppelinDriverManager.getDrivers(project).map { it.connectionData.innerId }
      val obsoleteDataSources = zepDataSources.filter { it.url !in currentIds }
      obsoleteDataSources.forEach {
        dataSourceManager.removeDataSource(it)
      }

      val instance = DatabaseDriverManager.getInstance()

      val drivers = instance.drivers.filter { it.name == "Apache Zeppelin" }
      drivers.forEach {
        instance.removeDriver(it)
      }
    }
  }
}