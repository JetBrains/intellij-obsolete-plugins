package com.intellij.aidebugger.evaluation.viewModels

import com.intellij.aidebugger.common.models.TracesDatasetsRepository
import com.intellij.aidebugger.evaluation.models.EvaluationPromptDefaults
import com.intellij.aidebugger.evaluation.models.entities.ConfigInfo
import com.intellij.aidebugger.evaluation.models.entities.EvalRunConfig
import com.intellij.aidebugger.evaluation.models.entities.EvaluatorConfig
import com.intellij.aidebugger.evaluation.models.llm.LlmProviderType
import com.intellij.aidebugger.evaluation.models.repositories.EvalConfigsRepositoryImpl
import com.intellij.aidebugger.evaluation.settings.AIToolkitModelsService
import com.intellij.aidebugger.evaluation.settings.AIToolkitSettingsService
import com.intellij.aidebugger.evaluation.settings.models.LlmModel
import com.intellij.aidebugger.evaluation.settings.models.ProviderInstance
import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Paths

class EvalConfigPopupViewModel(
    private val project: Project,
    val configsRepository: EvalConfigsRepositoryImpl,
    private val inputDatasetsRepo: TracesDatasetsRepository,
    private val settingsService: AIToolkitSettingsService,
    private val modelsService: AIToolkitModelsService
) {
    enum class NameValidationError {
        INVALID_CHARACTERS,
        ALREADY_EXISTS
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _config = MutableStateFlow<EvalRunConfig?>(null)
    val config: StateFlow<EvalRunConfig?> = _config.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _selectedDataset = MutableStateFlow<@NlsSafe String>("")
    val selectedDataset: StateFlow<@NlsSafe String> = _selectedDataset.asStateFlow()

    private val _modelName = MutableStateFlow("")
    val modelName: StateFlow<String> = _modelName

    private val _promptTemplate = MutableStateFlow("")
    val promptTemplate: StateFlow<String> = _promptTemplate

    private val _modelParams = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val modelParams: StateFlow<List<Pair<String, String>>> = _modelParams.asStateFlow()

    private val _runConfigName = MutableStateFlow<@NlsSafe String>("")
    val runConfigName: StateFlow<@NlsSafe String> = _runConfigName.asStateFlow()

    private val _availableDatasets = MutableStateFlow<List<@NlsSafe String>>(emptyList())
    val availableDatasets: StateFlow<List<@NlsSafe String>> = _availableDatasets.asStateFlow()

    private val _availableRunConfigs = MutableStateFlow<List<@NlsSafe String>>(emptyList())
    val availableRunConfigs: StateFlow<List<@NlsSafe String>> = _availableRunConfigs.asStateFlow()

    private val _availableProviders = MutableStateFlow<List<ProviderInstance>>(emptyList())
    val availableProviders: StateFlow<List<ProviderInstance>> = _availableProviders.asStateFlow()

    private val _selectedProvider = MutableStateFlow<ProviderInstance?>(null)
    val selectedProvider: StateFlow<ProviderInstance?> = _selectedProvider.asStateFlow()

    private val _availableModels = MutableStateFlow<List<LlmModel>>(emptyList())
    val availableModels: StateFlow<List<LlmModel>> = _availableModels.asStateFlow()

    private val _selectedModel = MutableStateFlow<LlmModel?>(null)
    val selectedModel: StateFlow<LlmModel?> = _selectedModel.asStateFlow()

    private val _isLoadingModels = MutableStateFlow(false)
    val isLoadingModels: StateFlow<Boolean> = _isLoadingModels.asStateFlow()

    private val _nameValidationError = MutableStateFlow<NameValidationError?>(null)
    val nameValidationError: StateFlow<NameValidationError?> = _nameValidationError.asStateFlow()

    private val _isValid = MutableStateFlow(true)
    val isValid: StateFlow<Boolean> = _isValid.asStateFlow()

    private val _evaluators = MutableStateFlow<List<EvaluatorConfig>>(emptyList())
    val evaluators: StateFlow<List<EvaluatorConfig>> = _evaluators.asStateFlow()

    private val _evaluatorValidation = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val evaluatorValidation: StateFlow<Map<Int, Boolean>> = _evaluatorValidation.asStateFlow()

    private var selectedInfo: ConfigInfo? = null

    init {
        scope.launch {
            _availableDatasets.collect {
                val cfg = _config.value
                if (cfg != null) {
                    val current = _selectedDataset.value
                    val resolved = resolveDatasetLabel(cfg.datasetName)
                    if (resolved.isNotEmpty() && current.isEmpty()) {
                        _selectedDataset.value = resolved
                        updateOverallValidation()
                    }
                }
            }
        }

        scope.launch {
            _availableProviders.collect {
                val cfg = _config.value
                if (cfg != null && it.isNotEmpty()) {
                    restoreProviderAndModel(cfg)
                }
            }
        }
    }

    fun loadAvailableOptions() {
        scope.launch {
            val dsNames = mutableListOf("")
            dsNames.addAll(inputDatasetsRepo.listDatasets().map { it.name })
            val runManager = RunManager.getInstance(project)
            val allRcSettings = runCatching { runManager.allSettings }.getOrNull() ?: emptyList()
            val providers = settingsService.getInstances().filter { it.hasCredentials() }
            withContext(Dispatchers.Default) {
                _availableDatasets.value = dsNames
                _availableRunConfigs.value = allRcSettings.map { it.name }
                _availableProviders.value = providers
            }
        }
    }

    fun loadConfig(configInfo: ConfigInfo? = null) {
        selectedInfo = configInfo

        val cfg = if (configInfo != null) {
            configsRepository.loadConfig(configInfo)
        } else {
            createDefaultConfig()
        }

        _config.value = cfg
        applyConfigToState(cfg)
    }

    private fun createDefaultConfig(): EvalRunConfig {
        val defaultProvider = _availableProviders.value.firstOrNull()
        val defaultModel = defaultProvider?.let {
            selectDefaultModel(it, it.availableModels.filter { model -> it.isModelEnabled(model.id) })
        }

        val defaultRunConfig = runCatching {
            RunManager.getInstance(project).selectedConfiguration?.name
        }.getOrNull()

        return EvalRunConfig(
            name = makeSafeConfigName(""),
            description = null,
            datasetName = getLatestModifiedDatasetName(),
            runConfigName = defaultRunConfig.orEmpty(),
            providerInstanceId = defaultProvider?.id,
            modelName = defaultModel?.id ?: "gpt-4o",
            modelParams = mapOf("temperature" to "0"),
            promptTemplate = EvaluationPromptDefaults.DEFAULT_LLM_JUDGE_PROMPT,
            evaluators = listOf(EvaluatorConfig(prompt = EvaluationPromptDefaults.DEFAULT_LLM_JUDGE_PROMPT))
        )
    }

    private fun getLatestModifiedDatasetName(): String? {
        val datasets = inputDatasetsRepo.listDatasets()
        if (datasets.isEmpty()) return null

        val basePath = Paths.get(project.basePath ?: System.getProperty("user.dir"))
            .resolve(".jbeval").resolve("datasets")

        return datasets.maxByOrNull { info ->
            if (info.lastModified > 0) {
                info.lastModified
            } else {
                runCatching {
                    basePath.resolve(info.fileName).toFile().lastModified()
                }.getOrDefault(0L)
            }
        }?.name
    }

    private fun applyConfigToState(cfg: EvalRunConfig) {
        _name.value = cfg.name
        _description.value = cfg.description.orEmpty()
        _selectedDataset.value = resolveDatasetLabel(cfg.datasetName)
        _runConfigName.value = cfg.runConfigName
        _evaluators.value = cfg.evaluators?.takeIf { it.isNotEmpty() }
            ?: listOf(EvaluatorConfig(prompt = cfg.promptTemplate ?: EvaluationPromptDefaults.DEFAULT_LLM_JUDGE_PROMPT))

        _modelParams.value = cfg.modelParams?.map { it.key to it.value } ?: emptyList()

        restoreProviderAndModel(cfg)
        validateEvaluators()
    }

    private fun restoreProviderAndModel(cfg: EvalRunConfig) {
        val provider = cfg.providerInstanceId?.let { id ->
            _availableProviders.value.firstOrNull { it.id == id }
        } ?: _availableProviders.value.firstOrNull()
        if (provider != null) {
            updateProviderModels(provider)
            _selectedProvider.value = provider

            val savedModel = cfg.modelName?.let { modelId ->
                _availableModels.value.firstOrNull { it.id == modelId }
            }
            _selectedModel.value = savedModel ?: selectDefaultModel(provider, _availableModels.value)
        } else {
            _selectedProvider.value = null
            _availableModels.value = emptyList()
            _selectedModel.value = null
        }
    }

    fun setName(value: String) {
        _name.value = value
        validateName(value)
    }

    private fun validateName(name: String) {
        val trimmed = name.trim()

        if (trimmed.isEmpty()) {
            _nameValidationError.value = NameValidationError.INVALID_CHARACTERS
            updateOverallValidation()
            return
        }

        val error = when {
            !isValidConfigName(trimmed) -> NameValidationError.INVALID_CHARACTERS
            isNameTaken(trimmed) -> NameValidationError.ALREADY_EXISTS
            else -> null
        }

        _nameValidationError.value = error
        updateOverallValidation()
    }

    private fun isValidConfigName(name: String): Boolean {
        return name.none { it in "\\/:*?\"<>|" }
    }

    private fun isNameTaken(name: String): Boolean {
        val existing = configsRepository.listConfigs()
        val currentName = selectedInfo?.name
        return existing.any {
            it.name.equals(name, ignoreCase = true) &&
                    !it.name.equals(currentName, ignoreCase = true)
        }
    }

    private fun updateOverallValidation() {
        val nameValid = _nameValidationError.value == null && _name.value.isNotBlank()
        val datasetValid = _selectedDataset.value.isNotBlank()
        val evaluatorsValid = _evaluatorValidation.value.values.all { it }
        _isValid.value = nameValid && datasetValid && evaluatorsValid
    }

    private fun validateEvaluators() {
        val validation = _evaluators.value.mapIndexed { index, evaluator ->
            index to evaluator.validatePrompt()
        }.toMap()

        _evaluatorValidation.value = validation
        updateOverallValidation()
    }

    fun setDescription(value: String) {
        _description.value = value
    }

    fun setSelectedDataset(value: String) {
        _selectedDataset.value = value
        updateOverallValidation()
    }

    fun setRunConfigName(value: String) {
        _runConfigName.value = value
    }

    fun onProviderSelected(provider: ProviderInstance?) {
        val currentConfig = _config.value ?: return

        if (_selectedProvider.value?.id == provider?.id) {
            return
        }

        if (provider == null) {
            clearProviderAndModels()
            _config.value = currentConfig.copy(
                providerType = null,
                providerInstanceId = null,
                modelName = null
            )
        } else {
            updateProviderModels(provider)
            _selectedProvider.value = provider

            val defaultModel = selectDefaultModel(provider, _availableModels.value)
            _selectedModel.value = defaultModel

            _config.value = currentConfig.copy(
                providerType = provider.providerType,
                providerInstanceId = provider.id,
                modelName = defaultModel?.id
            )
        }
    }

    fun setSelectedModel(model: LlmModel?) {
        val currentConfig = _config.value ?: return
        _selectedModel.value = model
        _config.value = currentConfig.copy(modelName = model?.id)
    }

    fun setModelParams(params: List<Pair<String, String>>) {
        val currentConfig = _config.value ?: return

        _modelParams.value = params
        val paramsMap = params
            .mapNotNull { (k, v) -> k.trim().takeIf { it.isNotEmpty() }?.let { it to v } }
            .toMap()
            .takeIf { it.isNotEmpty() }

        _config.value = currentConfig.copy(modelParams = paramsMap)
    }

    fun reloadProviders() {
        val previousProviderId = _selectedProvider.value?.id
        val previousModelId = _selectedModel.value?.id

        scope.launch {
            val providers = settingsService.getInstances().filter { it.hasCredentials() }
            withContext(Dispatchers.Default) {
                _availableProviders.value = providers
                if (_availableProviders.value.isNotEmpty()) {
                    val providerToSelect = _availableProviders.value.firstOrNull { it.id == previousProviderId }
                        ?: _availableProviders.value.first()
                    updateProviderModels(providerToSelect)
                    _selectedProvider.value = providerToSelect

                    val modelToSelect = previousModelId?.let { modelId ->
                        _availableModels.value.firstOrNull { it.id == modelId }
                    } ?: selectDefaultModel(providerToSelect, _availableModels.value)
                    _selectedModel.value = modelToSelect

                    val currentConfig = _config.value
                    if (currentConfig != null) {
                        _config.value = currentConfig.copy(
                            providerType = providerToSelect.providerType,
                            providerInstanceId = providerToSelect.id,
                            modelName = modelToSelect?.id
                        )
                    }
                }
            }
        }
    }

    private fun updateProviderModels(provider: ProviderInstance) {
        if (provider.availableModels.isEmpty()) {
            _isLoadingModels.value = true
            _availableModels.value = emptyList()
            _selectedModel.value = null

            scope.launch {
                try {
                    val models = modelsService.fetchModels(provider)
                    val updatedInstance = provider.copy(availableModels = models)
                    settingsService.updateInstance(updatedInstance)
                    withContext(Dispatchers.Default) {
                        _availableModels.value = models.filter { updatedInstance.isModelEnabled(it.id) }
                        _selectedModel.value = selectDefaultModel(updatedInstance, _availableModels.value)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Default) {
                        _availableModels.value = emptyList()
                        _selectedModel.value = null
                    }
                } finally {
                    withContext(Dispatchers.Default) {
                        _isLoadingModels.value = false
                    }
                }
            }
        } else {
            _availableModels.value = provider.availableModels.filter { provider.isModelEnabled(it.id) }
        }
    }

    private fun clearProviderAndModels() {
        _selectedProvider.value = null
        _availableModels.value = emptyList()
        _selectedModel.value = null
    }

    private fun selectDefaultModel(provider: ProviderInstance, models: List<LlmModel>): LlmModel? {
        if (models.isEmpty()) return null

        models.firstOrNull { it.id == provider.providerType.defaultModel }?.let { return it }

        val preferredModelIds = when (provider.providerType) {
            LlmProviderType.OPENAI,
            LlmProviderType.OPENAI_COMPATIBLE -> listOf("gpt-4o", "gpt-4o-mini", "gpt-4-turbo")
            LlmProviderType.ANTHROPIC -> listOf("claude-3-5-sonnet-20241022", "claude-3-sonnet-20240229")
            LlmProviderType.GEMINI -> listOf("gemini-2.0-flash-exp", "gemini-1.5-pro", "gemini-1.5-flash")
        }

        return preferredModelIds.firstNotNullOfOrNull { preferredId ->
            models.firstOrNull { it.id == preferredId }
        } ?: models.firstOrNull()
    }

    fun addEvaluator() {
        val currentConfig = _config.value ?: return
        val defaultType = "llm judge"
        val defaultPrompt = EvaluationPromptDefaults.DEFAULT_LLM_JUDGE_PROMPT
        val generatedName = generateUniqueName(defaultType)

        val newEvaluator = EvaluatorConfig(
            name = generatedName,
            type = defaultType,
            prompt = defaultPrompt
        )

        val updatedEvaluators = _evaluators.value + newEvaluator
        _evaluators.value = updatedEvaluators
        _config.value = currentConfig.copy(evaluators = updatedEvaluators)
        validateEvaluators()
    }

    private fun generateUniqueName(type: String): String {
        val baseName = EvaluatorConfig.getDefaultNameForType(type)
        val sameTypeCount = _evaluators.value.count { it.name.startsWith(baseName) }
        return "$baseName-${sameTypeCount + 1}"
    }

    fun removeEvaluator(index: Int) {
        if (_evaluators.value.size <= 1) return

        val currentConfig = _config.value ?: return
        val updatedEvaluators = _evaluators.value.toMutableList().apply { removeAt(index) }

        _evaluators.value = updatedEvaluators
        _config.value = currentConfig.copy(evaluators = updatedEvaluators)
        validateEvaluators()
    }

    fun updateEvaluator(index: Int, evaluator: EvaluatorConfig) {
        val currentConfig = _config.value ?: return
        val oldEvaluator = _evaluators.value.getOrNull(index)

        val updatedEvaluator = if (shouldAutoUpdateEvaluatorName(oldEvaluator, evaluator)) {
            evaluator.copy(name = generateUniqueNameForUpdate(evaluator.type, index))
        } else {
            evaluator
        }

        val updatedEvaluators = _evaluators.value.toMutableList().apply { set(index, updatedEvaluator) }
        _evaluators.value = updatedEvaluators
        _config.value = currentConfig.copy(evaluators = updatedEvaluators)
        validateEvaluators()
    }

    private fun shouldAutoUpdateEvaluatorName(old: EvaluatorConfig?, new: EvaluatorConfig): Boolean {
        if (old == null || old.type == new.type) return false
        val oldDefaultName = EvaluatorConfig.getDefaultNameForType(old.type)
        return old.name.matches(Regex("$oldDefaultName-\\d+"))
    }

    private fun generateUniqueNameForUpdate(type: String, currentIndex: Int): String {
        val baseName = EvaluatorConfig.getDefaultNameForType(type)
        val sameTypeCount = _evaluators.value.filterIndexed { idx, evaluator ->
            idx != currentIndex && evaluator.name.startsWith(baseName)
        }.count()
        return "$baseName-${sameTypeCount + 1}"
    }

    fun save(): String {
        val currentConfig = _config.value ?: return ""
        val newName = makeSafeConfigName(_name.value)
        if (!isValidConfigName(newName)) {
            _nameValidationError.value = NameValidationError.INVALID_CHARACTERS
            return ""
        }
        if (isNameTaken(newName)) {
            _nameValidationError.value = NameValidationError.ALREADY_EXISTS
            return ""
        }

        val finalConfig = currentConfig.copy(
            name = newName,
            description = _description.value.takeIf { it.isNotBlank() },
            datasetName = resolveDatasetPath(_selectedDataset.value),
            runConfigName = _runConfigName.value,
            evaluators = _evaluators.value,
            providerInstanceId = _selectedProvider.value?.id,
            providerType = _selectedProvider.value?.providerType,
            modelName = _selectedModel.value?.id
        )

        renameConfigIfNeeded(newName)
        selectedInfo = configsRepository.createOrUpdateConfig(newName, finalConfig)
        return newName
    }

    private fun renameConfigIfNeeded(newName: String): ConfigInfo? {
        return selectedInfo?.let { info ->
            if (newName != info.name) {
                configsRepository.renameConfig(info.name, newName)
            } else {
                info
            }
        }
    }

    private fun resolveDatasetLabel(datasetName: String?): String {
        val label = inputDatasetsRepo.resolveDataset(datasetName)?.name.orEmpty()
        return if (_availableDatasets.value.contains(label)) label else ""
    }

    private fun makeSafeConfigName(candidateName: String): String {
        val sanitized = candidateName.trim().ifBlank { "config" }
        if (selectedInfo != null && selectedInfo?.name == sanitized) {
            return sanitized
        }
        val existingConfigs = configsRepository.listConfigs()
        val existingNames = existingConfigs.map { it.name }.toSet()
        if (!existingNames.contains(sanitized)) {
            return sanitized
        }
        var c = 1
        while (true) {
            val candidate = "${sanitized}_$c"
            if (!existingNames.contains(candidate)) {
                return candidate
            }
            c++
        }
    }

    private fun resolveDatasetPath(datasetLabel: String): String? {
        if (datasetLabel.isBlank()) return null
        val info = inputDatasetsRepo.listDatasets().firstOrNull { it.name == datasetLabel } ?: return null
        val base = Paths.get(project.basePath ?: System.getProperty("user.dir"))
            .resolve(".jbeval").resolve("datasets")
        return base.resolve(info.fileName).toString()
    }
}