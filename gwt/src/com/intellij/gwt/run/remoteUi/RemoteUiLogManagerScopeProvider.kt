package com.intellij.gwt.run.remoteUi

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
class RemoteUiLogManagerScopeProvider(private val coroutineScope: CoroutineScope) {
  
  fun childScope(name: String): CoroutineScope {
    return coroutineScope.childScope(name)
  }
  
  companion object {
    @JvmStatic
    fun getInstance(project: Project): RemoteUiLogManagerScopeProvider = project.service()
  }
}
