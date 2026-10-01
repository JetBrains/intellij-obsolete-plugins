package com.intellij.aiplayground.models.settings

import com.intellij.aiplayground.models.CredentialStorage
import com.intellij.aiplayground.models.LlmProviderId
import com.intellij.aiplayground.models.LlmProviderInstanceId
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.util.messages.Topic


/**
 * Application level implementation of SettingsManagerService
 *
 * TODO: all operations with credentialStorage must be executed on BGT, not on EDT!
 */
@Service
class ApplicationSettingsManagerService {

  private val credentialStorage: CredentialStorage by lazy {
    PasswordSafeCredentialStorage()
  }

  private val appLevelPropertiesComponent: PropertiesComponent by lazy {
    PropertiesComponent.getInstance()
  }

  fun storeProviderApiKey(providerId: LlmProviderId, apiKey: String) {
    // Delegate to the credential storage
    credentialStorage.storeProviderApiKey(providerId, apiKey)
    appLevelPropertiesComponent.setValue(getPropertyKeyApiKeyExistenceFlag(providerId.id), true)
    ApplicationManager.getApplication().messageBus.syncPublisher(API_KEYS_UPDATES_TOPIC).keyAdded(providerId)
  }

  fun storeInstanceApiKey(instanceId: LlmProviderInstanceId, apiKey: String) {
    // Delegate to the credential storage
    credentialStorage.storeInstanceApiKey(instanceId, apiKey)
    ApplicationManager.getApplication().messageBus.syncPublisher(API_KEYS_UPDATES_TOPIC).keyAdded(instanceId)
  }

  fun getProviderApiKey(providerId: LlmProviderId): String? {
    // Delegate to the credential storage
    return credentialStorage.getProviderApiKey(providerId)
  }

  fun checkProviderApiKeyExists(providerId: LlmProviderId): Boolean {
    return appLevelPropertiesComponent.getBoolean(getPropertyKeyApiKeyExistenceFlag(providerId.id))
  }

  fun getInstanceApiKey(instanceId: LlmProviderInstanceId): String? {
    // Delegate to the credential storage
    return credentialStorage.getInstanceApiKey(instanceId)
  }

  fun removeProviderApiKey(providerId: LlmProviderId) {
    // Delegate to the credential storage
    credentialStorage.removeProviderApiKey(providerId)
    appLevelPropertiesComponent.setValue(getPropertyKeyApiKeyExistenceFlag(providerId.id), false)
    ApplicationManager.getApplication().messageBus.syncPublisher(API_KEYS_UPDATES_TOPIC).keyRemoved(providerId)
  }

  fun removeInstanceApiKey(instanceId: LlmProviderInstanceId) {
    // Delegate to the credential storage
    credentialStorage.removeInstanceApiKey(instanceId)
    ApplicationManager.getApplication().messageBus.syncPublisher(API_KEYS_UPDATES_TOPIC).keyRemoved(instanceId)
  }

  private fun getPropertyKeyApiKeyExistenceFlag(providerId: String): String {
    return "API_KEY_EXISTS_$providerId"
  }

  interface APIKeysUpdatesListener {
    fun keyAdded(providerId: LlmProviderId)
    fun keyAdded(instanceId: LlmProviderInstanceId)
    fun keyRemoved(providerId: LlmProviderId)
    fun keyRemoved(providerId: LlmProviderInstanceId)
  }

  companion object {
    val API_KEYS_UPDATES_TOPIC: Topic<APIKeysUpdatesListener> = Topic.create(
      "com.intellij.aiplayground.models.settings.api.keys.updates",
      APIKeysUpdatesListener::class.java
    )
  }
}