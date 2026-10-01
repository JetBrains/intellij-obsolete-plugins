package com.intellij.aidebugger.evaluation.settings

import com.intellij.aidebugger.evaluation.models.llm.LlmProviderType
import com.intellij.aidebugger.evaluation.settings.models.LlmModel
import com.intellij.aidebugger.evaluation.settings.models.ProviderInstance
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic

/**
 * Service for managing AI Toolkit LLM provider credential instances.
 * Supports multiple credential sets per provider type (e.g., "OpenAI Prod", "OpenAI Dev").
 * Stores API keys securely using PasswordSafe.
 */
@Service(Service.Level.APP)
@State(
    name = "AIToolkitSettings",
    storages = [Storage("ai-toolkit-settings.xml")]
)
class AIToolkitSettingsService : PersistentStateComponent<AIToolkitSettingsService.State> {

    private var myState = State()

    data class State(
        var instances: MutableList<InstanceData> = mutableListOf()
    )

    /**
     * Serializable instance data (without API keys)
     */
    data class InstanceData(
        var id: String = "",
        var providerType: String = "",
        var name: String = "",
        var baseUrl: String = "",
        var disabledModels: Set<String> = emptySet(),
        var availableModels: List<ModelData> = emptyList(),
    )

    /**
     * Serializable model data
     */
    data class ModelData(
        var id: String = "",
        var displayName: String = "",
    )

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    /**
     * Get all provider instances with credentials loaded from PasswordSafe
     */
    fun getInstances(): List<ProviderInstance> {
        return myState.instances
            .filter { it.id.isNotBlank() && it.providerType.isNotBlank() && it.name.isNotBlank() }
            .mapNotNull { data ->
                runCatching {
                    ProviderInstance(
                        id = data.id,
                        providerType = LlmProviderType.valueOf(data.providerType),
                        name = data.name,
                        apiKey = getApiKey(data.id) ?: "",
                        baseUrl = data.baseUrl,
                        disabledModels = data.disabledModels,
                        availableModels = data.availableModels.map { modelData ->
                            LlmModel(
                                id = modelData.id,
                                displayName = modelData.displayName,
                                providerType = LlmProviderType.valueOf(data.providerType)
                            )
                        }
                    )
                }.getOrNull()
            }
    }

    /**
     * Save provider instances (credentials stored in PasswordSafe)
     */
    fun saveInstances(instances: List<ProviderInstance>) {
        myState.instances.clear()
        myState.instances.addAll(instances.map { instance ->
            // Store API key in PasswordSafe
            setApiKey(instance.id, instance.apiKey)

            // Store non-sensitive data in XML
            InstanceData(
                id = instance.id,
                providerType = instance.providerType.name,
                name = instance.name,
                baseUrl = instance.baseUrl,
                disabledModels = instance.disabledModels,
                availableModels = instance.availableModels.map { model ->
                    ModelData(
                        id = model.id,
                        displayName = model.displayName
                    )
                }
            )
        })
        ApplicationManager.getApplication().messageBus.syncPublisher(AI_TOOLKIT_SETTINGS_UPDATE).settingsUpdated()
    }

    /**
     * Add a new provider instance
     */
    fun addInstance(instance: ProviderInstance) {
        val instances = getInstances().toMutableList()
        instances.add(instance)
        saveInstances(instances)
    }

    /**
     * Update an existing provider instance
     */
    fun updateInstance(instance: ProviderInstance) {
        val instances = getInstances().toMutableList()
        val index = instances.indexOfFirst { it.id == instance.id }
        if (index != -1) {
            instances[index] = instance
            saveInstances(instances)
        }
    }

    /**
     * Remove a provider instance
     */
    fun removeInstance(id: String) {
        val instances = getInstances().toMutableList()
        instances.removeIf { it.id == id }
        removeApiKey(id)
        saveInstances(instances)
    }

    /**
     * Get credentials for a specific provider type (uses first matching instance)
     */
    fun getCredentials(providerType: LlmProviderType): ProviderInstance? {
        return getInstances().firstOrNull { it.providerType == providerType && it.hasCredentials() }
    }

    /**
     * Get credentials for a specific instance by name
     */
    fun getCredentialsByName(name: String): ProviderInstance? {
        return getInstances().firstOrNull { it.name == name && it.hasCredentials() }
    }

    /**
     * Get credentials for a specific instance by id
     */
    fun getCredentialsById(id: String?): ProviderInstance? {
        if (id == null) return null
        return getInstances().firstOrNull { it.id == id && it.hasCredentials() }
    }

    fun getCredentialsProviderType(providerType: LlmProviderType?): ProviderInstance? {
        if (providerType == null) return null
        return getInstances().firstOrNull { it.providerType == providerType && it.hasCredentials() }
    }

    private fun getApiKey(instanceId: String): String? {
        val credentialAttributes = createCredentialAttributes(instanceId)
        return PasswordSafe.instance.getPassword(credentialAttributes)
    }

    private fun setApiKey(instanceId: String, apiKey: String) {
        if (apiKey.isBlank()) {
            removeApiKey(instanceId)
            return
        }
        val credentialAttributes = createCredentialAttributes(instanceId)
        val credentials = Credentials(instanceId, apiKey)
        PasswordSafe.instance.set(credentialAttributes, credentials)
    }

    private fun removeApiKey(instanceId: String) {
        val credentialAttributes = createCredentialAttributes(instanceId)
        PasswordSafe.instance.set(credentialAttributes, null)
    }

    private fun createCredentialAttributes(instanceId: String): CredentialAttributes {
        return CredentialAttributes(
            generateServiceName("AIAgentsDebugger", instanceId)
        )
    }

    interface AIToolkitSettingUpdatesListener {
        fun settingsUpdated()
    }

    companion object {
        val AI_TOOLKIT_SETTINGS_UPDATE: Topic<AIToolkitSettingUpdatesListener> = Topic.create(
            "com.intellij.aidebugger.settings.updates",
            AIToolkitSettingUpdatesListener::class.java
        )
    }
}
