package com.intellij.aidebugger.evaluation.settings.viewmodels

import com.intellij.aidebugger.evaluation.models.llm.LlmProviderType
import com.intellij.aidebugger.evaluation.settings.AIToolkitModelsService
import com.intellij.aidebugger.evaluation.settings.AIToolkitSettingsService
import com.intellij.aidebugger.evaluation.settings.env.ApiKeysInEnvVariablesService
import com.intellij.aidebugger.evaluation.settings.isChinaRegion
import com.intellij.aidebugger.evaluation.settings.models.LlmModel
import com.intellij.aidebugger.evaluation.settings.models.ProviderInstance
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.util.coroutines.childScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AIToolkitSettingsViewModel(project: Project, parentScope: CoroutineScope) {

    private val coroutineScope = parentScope.childScope("AIToolkitSettingsViewModel")
    private val settingsService = service<AIToolkitSettingsService>()
    private val modelsService = service<AIToolkitModelsService>()

    private val _instances = MutableStateFlow<List<ProviderInstance>>(emptyList())
    val instances: StateFlow<List<ProviderInstance>> = _instances.asStateFlow()

    private val _selectedInstance = MutableStateFlow<ProviderInstance?>(null)
    val selectedInstance: StateFlow<ProviderInstance?> = _selectedInstance.asStateFlow()

    private val _connectionState = MutableStateFlow<Map<String, ConnectionState>>(emptyMap())
    val connectionState: StateFlow<Map<String, ConnectionState>> = _connectionState.asStateFlow()

    private val _availableModels = MutableStateFlow<Map<String, List<LlmModel>>>(emptyMap())  // instanceId -> models
    val availableModels: StateFlow<Map<String, List<LlmModel>>> = _availableModels.asStateFlow()

    private val _modelsLoading = MutableStateFlow<Map<String, Boolean>>(emptyMap())  // instanceId -> loading state
    val modelsLoading: StateFlow<Map<String, Boolean>> = _modelsLoading.asStateFlow()

    private var savedInstances: List<ProviderInstance> = emptyList()

    val availableProviderTypes: List<LlmProviderType> = if (isChinaRegion()) {
        listOf(LlmProviderType.OPENAI_COMPATIBLE)
    } else {
        LlmProviderType.entries
    }

    private val _showAPIKeysFoundBanner = MutableStateFlow(
        PropertiesComponent.getInstance(project).getBoolean(AI_TOOLKIT_SHOW_IMPORT_KEYS_BANNER_IN_SETTINGS, true)
    )
    val showAPIKeysFoundBanner: StateFlow<Boolean> = _showAPIKeysFoundBanner.asStateFlow()

    val newApiKeysNotInCurrentProviders: StateFlow<Map<LlmProviderType, ApiKeysInEnvVariablesService.FoundKey>> = combine(
        instances,
        ApiKeysInEnvVariablesService.getInstance(project).newKeys
    ) { providers, newKeys ->
        newKeys.filter { !providers.any { provider -> provider.providerType == it.key } }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, mapOf())


    private var resetJob: Job? = null
    private var updateJob: Job? = null


    init {
        reset()
    }

    /**
     * Reset to saved state
     */
    fun reset() {
        _instances.value = emptyList()
        _availableModels.value = emptyMap()
        _selectedInstance.value = null
        savedInstances = emptyList()
        val oldJob = resetJob

        resetJob = coroutineScope.launch(Dispatchers.IO) {
            oldJob?.cancelAndJoin()
            val loaded = settingsService.getInstances()
            withContext(Dispatchers.Main) {
                _instances.value = loaded
                savedInstances = loaded
                _availableModels.value = loaded.associate { instance ->
                    instance.id to instance.availableModels
                }
                _selectedInstance.value = null
            }
        }
    }

    fun update() {
        val oldJob = updateJob
        updateJob = coroutineScope.launch {
            oldJob?.cancelAndJoin()
            settingsService.getInstances()
                .let { instances ->
                    val mergedProviders = mutableListOf<ProviderInstance>()
                    mergedProviders.addAll(_instances.value)
                    instances.filter {
                        !mergedProviders.any { instance -> instance.id == it.id }
                    }.let { mergedProviders.addAll(it) }
                    _instances.value = mergedProviders
                    apply()
                }
        }
    }

    /**
     * Apply changes to persistent storage
     */
    fun apply() {
        settingsService.saveInstances(_instances.value)
        savedInstances = _instances.value
    }

    /**
     * Check if settings have been modified
     */
    fun isModified(): Boolean {
        return _instances.value != savedInstances
    }

    /**
     * Add a new provider instance with default name
     */
    fun addInstance(providerType: LlmProviderType) {
        if (_instances.value.any { it.providerType == providerType }) {
            return
        }
        val newInstance = ProviderInstance(
            providerType = providerType,
            name = providerType.displayName
        )
        _instances.update { it + newInstance }
        _selectedInstance.value = newInstance
    }

    /**
     * Remove the currently selected instance
     */
    fun removeSelectedInstance() {
        val current = _selectedInstance.value ?: return
        _instances.update { list ->
            val filtered = list.filter { it.id != current.id }
            _selectedInstance.value = filtered.firstOrNull()
            filtered
        }
        _connectionState.update { it - current.id }
    }

    /**
     * Update the selected instance
     */
    fun updateSelectedInstance(updatedInstance: ProviderInstance) {
        _instances.update { list ->
            list.map { if (it.id == updatedInstance.id) updatedInstance else it }
        }
        _selectedInstance.value = updatedInstance
    }

    /**
     * Select an instance
     */
    fun selectInstance(instance: ProviderInstance?) {
        _selectedInstance.value = instance
    }

    /**
     * Test connection for the selected instance
     */
    fun testConnection() {
        val instance = _selectedInstance.value ?: return
        if (instance.apiKey.isBlank()) {
            _connectionState.update { it + (instance.id to ConnectionState.FAILED) }
            return
        }

        _connectionState.update { it + (instance.id to ConnectionState.CONNECTING) }

        coroutineScope.launch {
            val success = modelsService.testConnection(instance)
            _connectionState.update {
                it + (instance.id to if (success) ConnectionState.CONNECTED else ConnectionState.FAILED)
            }
        }
    }

    /**
     * Fetch available models for the selected instance
     * Updates happen atomically to prevent UI glitches
     */
    fun fetchModels() {
        val instance = _selectedInstance.value ?: return
        if (instance.apiKey.isBlank()) return

        if (_modelsLoading.value[instance.id] == true) return

        _modelsLoading.update { it + (instance.id to true) }

        coroutineScope.launch {
            try {
                val models = modelsService.fetchModels(instance)

                withContext(Dispatchers.Main) {
                    _instances.update { list ->
                        list.map { if (it.id == instance.id) it.copy(availableModels = models) else it }
                    }
                    _selectedInstance.update { current ->
                        if (current?.id == instance.id) current.copy(availableModels = models) else current
                    }

                    _availableModels.update { it + (instance.id to models) }
                    _modelsLoading.update { it + (instance.id to false) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _availableModels.update { it + (instance.id to emptyList()) }
                    _modelsLoading.update { it + (instance.id to false) }
                }
            }
        }
    }

    /**
     * Enable models for the selected instance
     */
    fun enableModels(modelIds: List<String>) {
        val instance = _selectedInstance.value ?: return
        val updated = instance.copy(
            disabledModels = instance.disabledModels - modelIds.toSet()
        )
        updateSelectedInstance(updated)
    }

    /**
     * Disable models for the selected instance
     */
    fun disableModels(modelIds: List<String>) {
        val instance = _selectedInstance.value ?: return
        val updated = instance.copy(
            disabledModels = instance.disabledModels + modelIds.toSet()
        )
        updateSelectedInstance(updated)
    }

    fun dispose() {
        coroutineScope.cancel()
    }

    enum class ConnectionState {
        UNKNOWN,
        CONNECTING,
        CONNECTED,
        FAILED
    }

    fun updateShowAPIKeysFoundBanner(value: Boolean) {
        _showAPIKeysFoundBanner.value = value
    }

    companion object {
        const val AI_TOOLKIT_SHOW_IMPORT_KEYS_BANNER_IN_SETTINGS: String = "ai.toolkit.ignore.import.keys.banner.in.settings"
    }
}
