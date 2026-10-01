package com.intellij.aiplayground.models.settings

import com.intellij.aiplayground.models.LlmModelId
import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.LlmProviderId
import com.intellij.aiplayground.models.LlmProviderInstance
import com.intellij.aiplayground.models.LlmProviderInstanceId
import com.intellij.aiplayground.models.extension.LlmServiceExtension
import com.intellij.aiplayground.models.extension.LlmServiceExtension.Companion.getProviderExtension
import com.intellij.aiplayground.models.settings.PlaygroundSettings.PlaygroundSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.intellij.util.xmlb.annotations.XMap
import java.util.UUID

/**
 * Service for persisting AI Playground settings (application-level configuration)
 */
@Service
@State(
  name = "com.intellij.aiplayground.settings.PlaygroundSettingsState",
  storages = [Storage("AIPlaygroundAppSettings.xml")]
)
class PlaygroundSettings : SerializablePersistentStateComponent<PlaygroundSettingsState>(PlaygroundSettingsState()) {

  var showStartupNotification: Boolean
    get() {
      return state.showStartupNotification ?: true
    }
    set(value) {
      updateState {
        it.copy(showStartupNotification = value)
      }
    }

  fun getDisabledModels(providerId: LlmProviderId): Set<LlmModelId> {
    return state.disabledModelsByProvider[providerId.id]
             ?.split(',')
             ?.mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } }
             ?.map { LlmModelId(it) }
             ?.toSet()
           ?: emptySet()
  }

  fun setDisabledModels(providerId: LlmProviderId, models: Set<LlmModelId>) {
    updateState { st ->
      st.copy(
        disabledModelsByProvider = st.disabledModelsByProvider.toMutableMap().apply {
          if (models.isEmpty()) remove(providerId.id)
          else put(providerId.id, models.joinToString(",") { it.id })
        }
      )
    }
    ApplicationManager.getApplication().messageBus.syncPublisher(PLAYGROUND_SETTINGS_UPDATES_TOPIC).settingsUpdated()
  }

  fun updateProviderSettings(settings: List<LlmProviderInstance>) {
    val settingsById = settings.associateBy { it.id }
    updateState { oldSettings ->
      oldSettings.providers.forEach {
        LlmServiceExtension.getProviderById(LlmProviderId(it.providerId))?.let { provider ->
          val instanceId = LlmProviderInstanceId(it.id)
          if (!settingsById.contains(instanceId)) {
            getProviderExtension(provider).removeSettings(instanceId)
          }
        }
      }
      oldSettings.copy(providers = settings.map {
        getProviderExtension(it.provider).updateSettings(it)
      })
    }
    ApplicationManager.getApplication().messageBus.syncPublisher(PLAYGROUND_SETTINGS_UPDATES_TOPIC).settingsUpdated()
  }

  // does not account for availability!
  fun getConfiguredProviders(): List<LlmProviderInstance> {
    return state.providers.mapNotNull {
      LlmServiceExtension.getProviderById(LlmProviderId(it.providerId))?.let { provider ->
        LlmProviderInstance(
          LlmProviderInstanceId(it.id),
          provider,
          getProviderExtension(provider).createSettings(it))
      }
    }
  }

  fun createProviderSettings(provider: LlmProvider): LlmProviderSettings<*> {
    return getProviderExtension(provider).createSettings(
      ProviderSettings("", provider.id.id, true, displayName = provider.displayName)
    )
  }

  override fun loadState(state: PlaygroundSettingsState) {
    super.loadState(state)
    updateState { oldState ->
      oldState.copy(
        providers = state.providers.map {
          fixName(fixId(it))
        }
      )
    }
    ApplicationManager.getApplication().messageBus.syncPublisher(PLAYGROUND_SETTINGS_UPDATES_TOPIC).settingsUpdated()
  }

  private fun fixName(settings: ProviderSettings): ProviderSettings = if (settings.displayName.isNullOrBlank()) {
    LlmServiceExtension.getProviderById(LlmProviderId(settings.providerId))?.let { provider ->
      settings.copy(displayName = provider.displayName)
    } ?: settings.copy(displayName = settings.providerId)
  }
  else {
    settings
  }

  private fun fixId(settings: ProviderSettings): ProviderSettings = if (settings.id.isEmpty()) {
    settings.copy(id = UUID.randomUUID().toString())
  }
  else {
    settings
  }

  fun addProviderInstance(instance: LlmProviderInstance) {
    updateState { state ->
      state.copy(
        providers = state.providers + getProviderExtension(instance.provider).updateSettings(instance)
      )
    }
    ApplicationManager.getApplication().messageBus.syncPublisher(PLAYGROUND_SETTINGS_UPDATES_TOPIC).settingsUpdated()
  }

  fun addProviderInstanceInBeginning(instance: LlmProviderInstance) {
    updateState { state ->
      state.copy(
        providers = listOf(getProviderExtension(instance.provider).updateSettings(instance)) + state.providers
      )
    }
    ApplicationManager.getApplication().messageBus.syncPublisher(PLAYGROUND_SETTINGS_UPDATES_TOPIC).settingsUpdated()
  }

  fun updateRecentModel(instanceId: LlmProviderInstanceId, modelId: LlmModelId) {
    updateState { state ->
      state.copy(
        recentModels = state.recentModels.toMutableList().apply {
          removeIf { it.instanceId == instanceId.id && it.modelId == modelId.id }
          add(0, RecentModel(instanceId.id, modelId.id))
        }.take(10)
      )
    }
    ApplicationManager.getApplication().messageBus.syncPublisher(PLAYGROUND_SETTINGS_UPDATES_TOPIC).settingsUpdated()
  }

  fun getRecentModels(): List<Pair<LlmProviderInstanceId, LlmModelId>> {
    return state.recentModels.map {
      LlmProviderInstanceId(it.instanceId) to LlmModelId(it.modelId)
    }
  }

  fun removeProviderInstance(id: LlmProviderInstanceId) {
    updateState { state ->
      state.copy(
        providers = state.providers.filterNot { it.id == id.id }
      )
    }
    ApplicationManager.getApplication().messageBus.syncPublisher(PLAYGROUND_SETTINGS_UPDATES_TOPIC).settingsUpdated()
  }

  data class PlaygroundSettingsState(
    @JvmField
    @Attribute
    val settingsVersion: String = "4",
    @JvmField
    @XCollection(propertyElementName = "providers", elementName = "provider")
    val providers: List<ProviderSettings> = emptyList(),
    @XCollection(propertyElementName = "recentModels", elementName = "model")
    val recentModels: List<RecentModel> = emptyList(),
    @XMap(entryTagName = "disabledModelsByProvider", keyAttributeName = "providerId", valueAttributeName = "modelsCsv")
    val disabledModelsByProvider: Map<String, String> = emptyMap(),
    @JvmField
    @Attribute
    val showStartupNotification: Boolean? = true,
  )

  @Tag("provider")
  data class ProviderSettings(
    @JvmField @Attribute val id: String = "",
    @JvmField @Attribute val providerId: String = "",
    @JvmField @Attribute val enabled: Boolean = true,
    @JvmField @Attribute val displayName: String? = null,
    @JvmField @XMap(entryTagName = "property",
                    keyAttributeName = "name",
                    valueAttributeName = "value") val properties: Map<String, String> = emptyMap(),
  )

  @Tag("model")
  data class RecentModel(
    @JvmField @Attribute val instanceId: String = "",
    @JvmField @Attribute val modelId: String = "",
  )


  interface PlaygroundSettingsUpdatesListener {
    fun settingsUpdated()
  }

  companion object {
    val PLAYGROUND_SETTINGS_UPDATES_TOPIC: Topic<PlaygroundSettingsUpdatesListener> = Topic.create(
      "com.intellij.aiplayground.settings.api.keys.updates",
      PlaygroundSettingsUpdatesListener::class.java
    )
  }
}