package com.intellij.aiplayground.ui.env

import com.intellij.aiplayground.models.ANTHROPIC_PROVIDER
import com.intellij.aiplayground.models.DEEPSEEK_PROVIDER
import com.intellij.aiplayground.models.GEMINI_PROVIDER
import com.intellij.aiplayground.models.LlmProviderId
import com.intellij.aiplayground.models.LlmProviderInstanceId
import com.intellij.aiplayground.models.MISTRAL_PROVIDER
import com.intellij.aiplayground.models.OPENAI_PROVIDER
import com.intellij.aiplayground.models.settings.ApplicationSettingsManagerService
import com.intellij.aiplayground.models.settings.ApplicationSettingsManagerService.Companion.API_KEYS_UPDATES_TOPIC
import com.intellij.aiplayground.models.settings.PlaygroundSettings
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.readText
import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class ApiKeysInEnvVariablesService(val project: Project, coroutineScope: CoroutineScope) : Disposable {
  data class FoundKey(
    val providerId: LlmProviderId,
    val envVar: String,
    val apiKey: String,
    val source: EnvVarSource,
  )

  data class EnvVar(
    val envVar: String,
    val envVarValue: String,
    val source: EnvVarSource,
  )

  enum class EnvVarSource(val priority: Int) {
    DOT_ENV_FILE(0),
    DOR_ENV_FILE_TEMPLATE(2),
  }

  private val _keys = MutableStateFlow<AsyncState<Map<LlmProviderId, FoundKey>>>(AsyncState.Uninitialized)
  val keys: StateFlow<AsyncState<Map<LlmProviderId, FoundKey>>> = _keys.asStateFlow()

  val newKeys: StateFlow<Map<LlmProviderId, FoundKey>> = keys
    .map { asyncState ->
      when (asyncState) {
        is AsyncState.Uninitialized -> emptyMap()
        is AsyncState.Ready -> {
          val filtered = asyncState.value.filter { entry ->
            !ApplicationSettingsManagerService().checkProviderApiKeyExists(entry.key)
          }
          filtered
        }
      }
    }
    .stateIn(coroutineScope, SharingStarted.Eagerly, emptyMap())

  sealed interface NotificationEvent {
    data object Show : NotificationEvent
    data object Hide : NotificationEvent
    data object ProcessingComplete : NotificationEvent
  }

  private val _showAPIKeysFoundBanner = MutableStateFlow(true)
  val showAPIKeysFoundBanner: StateFlow<Boolean> = _showAPIKeysFoundBanner.asStateFlow()

  private val _notificationEvents = MutableSharedFlow<NotificationEvent>(
    replay = Int.MAX_VALUE,
    onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
  )
  val notificationEvents: SharedFlow<NotificationEvent> = _notificationEvents.asSharedFlow()

  init {
    coroutineScope.launch {
      update()
    }

    // Observe newKeys and emit show notification events
    coroutineScope.launch {
      keys.collect { asyncState ->
        if (asyncState is AsyncState.Ready) {
          val added = asyncState.value.filter { entry ->
            !ApplicationSettingsManagerService().checkProviderApiKeyExists(entry.key)
          }
          if (shouldShowStartupNotification(added.values.map { it.providerId }.toSet())) {
            _notificationEvents.emit(NotificationEvent.Show)
          }
          _notificationEvents.emit(NotificationEvent.ProcessingComplete)
        }
      }
    }

    val fileUpdatesListener = object : AsyncFileListener {
      override fun prepareChange(events: List<VFileEvent>): AsyncFileListener.ChangeApplier? {
        val root = project.guessProjectDir() ?: return null
        val hit = events.any { e ->
          when (e) {
            is VFileContentChangeEvent -> e.file.name == ".env" && e.file.parent == root
            is VFileCreateEvent -> !e.isDirectory && e.childName == ".env" && e.parent == root
            is VFileCopyEvent -> e.newChildName == ".env" && e.newParent == root
            is VFileMoveEvent -> e.file.name == ".env" && e.newParent == root
            else -> false
          }
        }
        if (!hit) return null
        return object : AsyncFileListener.ChangeApplier {
          override fun afterVfsChange() {
            AppExecutorUtil.getAppExecutorService().execute {
              coroutineScope.launch {
                update()
              }
            }
          }
        }
      }
    }

    VirtualFileManager.getInstance().addAsyncFileListenerBackgroundable(fileUpdatesListener, this)

    val apiKeysUpdatesListener = object : ApplicationSettingsManagerService.APIKeysUpdatesListener {
      override fun keyAdded(providerId: LlmProviderId) {
        coroutineScope.launch {
          update()
        }
      }

      override fun keyAdded(instanceId: LlmProviderInstanceId) {
        coroutineScope.launch {
          update()
        }
      }

      override fun keyRemoved(providerId: LlmProviderId) {
        coroutineScope.launch {
          update()
        }
      }

      override fun keyRemoved(providerId: LlmProviderInstanceId) {
        coroutineScope.launch {
          update()
        }
      }
    }

    ApplicationManager.getApplication().messageBus.connect(this).subscribe(API_KEYS_UPDATES_TOPIC, apiKeysUpdatesListener)
  }

  fun hideNotification() {
    _notificationEvents.tryEmit(NotificationEvent.Hide)
  }

  fun updateShowAPIKeysFoundBanner(value: Boolean) {
    _showAPIKeysFoundBanner.value = value
  }

  fun doNotShowAgain() {
    service<PlaygroundSettings>().showStartupNotification = false
    hideNotification()
  }

  fun cancelStartupNotification() {
    markNotificationAsShown()
    hideNotification()
  }

  private fun wasNotificationShown(): Boolean {
    return PropertiesComponent.getInstance(project).getBoolean(STARTUP_NOTIFICATION_SHOWN_KEY, false)
  }

  private fun shouldShowStartupNotification(addedKeys: Set<LlmProviderId>): Boolean {
    return addedKeys.isNotEmpty()
           && !wasNotificationShown()
           && service<PlaygroundSettings>().showStartupNotification
  }

  fun markNotificationAsShown() {
    PropertiesComponent.getInstance(project).setValue(STARTUP_NOTIFICATION_SHOWN_KEY, true)
  }

  suspend fun update() {
    val envVars = envVarsFromFiles(project)
    updateWithFoundEnvVars(envVars)
  }

  private fun updateWithFoundEnvVars(envVars: List<EnvVar>) {
    val settingsManager = service<ApplicationSettingsManagerService>()

    val prioritizedFoundEnvVars = envVars.sortedByDescending { it.source.priority }.associateBy { it.envVar }

    val updatedKeys = AI_PLAYGROUND_ENV_VARS_NAMES.map { (envVar, providerId) ->
      if (settingsManager.checkProviderApiKeyExists(providerId)) return@map null

      prioritizedFoundEnvVars[envVar]?.let {
        if (it.envVarValue.isBlank()) null
        else FoundKey(providerId, envVar, it.envVarValue, it.source)
      }
    }.filterNotNull().associateBy { it.providerId }

    _keys.value = AsyncState.Ready(updatedKeys)
  }

  private suspend fun envVarsFromFiles(
    project: Project,
  ): List<EnvVar> {
    val envVars: MutableList<EnvVar> = mutableListOf()

    val moduleManager = readAction {
      ModuleManager.getInstance(project)
    }
    for (module in moduleManager.modules) {
      val moduleRootManager = ModuleRootManager.getInstance(module)
      for (contentRoot in moduleRootManager.contentRoots) {
        val contentRootPath = Path.of(contentRoot.path)

        if (Files.exists(contentRootPath.resolve(".env")) && Files.isRegularFile(contentRootPath.resolve(".env"))) {
          val newEnvVars = envVarsFromFile(contentRootPath.resolve(".env"))
          envVars.addAll(newEnvVars)
        }

        try {
          Files.newDirectoryStream(contentRootPath) { path ->
            val fileName = path.fileName.toString()
            fileName.startsWith(".env.") && Files.isRegularFile(path)
          }.use { stream ->
            stream.forEach { envFilePath ->
              val newEnvVars = envVarsFromFile(envFilePath)
              envVars.addAll(newEnvVars)
            }
          }
        }
        catch (e: Exception) {
        }
      }
    }

    return envVars
  }

  private fun envVarsFromFile(envPath: Path): List<EnvVar> {
    if (Files.exists(envPath) && Files.isRegularFile(envPath)) {
      try {
        val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(envPath)
        return envVarsFromFile(virtualFile)
      }
      catch (e: Exception) {
      }
    }
    return listOf()
  }

  private fun envVarsFromFile(
    virtualFile: VirtualFile?,
  ): List<EnvVar> {
    val envVars: MutableList<EnvVar> = mutableListOf()
    val source = virtualFile?.name?.let { if (it == ".env") EnvVarSource.DOT_ENV_FILE else EnvVarSource.DOR_ENV_FILE_TEMPLATE }
                 ?: EnvVarSource.DOT_ENV_FILE

    virtualFile?.readText()?.lineSequence()?.filter { it.isNotBlank() && !it.startsWith("#") }?.forEach { line ->
      val parts = line.trim().split("=", limit = 2)
      if (parts.size == 2) {
        val key = parts[0].trim()
        val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
        if (value.isNotBlank()) {
          envVars.add(EnvVar(key, value, source))
        }
      }
    }

    return envVars
  }

  override fun dispose() {
  }

  companion object {
    private const val STARTUP_NOTIFICATION_SHOWN_KEY = "ai.playground.env.api.keys.dialog.shown"

    fun getInstance(project: Project): ApiKeysInEnvVariablesService = project.service<ApiKeysInEnvVariablesService>()
    private val AI_PLAYGROUND_ENV_VARS_NAMES = mapOf(
      "OPENAI_API_KEY" to OPENAI_PROVIDER.id,
      "ANTHROPIC_API_KEY" to ANTHROPIC_PROVIDER.id,
      "MISTRAL_API_KEY" to MISTRAL_PROVIDER.id,
      "DEEPSEEK_API_KEY" to DEEPSEEK_PROVIDER.id,
      "GEMINI_API_KEY" to GEMINI_PROVIDER.id,
    )

  }
}

sealed interface AsyncState<out T> {
  object Uninitialized : AsyncState<Nothing>
  data class Ready<T>(val value: T) : AsyncState<T>
}