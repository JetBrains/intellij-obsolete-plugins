package com.intellij.bigdatatools.notebooks.core.api.executor

import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager

object NoteExecutorUtils {
  fun getSupportedDrivers(project: Project?): List<NoteExecutable> = DriverManager.getDrivers(project).filterIsInstance<NoteExecutable>()
  fun getDriver(project: Project?, configId: String): NoteExecutable? = getSupportedDrivers(project).find { it.getExternalId() == configId }
}