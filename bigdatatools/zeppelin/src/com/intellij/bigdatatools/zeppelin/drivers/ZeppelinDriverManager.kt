package com.intellij.bigdatatools.zeppelin.drivers

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager

object ZeppelinDriverManager {
  fun getDriver(project: Project?, configId: String) = DriverManager.getDriverById(project, configId) as? ZeppelinDriver
  fun getDrivers(project: Project?) = DriverManager.getDrivers(project).filterIsInstance<ZeppelinDriver>()
}