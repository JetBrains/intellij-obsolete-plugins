package com.intellij.aiplayground.models

/**
 * Interface for securely storing and retrieving credentials
 */
interface CredentialStorage {
    /**
     * Stores a credential securely
     */
    fun storeCredential(key: String, value: String)

  /**
     * Retrieves a credential by key
     */
    fun retrieveCredential(key: String): String?

  /**
     * Removes a credential by key
     */
    fun removeCredential(key: String): Boolean

  /**
     * Gets the API key for a provider
     */
  fun getProviderApiKey(providerId: LlmProviderId): String? = retrieveCredential("${providerId.id}.apiKey")

  fun getInstanceApiKey(instanceId: LlmProviderInstanceId): String? = retrieveCredential("${instanceId.id}.apiKey")

  /**
     * Stores the API key for a provider
     */
  fun storeProviderApiKey(providerId: LlmProviderId, apiKey: String) = storeCredential("${providerId.id}.apiKey", apiKey)

  fun storeInstanceApiKey(instanceId: LlmProviderInstanceId, apiKey: String) = storeCredential("${instanceId.id}.apiKey", apiKey)

  /**
     * Removes the API key for a provider
     */
  fun removeProviderApiKey(providerId: LlmProviderId): Boolean = removeCredential("${providerId.id}.apiKey")

  fun removeInstanceApiKey(instanceId: LlmProviderInstanceId): Boolean = removeCredential("${instanceId.id}.apiKey")

}