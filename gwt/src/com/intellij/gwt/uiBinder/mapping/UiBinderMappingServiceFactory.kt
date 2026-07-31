package com.intellij.gwt.uiBinder.mapping

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Service(Service.Level.PROJECT)
internal class UiBinderMappingServiceFactory {
  private val instances: ConcurrentMap<Module, UiBinderMappingService> = ConcurrentHashMap()

  fun getService(module: Module): UiBinderMappingService {
    return instances.computeIfAbsent(module) {
      Disposer.register(module) {
        instances.remove(module)
      }
      UiBinderMappingServiceImpl(module)
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): UiBinderMappingServiceFactory {
      return project.service<UiBinderMappingServiceFactory>()
    }
  }
}
