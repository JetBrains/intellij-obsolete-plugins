package com.intellij.python.huggingFace.cacheManager

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.python.community.impl.huggingFace.service.HuggingFaceCoroutine
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.python.packaging.common.PythonPackageManagementListener
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.packaging.management.PythonPackageManager.Companion.PACKAGE_MANAGEMENT_TOPIC
import com.jetbrains.python.packaging.management.hasInstalledPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
internal class HfCachePackageManagementListenerService(private val project: Project, private val coroutineScope: CoroutineScope) {
  companion object {
    @JvmStatic
    fun getInstance(project: Project): HfCachePackageManagementListenerService = project.service()
  }

  private var packageManagementListener: HfCachePackageManagementListener? = null

  fun register(toolWindow: ToolWindow) {
    packageManagementListener?.let { Disposer.register(toolWindow.disposable, it) }
  }

  fun createListener() {
    packageManagementListener?.let { Disposer.dispose(it) }
    packageManagementListener = HfCachePackageManagementListener(project, coroutineScope)
  }
}

private class HfCachePackageManagementListener(private val project: Project, val coroutineScope: CoroutineScope) : Disposable {
  private var connection: MessageBusConnection? = null

  init {
    connection = ApplicationManager.getApplication().messageBus.connect()
    connection?.subscribe(PACKAGE_MANAGEMENT_TOPIC, object : PythonPackageManagementListener {
      override fun packagesChanged(sdk: Sdk) {
        if (!Registry.`is`("hugging.face.cache.management", false)) return
        coroutineScope.launch {
          val hasHf = PythonPackageManager.forSdk(project, sdk).hasInstalledPackage(HF_HUB_PACKAGE)
          withContext(Dispatchers.EDT) {
            if (hasHf) showToolWindow() else hideToolWindow()
          }
        }

      }
    })
  }

  private fun showToolWindow() = HuggingFaceCoroutine.Utils.edtScope.launch { getToolwindow()?.setAvailable(true) }
  private fun hideToolWindow() = HuggingFaceCoroutine.Utils.edtScope.launch { getToolwindow()?.setAvailable(false) }
  private fun getToolwindow(): ToolWindow? = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID)
  override fun dispose() = connection?.disconnect() ?: Unit

  companion object {
    const val TOOL_WINDOW_ID = "HfCacheToolWindow"
    const val HF_HUB_PACKAGE = "huggingface-hub"
  }
}
