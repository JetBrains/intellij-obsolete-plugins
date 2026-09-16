package com.intellij.bigdatatools.zeppelin.dependency

import com.intellij.bigdatatools.zeppelin.components.ZeppelinInstanceCachedConnection
import com.intellij.bigdatatools.zeppelin.components.connections.ZeppelinConnectionListener
import com.intellij.bigdatatools.zeppelin.dependency.collector.ZeppelinLibraryCollector
import com.intellij.bigdatatools.zeppelin.dependency.model.ZeppelinLibrary
import com.intellij.bigdatatools.zeppelin.dependency.module.NoteModuleSynchronizer
import com.intellij.bigdatatools.zeppelin.dependency.module.ZeppelinModuleUtils
import com.intellij.bigdatatools.zeppelin.dependency.resolver.ZepDepRootsResolverService
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.bigdatatools.zeppelin.utils.ZeppelinProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.Disposer
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.jetbrains.bigdatatools.common.util.launchBackgroundTask

class ZeppelinDependencyManager(private val project: Project?,
                                val cachedConnection: ZeppelinInstanceCachedConnection) : NoteDependencyManager {
  private var isDisposed = false
  private val config = cachedConnection.config
  private val dependenciesCollector = ZeppelinLibraryCollector(if (config.isPerProject) project else null, cachedConnection)

  private val connectionListener = object : ZeppelinConnectionListener {
    override fun updateInterpreterSettings(interpreterSettings: List<InterpreterSettings>) =
      launchResolveDependenciesForAllProject()
  }

  init {
    val projectListener = object : ProjectManagerListener {
      @Deprecated("Deprecated in Java")
      override fun projectOpened(project: Project) {
        if (this@ZeppelinDependencyManager.project == null)
          invokeUpdateForProject(project)
      }
    }
    ApplicationManager.getApplication().messageBus.connect(this).subscribe(ProjectManager.TOPIC, projectListener)

    cachedConnection.addListener(connectionListener)
  }

  override fun dispose() {
    cachedConnection.removeListener(connectionListener)
    isDisposed = true
  }

  override fun getSdk(project: Project): Sdk? = ZeppelinModuleUtils.getModule(project, config.innerId)?.let {
    ModuleRootManager.getInstance(it).sdk
  }

  override fun getAllLibraries() = dependenciesCollector.getAllDependencies()

  override fun updateUserLibs(libs: List<ZeppelinLibrary>) {
    dependenciesCollector.updateStoredLibs(libs)
    launchResolveDependenciesForAllProject()
  }

  override fun isResolving(project: Project) = synchronized(this) {
    currentTasksForProject[project] != null
  }

  override fun isLibraryResolved(project: Project, library: ZeppelinLibrary) = NoteModuleSynchronizer.isExists(project, config.innerId,
                                                                                                               library)

  override fun launchResolveDependenciesForAllProject(force: Boolean) {
    val projects = ZeppelinProjectUtil.getProjectList(project)
    projects.forEach {
      invokeUpdateForProject(it, force)
    }
  }

  private val currentTasksForProject = mutableMapOf<Project, List<ZeppelinLibrary>>()
  private val planningTask = mutableMapOf<Project, List<ZeppelinLibrary>>()

  private fun invokeUpdateForProject(project: Project, force: Boolean = false) {
    if (ApplicationManager.getApplication().isUnitTestMode)
      return

    val dependencies = dependenciesCollector.getAllDependencies()
    synchronized(project) {
      val existsDependenciesUpdate = currentTasksForProject[project]
      if (existsDependenciesUpdate == null) {
        currentTasksForProject[project] = dependencies
        return@synchronized
      }
      if (existsDependenciesUpdate == dependencies && planningTask[project] == null)
        return
      planningTask[project] = dependencies
      return
    }
    launchBackgroundLibUpdateTask(project, dependencies, force)
  }

  private fun launchBackgroundLibUpdateTask(project: Project,
                                            dependencies: List<ZeppelinLibrary>,
                                            force: Boolean = false) = launchBackgroundTask(project,
                                                                                           ZepMessagesBundle.message("dependency.sync.task",
                                                                                                                     config.getNameWithAddress()),
                                                                                           cancelable = true) { indicator ->
    if (isDisposed)
      return@launchBackgroundTask
    Disposer.register(this) {
      indicator.cancel()
    }

    indicator.isIndeterminate = false
    indicator.fraction = 0.0
    indicator.text = ZepMessagesBundle.message("dependency.synchronization.start", config.getNameWithAddress())
    try {
      val libs = dependencies.filter { it.dep.isFile() || it.dep.isMaven() }
      val repos = dependenciesCollector.getAllRepositories()
      val configId = config.innerId

      val resolverService = ZepDepRootsResolverService.getInstance()
      val projectForResolve = if (config.isPerProject) project else null
      val roots = resolverService.resolveOrGetFromCache(libs, repos, projectForResolve, configId, indicator, force)

      val moduleDependencies = dependencies.filter { it.dep.isModule() }
      NoteModuleSynchronizer.createOrUpdateModuleWithDependencies(project, config.innerId, libs, roots, moduleDependencies, emptyList(),
                                                                  parentDisposable = this)
    }
    finally {
      synchronized(project) {
        val plans = planningTask[project]
        currentTasksForProject -= project
        planningTask -= project
        if (plans != null)
          executeOnPooledThread {
            invokeUpdateForProject(project, force)
          }
      }
    }
  }
}