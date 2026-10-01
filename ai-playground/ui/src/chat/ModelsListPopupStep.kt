package com.intellij.aiplayground.ui.chat

import com.intellij.aiplayground.models.LlmModel
import com.intellij.aiplayground.models.LlmProviderInstance
import com.intellij.aiplayground.models.LlmServiceManager
import com.intellij.aiplayground.models.settings.PlaygroundSettings
import com.intellij.aiplayground.ui.AIPlaygroundIconsHelper
import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.aiplayground.ui.chat.ModelsListPopupStep.ListItem
import com.intellij.aiplayground.ui.chat.ModelsListPopupStep.ListItem.ModelListItem
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.ListSeparator
import com.intellij.openapi.ui.popup.MnemonicNavigationFilter
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.SpeedSearchFilter
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.popup.list.FilterableListPopupStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.Icon

@OptIn(ExperimentalCoroutinesApi::class)
class ModelsListPopupStep(
  private val project: Project,
  parentScope: CoroutineScope,
  private val chatViewModel: ChatViewModel,
) : FilterableListPopupStep<ListItem> {

  private var models = listOf<Pair<LlmProviderInstance, LlmModel>>()

  private var recentModels = listOf<Pair<LlmProviderInstance, LlmModel>>()

  private val listeners = mutableListOf<ListPopupStep.ListPopupModelListener>()

  private val coroutineScope = parentScope.childScope("ChatsListPopupStep")

  private val filter = MutableStateFlow<String?>(null)

  private var finalRunnable: Runnable? = null

  init {
    coroutineScope.launch {
      val manager = LlmServiceManager.getInstance(project)
      manager.configuredProvidersState.map {
        combine(it.map { instance ->
          manager.getSupportedModels(instance)
            .map { it.map { instance to it } }
        }
        ) { receivedModels -> receivedModels.toList() }
      }.flattenConcat().collectLatest { receivedModels ->
        models = receivedModels.flatten()
        recentModels = service<PlaygroundSettings>().getRecentModels().mapNotNull { recentModel ->
          models.firstOrNull { model -> model.first.id == recentModel.first && model.second.id == recentModel.second }
        }
        withContext(Dispatchers.EDT) {
          fireUpdate()
        }
      }
    }
    coroutineScope.launch {
      filter.collectLatest {
        withContext(Dispatchers.EDT) {
          fireUpdate()
        }
      }
    }
  }

  private fun fireUpdate() {
    listeners.forEach { it.onModelChanged() }
  }

  override fun getValues(): List<ListItem> {
    val action = ActionManager.getInstance().getAction("AIPlayground.ManageProviders")

    val result = mutableListOf<ListItem>()

    // Recent models section (flat, capped at 5)
    val recentItems = applyFilter(recentModels).take(5).toList()
    recentItems.forEachIndexed { i, it ->
      result.add(ModelListItem(it.first, it.second, if (i == 0) ListSeparator(AIPlaygroundUIBundle.message("separator.recent.models")) else null))
    }

    // Provider groups section
    val filteredModels = applyFilter(models).toList()
    val groupedByProvider = filteredModels.groupBy { it.first.id }

    var isFirstProvider = true
    for ((_, providerModels) in groupedByProvider) {
      val instance = providerModels.first().first
      val separator = if (isFirstProvider) ListSeparator(AIPlaygroundUIBundle.message("separator.all.models")) else null
      isFirstProvider = false

      if (providerModels.size == 1) {
        // Single model for provider — show directly as a flat item
        result.add(ModelListItem(instance, providerModels.first().second, separator))
      }
      else {
        // Multiple models — show provider as expandable group
        result.add(ListItem.ProviderListItem(instance, providerModels.map { it.second }, separator))
      }
    }

    // Manage providers at bottom
    result.add(ListItem.ActionListItem(action, ListSeparator()))

    return result
  }

  private fun applyFilter(models: List<Pair<LlmProviderInstance, LlmModel>>): Sequence<Pair<LlmProviderInstance, LlmModel>> {
    val disabledByProvider: (LlmProviderInstance) -> Set<String> = { instance ->
      service<PlaygroundSettings>().getDisabledModels(instance.provider.id).map { it.id }.toSet()
    }
    val baseSeq = models.asSequence()
      .filter { pair ->
        val disabled = disabledByProvider(pair.first)
        !disabled.contains(pair.second.id.id)
      }
    return filter.value?.let { f ->
      baseSeq.filter { ("${it.first.settings.displayName} - ${it.second.displayName}").contains(f, ignoreCase = true) }
    } ?: baseSeq
  }

  override fun isSelectable(value: ListItem?): Boolean = true

  override fun getIconFor(value: ListItem): Icon? = when (value) {
    is ModelListItem -> AIPlaygroundIconsHelper.providerName2Icon(value.instance.provider.id.id)
    is ListItem.ProviderListItem -> AIPlaygroundIconsHelper.providerName2Icon(value.instance.provider.id.id)
    else -> null
  }

  @Suppress("DialogTitleCapitalization")
  override fun getTextFor(value: ListItem): @NlsContexts.ListItem String = when (value) {
    is ListItem.ActionListItem -> value.action.templateText ?: ""
    is ModelListItem -> getDisplayName(value.instance, value.model)
    is ListItem.ProviderListItem -> value.instance.settings.displayName ?: value.instance.provider.id.id
  }

  private fun getDisplayName(instance: LlmProviderInstance, model: LlmModel): @NlsSafe String = "${instance.settings.displayName} - ${truncateModelName(model.displayName)}"

  private fun truncateModelName(name: String, max: Int = 35): String =
    if (name.length <= max) name else name.take(max) + "…"

  override fun getSeparatorAbove(value: ListItem): ListSeparator? = value.splitterAbove

  override fun getDefaultOptionIndex(): Int = 0

  override fun getTitle(): @NlsContexts.PopupTitle String = AIPlaygroundUIBundle.message("popup.title.add.model")

  override fun onChosen(selectedValue: ListItem, finalChoice: Boolean): PopupStep<*>? {
    when (selectedValue) {
      is ListItem.ActionListItem -> {
        finalRunnable = Runnable {
          ActionManager.getInstance().tryToExecute(selectedValue.action, null, null, null, true)
        }
      }
      is ModelListItem -> chatViewModel.addActiveModel(selectedValue.instance, selectedValue.model.id)
      is ListItem.ProviderListItem -> {
        // Return sub-step with models for this provider
        return ProviderModelsSubStep(selectedValue.instance, selectedValue.models, chatViewModel)
      }
    }
    return null
  }

  override fun hasSubstep(selectedValue: ListItem): Boolean = selectedValue is ListItem.ProviderListItem

  override fun canceled() {
    coroutineScope.cancel()
  }

  override fun isMnemonicsNavigationEnabled(): Boolean = false

  override fun getMnemonicNavigationFilter(): MnemonicNavigationFilter<ListItem?>? = null

  override fun isSpeedSearchEnabled(): Boolean = true

  override fun getSpeedSearchFilter(): SpeedSearchFilter<ListItem?>? = null

  override fun isAutoSelectionEnabled(): Boolean = false

  override fun getFinalRunnable(): Runnable? = finalRunnable

  override fun addListener(listener: ListPopupStep.ListPopupModelListener) {
    listeners.add(listener)
    fireUpdate()
  }

  override fun removeListener(listener: ListPopupStep.ListPopupModelListener) {
    listeners.remove(listener)
  }

  override fun updateFilter(f: String?) {
    filter.value = f
  }

  sealed class ListItem(val splitterAbove: ListSeparator? = null) {
    class ModelListItem(val instance: LlmProviderInstance, val model: LlmModel, splitterAbove: ListSeparator? = null) : ListItem(splitterAbove)
    class ProviderListItem(val instance: LlmProviderInstance, val models: List<LlmModel>, splitterAbove: ListSeparator? = null) : ListItem(splitterAbove)
    class ActionListItem(val action: AnAction, splitterAbove: ListSeparator? = null) : ListItem(splitterAbove)
  }

}

/**
 * Sub-popup step showing models for a single provider.
 */
private class ProviderModelsSubStep(
  private val instance: LlmProviderInstance,
  models: List<LlmModel>,
  private val chatViewModel: ChatViewModel,
) : com.intellij.openapi.ui.popup.util.BaseListPopupStep<LlmModel>(instance.settings.displayName ?: instance.provider.id.id, models) {

  override fun getTextFor(value: LlmModel): String = value.displayName

  override fun getIconFor(value: LlmModel): Icon? = AIPlaygroundIconsHelper.providerName2Icon(instance.provider.id.id)

  override fun onChosen(selectedValue: LlmModel, finalChoice: Boolean): PopupStep<*>? {
    chatViewModel.addActiveModel(instance, selectedValue.id)
    return PopupStep.FINAL_CHOICE
  }
}
