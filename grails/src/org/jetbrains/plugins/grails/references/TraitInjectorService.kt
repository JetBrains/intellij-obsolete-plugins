/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.plugins.grails.references

import com.intellij.execution.CantRunException
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.FindClassUtil
import com.intellij.util.Function
import com.intellij.util.PathUtil
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.jetbrains.groovy.grails.rt.GrailsRtMarker
import org.jetbrains.plugins.grails.util.GrailsArtifact
import org.jetbrains.plugins.grails.util.GrailsUtils
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition
import org.jetbrains.plugins.groovy.runner.DefaultGroovyScriptRunner
import org.jetbrains.plugins.groovy.runner.GroovyScriptRunConfiguration
import java.util.Collections
import java.util.LinkedList
import java.util.Objects
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext

@Service(Service.Level.PROJECT)
@State(name = "TraitInjectorService", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class TraitInjectorService(
  private val myProject: Project
) : PersistentStateComponent<TraitInjectorService.PersistentState>, Disposable {

  private val myMapRef = AtomicReference<Map<String, StateHolder>>(HashMap())

  private val mySupervisorJob = SupervisorJob()
  private val myCoroutineScope = CoroutineScope(mySupervisorJob)

  init {
    myProject.messageBus.connect().subscribe(ModuleListener.TOPIC, object : ModuleListener {
      override fun modulesRenamed(project: Project,
                                  modules: List<Module>,
                                  oldNameProvider: Function<in Module, String>) {
        mySupervisorJob.cancelChildren() // cancel early, roots changed is about to happen
        while (true) {
          val existingMap = myMapRef.get()
          val newMap: MutableMap<String, StateHolder> = HashMap(existingMap)
          for (module in modules) {
            val oldName = oldNameProvider.`fun`(module)
            val state = newMap.remove(oldName)
            if (state != null) newMap[module.name] = state
          }
          if (myMapRef.compareAndSet(existingMap, Collections.unmodifiableMap(newMap))) break
        }
      }
    })
  }

  override fun dispose() {
    mySupervisorJob.cancel()
  }

  fun queueUpdate() {
    if (ApplicationManagerEx.isInIntegrationTest()) {
      LOG.trace("$myProject: Update requested and skipped")
      return
    }
    LOG.trace("$myProject: Update requested")
    mySupervisorJob.cancelChildren()
    myCoroutineScope.launch {
      update()
    }
    LOG.debug("$myProject: Update queued")
  }

  private suspend fun update() {
    val dataHolder: RunDataHolder? = smartReadAction(myProject, ::createRunData)
    if (dataHolder == null || dataHolder.dataList.isEmpty()) {
      return
    }
    val results = runData(dataHolder.dataList) ?: return
    var needToRestartHighlighting = false
    for ((data: RunData, traits: Map<String, Collection<String>>) in results) {
      dataHolder.newMap[data.key] = StateHolder(data.knownFactories, traits)
      val oldState: StateHolder? = dataHolder.oldMap[data.key]
      needToRestartHighlighting = needToRestartHighlighting or (oldState == null || traits != oldState.traits)
    }
    coroutineContext.ensureActive()
    val newMap: Map<String, StateHolder> = Collections.unmodifiableMap(dataHolder.newMap)
    if (!myMapRef.compareAndSet(dataHolder.oldMap, newMap)) {
      LOG.warn("$myProject: State changed, updating anyway")
      myMapRef.set(newMap)
    }
    else {
      LOG.debug("$myProject: State updated")
    }
    if (needToRestartHighlighting) {
      restartHighlighting()
    }
  }

  private fun createRunData(): RunDataHolder? {
    LOG.debug("$myProject: Checking modules and creating run data")
    val modules = FindClassUtil.findModulesWithClass(myProject, TRAIT_INJECTOR_FQN)
    if (modules.isEmpty()) {
      LOG.debug("$myProject: No modules with TraitInjector")
      return null
    }
    val oldMap = myMapRef.get()
    val newMap: MutableMap<String, StateHolder> = HashMap()
    val dataList: MutableList<RunData> = ArrayList()
    for (module in modules) {
      ProgressManager.checkCanceled()
      val key = module.name
      val oldState = oldMap[key]
      val data: RunData? = try {
        checkIfModuleNeedsUpdate(oldState, module)
      }
      catch (e: CantRunException) {
        LOG.debug(e)
        null
      }
      if (data == null) { // module does not need update or smth failed to create command line
        if (oldState != null) {
          newMap[key] = oldState // reuse old state if available
        }
      }
      else {
        dataList.add(data)
      }
    }
    return RunDataHolder(dataList, oldMap, newMap)
  }

  @Throws(CantRunException::class)
  private fun checkIfModuleNeedsUpdate(oldState: StateHolder?, module: Module): RunData? {
    LOG.debug("$module: Checking module")
    val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
    val factories = FilenameIndex.getVirtualFilesByName("grails.factories", scope)
    val factoriesPaths = ContainerUtil.map(factories) { obj: VirtualFile -> obj.path }
    if (oldState != null && oldState.knownFactories == factoriesPaths) {
      LOG.debug("$module: Factories not changed")
      return null
    }
    val params = GroovyScriptRunConfiguration.createJavaParametersWithSdk(module)
    DefaultGroovyScriptRunner.configureGenericGroovyRunner(params, module, "groovy.ui.GroovyMain", false, true)
    params.classPath.add(PathUtil.getJarPathForClass(GrailsRtMarker::class.java))
    params.setDefaultCharset(myProject)
    params.programParametersList.add(GrailsRtMarker::class.java.getResource("/idea-injected-traits.groovy").toString())
    params.setUseDynamicParameters(true)
    params.useDynamicClasspathDefinedByJdkLevel()
    val commandLine = params.toCommandLine()
    LOG.debug("$module: Command line created")
    LOG.trace(module.toString() + ": " + commandLine.commandLineString)
    return RunData(module.name, module, factoriesPaths, commandLine)
  }

  private suspend fun runData(dataList: List<RunData>): List<Pair<RunData, Map<String, Collection<String>>>>? {
    return coroutineScope {
      val deferredResults = dataList.map { data ->
        async(Dispatchers.IO) {
          val traits: Map<String, Collection<String>>? = runData(data)
          if (traits == null) null else Pair(data, traits)
        }
      }
      try {
        deferredResults.awaitAll().filterNotNull()
      }
      catch (e: ExecutionException) {
        ExecutionUtil.handleExecutionError(myProject, ToolWindowId.RUN, "Collecting Injected Traits", e)
        LOG.debug("Error executing script", e)
        null
      }
    }
  }

  private fun restartHighlighting() {
    ApplicationManager.getApplication().invokeLater(Runnable {
      PsiManager.getInstance(myProject).dropPsiCaches()
      LOG.debug("$myProject: Highlighting restarted")
    }, myProject.disposed)
  }

  private suspend fun runData(data: RunData): Map<String, Collection<String>>? {
    val module = data.module
    val commandLine = data.commandLine
    val handler = CapturingProcessHandler(commandLine)
    val output = coroutineToIndicator {
      val indicator = requireNotNull(ProgressManager.getGlobalProgressIndicator())
      handler.runProcessWithProgressIndicator(indicator, 1000 * 60)
    }
    if (output.exitCode != 0) {
      LOG.debug(module.toString() + ": Script exited with " + output.exitCode + ". Stderr: '" + output.stderr + "'")
      return null
    }
    val stdout = output.stdout
    LOG.debug("$module: Script output: $stdout")
    return try {
      doParseOutput(stdout)
    }
    catch (e: Throwable) {
      LOG.info("$module: Error parsing output", e)
      null
    }
  }

  private fun doParseOutput(output: String): Map<String, Collection<String>> {
    val result: MutableMap<String, MutableCollection<String>> = HashMap()
    val lines: LinkedList<String> = LinkedList(output.trim().split("\n".toRegex()))
    while (true) {
      if (lines.pop() == MARKER) break
    }
    while (lines.size > 1) {
      val traitName = lines.pop()
      var artifactCount = lines.pop().toInt()
      while (artifactCount-- > 0) {
        val artifactName = lines.pop()
        result.computeIfAbsent(artifactName) { SmartList() }.add(traitName)
      }
    }
    assert(lines.pop() == MARKER)
    return result
  }

  override fun getState(): PersistentState? {
    val map = myMapRef.get()
    return if (map.isEmpty()) null else PersistentState(map)
  }

  override fun loadState(persistentState: PersistentState) {
    myMapRef.set(Collections.unmodifiableMap(persistentState.state))
  }

  private class RunDataHolder(val dataList: List<RunData>,
                              val oldMap: Map<String, StateHolder>,
                              val newMap: MutableMap<String, StateHolder>)

  private class RunData(val key: String,
                        val module: Module,
                        val knownFactories: List<String>,
                        val commandLine: GeneralCommandLine)

  class PersistentState(var state: Map<String, StateHolder> = HashMap())

  class StateHolder(var knownFactories: Collection<String> = ArrayList(),
                    var traits: Map<String, Collection<String>> = HashMap())

  companion object {
    private val LOG: Logger = Logger.getInstance(TraitInjectorService::class.java)
    private const val TRAIT_INJECTOR_FQN = "grails.compiler.traits.TraitInjector"
    private const val MARKER = "--------------------------------------"
    private val ARTEFACT_TO_NAME: Map<GrailsArtifact, String> = mapOf(
      GrailsArtifact.DOMAIN to "Domain",
      GrailsArtifact.CONTROLLER to "Controller",
      GrailsArtifact.SERVICE to "Service",
      GrailsArtifact.INTERCEPTOR to "Interceptor",
      GrailsArtifact.TAGLIB to "TagLibrary"
    )

    private fun getInstance(project: Project): TraitInjectorService {
      return Objects.requireNonNull(project.getService(TraitInjectorService::class.java))
    }

    @JvmStatic
    fun getInjectedTraits(clazz: GrTypeDefinition): Collection<String> {
      val artefactType = ARTEFACT_TO_NAME[GrailsUtils.calculateArtifactType(clazz)]
                         ?: return emptyList()
      return getInjectedTraits(clazz, artefactType)
    }

    fun getInjectedTraits(context: PsiElement, artefactType: String): Collection<String> {
      val result: MutableSet<String> = LinkedHashSet()
      result.addAll(doGetInjectedTraits(context, artefactType))
      result.addAll(doGetEnhancesTraits(context, artefactType))
      return result
    }

    private fun doGetInjectedTraits(context: PsiElement, artefactType: String): Collection<String> {
      val module = ModuleUtilCore.findModuleForPsiElement(context) ?: return emptyList()
      val state = getInstance(context.project).myMapRef.get()[module.name]
                  ?: return emptyList()
      val result = state.traits[artefactType]
      return result ?: emptyList()
    }

    @JvmStatic
    fun queueUpdate(project: Project) {
      getInstance(project).queueUpdate()
    }
  }
}
