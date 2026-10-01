package com.intellij.aidebugger.evaluation.settings.env

import com.intellij.aidebugger.evaluation.models.llm.LlmProviderType
import com.intellij.aidebugger.evaluation.settings.AIToolkitSettingsService
import com.intellij.aidebugger.evaluation.settings.AIToolkitSettingsService.Companion.AI_TOOLKIT_SETTINGS_UPDATE
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
import com.intellij.util.EnvironmentUtil
import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class ApiKeysInEnvVariablesService(val project: Project, coroutineScope: CoroutineScope) : Disposable {
    data class FoundKey(
        val providerId: LlmProviderType,
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
        ENV(1),
        DOT_ENV_FILE(0),
        DOR_ENV_FILE_TEMPLATE(2),
    }

    private val _keys = MutableStateFlow<Map<LlmProviderType, FoundKey>>(emptyMap())
    val keys: StateFlow<Map<LlmProviderType, FoundKey>> = _keys.asStateFlow()

    val newKeys: StateFlow<Map<LlmProviderType, FoundKey>> = keys
        .map { it.filter { entry -> service<AIToolkitSettingsService>().getCredentials(entry.key) == null } }
        .stateIn(coroutineScope, SharingStarted.Eagerly, emptyMap())

    private val _showAPIKeysFoundBanner = MutableStateFlow(true)
    val showAPIKeysFoundBanner: StateFlow<Boolean> = _showAPIKeysFoundBanner.asStateFlow()

    val bannerVisibleFlow: Flow<Boolean> = combine(
        showAPIKeysFoundBanner,
        newKeys
    ) { show, keys -> show && keys.isNotEmpty() }
        .distinctUntilChanged()

    val _hideNotificationFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val hideNotificationFlow: StateFlow<Boolean> = _hideNotificationFlow.asStateFlow()

    init {
        coroutineScope.launch {
            update()
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

        VirtualFileManager.getInstance().addAsyncFileListener(fileUpdatesListener, this)

        val apiKeysUpdatesListener = object : AIToolkitSettingsService.AIToolkitSettingUpdatesListener {
            override fun settingsUpdated() {
                coroutineScope.launch {
                    update()
                }
            }
        }

        ApplicationManager.getApplication().messageBus.connect(this)
            .subscribe(AI_TOOLKIT_SETTINGS_UPDATE, apiKeysUpdatesListener)
    }

    fun hideNotification() {
        _hideNotificationFlow.value = true
    }

    fun updateShowAPIKeysFoundBanner(value: Boolean) {
        _showAPIKeysFoundBanner.value = value
    }

    fun anyNewKeysFound(): Boolean {
        return newKeys.value.isNotEmpty()
    }

    suspend fun update() {
        val envVars =
            EnvironmentUtil.getEnvironmentMap().map { EnvVar(it.key, it.value, EnvVarSource.ENV) }.toMutableList()

        val envVarsFromFiles = envVarsFromFiles(project)
        envVars.addAll(envVarsFromFiles)

        updateWithFoundEnvVars(envVars)
    }

    private fun updateWithFoundEnvVars(envVars: List<EnvVar>) {
        val settingsManager = service<AIToolkitSettingsService>()

        val prioritizedFoundEnvVars = envVars.sortedByDescending { it.source.priority }.associateBy { it.envVar }

        val updatedKeys = AI_TOOLKIT_ENV_VARS_NAMES.map { (envVar, providerId) ->
            if (settingsManager.getCredentials(providerId) != null) return@map null

            prioritizedFoundEnvVars[envVar]?.let {
                if (it.envVarValue.isBlank()) null
                else FoundKey(providerId, envVar, it.envVarValue, it.source)
            }
        }.filterNotNull().associateBy { it.providerId }

        _keys.value = updatedKeys
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
                } catch (e: Exception) {
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
            } catch (e: Exception) {
            }
        }
        return listOf()
    }

    private fun envVarsFromFile(
        virtualFile: VirtualFile?,
    ): List<EnvVar> {
        val envVars: MutableList<EnvVar> = mutableListOf()
        val source =
            virtualFile?.name?.let { if (it == ".env") EnvVarSource.DOT_ENV_FILE else EnvVarSource.DOR_ENV_FILE_TEMPLATE }
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
        fun getInstance(project: Project): ApiKeysInEnvVariablesService =
            project.service<ApiKeysInEnvVariablesService>()

        private val AI_TOOLKIT_ENV_VARS_NAMES = mapOf(
            "OPENAI_API_KEY" to LlmProviderType.OPENAI,
            "ANTHROPIC_API_KEY" to LlmProviderType.ANTHROPIC,
            "GEMINI_API_KEY" to LlmProviderType.GEMINI,
        )
    }
}