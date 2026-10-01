package com.intellij.dbt

import com.intellij.dbt.DbtUtils.Companion.isUnderIgnoredDirectories
import com.intellij.dbt.detection.DbtService
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import kotlinx.coroutines.launch

class DbtFileListener : AsyncFileListener {
  override fun prepareChange(events: MutableList<out VFileEvent>): AsyncFileListener.ChangeApplier? {
    for (event in events) {
      ProgressManager.checkCanceled()
      if (event.fileSystem !is LocalFileSystem) continue
      val fileName: String
      val directory: VirtualFile
      if (event is VFileCreateEvent) {
        fileName = event.childName
        directory = event.parent
      } else if (event is VFileDeleteEvent) {
        fileName = event.file.name
        directory = event.file.parent
      } else {
        continue
      }

      if (fileName == "dbt_project.yml") {
        for (project in ProjectManager.getInstance().getOpenProjects()) {
          ProgressManager.checkCanceled()
          val module = ModuleUtilCore.findModuleForFile(directory, project) ?: continue
          if (isUnderIgnoredDirectories(directory, module)) {
            continue
          }
          val dbtService = DbtService.getInstance(project)
          @Suppress("KotlinConstantConditions")
          if (event is VFileCreateEvent) {
            dbtService.coroutineScope.launch {
              dbtService.initializeDbtSupport(module, directory)
            }
          } else if (event is VFileDeleteEvent) {
            dbtService.coroutineScope.launch {
              dbtService.removeDbtSupport(module)
            }
          }
        }
      }
    }
    return null
  }
}