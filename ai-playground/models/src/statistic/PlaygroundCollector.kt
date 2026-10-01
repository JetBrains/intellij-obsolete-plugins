package com.intellij.aiplayground.models.statistic

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.LlmProviderId
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFileWithId


enum class CreateChatPlace {
  TOOL_WINDOW_EMPTY_STATE, TOOL_WINDOW_ACTION, CODE_ENTRY
}

enum class MessageType {
  USER, ASSISTANT, SYSTEM
}

enum class Parameter {
  TOP_P, MAX_TOKENS, TEMPERATURE,
}

private val ALLOWED_MODELS = PlaygroundCollector::class.java.classLoader
                               .getResourceAsStream("statistic/models-whitelist.txt")
                               ?.bufferedReader()
                               ?.readLines()
                             ?: emptyList()


private class PrefixTrieNode {
  val children = mutableMapOf<Char, PrefixTrieNode>()
  var isEndOfWord = false
  var word: String? = null
}

private class PrefixTrie {
  private val root = PrefixTrieNode()

  fun insert(word: String) {
    var current = root
    for (char in word) {
      current = current.children.getOrPut(char) { PrefixTrieNode() }
    }
    current.isEndOfWord = true
    current.word = word
  }

  fun findLongestPrefix(input: String): String? {
    var current = root
    var longestMatch: String? = null

    for (char in input) {
      current = current.children[char] ?: break
      if (current.isEndOfWord) {
        longestMatch = current.word
      }
    }

    return longestMatch
  }
}

private val prefixTrie = PrefixTrie().apply {
  ALLOWED_MODELS.forEach { insert(it) }
}

private fun findLongestModelName(input: String): String {
  return prefixTrie.findLongestPrefix(input) ?: "unknown-model"
}

object PlaygroundCollector : CounterUsagesCollector() {

  private val GROUP = EventLogGroup("aiplayground", 4)
  override fun getGroup(): EventLogGroup = GROUP

  private val ALLOWED_PROVIDERS = listOf(
    "openai",
    "openai_compatible",
    "anthropic",
    "mistral",
    "ollama",
    "aiassistant",
    "deepseek",
    "gemini"
  )

  private val CHAT_CREATED = GROUP.registerEvent(
    "chat_created",
    EventFields.Enum<CreateChatPlace>("place")
  )

  // region chat

  fun logChatCreated(place: CreateChatPlace) {
    CHAT_CREATED.log(place)
  }

  private val CHAT_OPENED = GROUP.registerEvent(
    "chat_opened",
  )

  fun logEditorOpened() {
    CHAT_OPENED.log()
  }

  private val CHAT_CLOSED = GROUP.registerEvent(
    "chat_closed",
  )

  fun logEditorClosed() {
    CHAT_CLOSED.log()
  }

  private val CHAT_REMOVED = GROUP.registerEvent(
    "chat_removed",
  )

  fun logChatRemoved() {
    CHAT_REMOVED.log()
  }

  private val TOOLWINDOW_OPENED = GROUP.registerEvent("toolwindow_opened")

  fun logToolWindowOpened() {
    TOOLWINDOW_OPENED.log()
  }

  private val PROMPT_SUBMITTED = GROUP.registerEvent(
    "prompt_submitted",
    EventFields.Int("model_count"),
    EventFields.Boolean("has_system_prompt"),
  )

  fun logPromptSubmitted(modelCount: Int, hasSystemPrompt: Boolean) {
    PROMPT_SUBMITTED.log(modelCount, hasSystemPrompt)
  }

  private val MESSAGE_EDITED = GROUP.registerEvent(
    "message_edited",
    EventFields.Enum<MessageType>("target_type"),
  )

  fun logMessageEdited(type: MessageType) {
    MESSAGE_EDITED.log(type)
  }

  private val MESSAGE_REGENERATED = GROUP.registerEvent(
    "message_regenerated"
  )

  fun logMessageRegenerated() {
    MESSAGE_REGENERATED.log()
  }

  private val MESSAGE_COMPLETED = GROUP.registerEvent(
    "message_completed",
    EventFields.String("provider_id", ALLOWED_PROVIDERS),
    EventFields.String("model_id", ALLOWED_MODELS),
    EventFields.Int("tokens_consumed"),
  )

  fun logMessageCompleted(providerId: String, modelId: String, tokensConsumed: Int) {
    MESSAGE_COMPLETED.log(providerId, findLongestModelName(modelId), tokensConsumed)
  }

  private val PROMPT_PARAMETERS_SET = GROUP.registerEvent(
    "prompt_parameters_set",
    EventFields.Enum<Parameter>("parameter"),
  )

  fun logPromptParametersSet(parameter: Parameter) {
    PROMPT_PARAMETERS_SET.log(parameter)
  }

  private val MODEL_ADDED = GROUP.registerEvent(
    "model_added",
    EventFields.String("provider_id", ALLOWED_PROVIDERS),
    EventFields.String("model_id", ALLOWED_MODELS),
  )

  fun logModelAdded(providerId: String, modelId: String) {
    MODEL_ADDED.log(providerId, findLongestModelName(modelId))
  }

  private val MODEL_REMOVED = GROUP.registerEvent(
    "model_removed",
    EventFields.String("provider_id", ALLOWED_PROVIDERS),
    EventFields.String("model_id", ALLOWED_MODELS),
  )

  fun logModelRemoved(providerId: String, modelId: String) {
    MODEL_REMOVED.log(providerId, findLongestModelName(modelId))
  }

  private val CHAT_HISTORY_CLEARED = GROUP.registerEvent("chat_history_cleared")

  fun logChatHistoryCleared() {
    CHAT_HISTORY_CLEARED.log()
  }

  private val CHAT_RENAMED = GROUP.registerEvent("chat_renamed")

  fun logChatRenamed() {
    CHAT_RENAMED.log()
  }

  private val ADD_PROVIDER_DIALOG_OPEN = GROUP.registerEvent(
    "add_provider_dialog_open"
  )

  fun logAddProviderDialogOpened() {
    ADD_PROVIDER_DIALOG_OPEN.log()
  }

  private val PROVIDER_ADDED_USING_DIALOG = GROUP.registerEvent(
    "provider",
    EventFields.String("provider_id", ALLOWED_PROVIDERS),
  )

  fun logProviderAddedUsingDialog(provider: LlmProvider) {
    PROVIDER_ADDED_USING_DIALOG.log(provider.id.id)
  }

  private val MANAGE_PROVIDERS_OPENED = GROUP.registerEvent("manage_providers_opened")

  fun logManageProvidersOpened() {
    MANAGE_PROVIDERS_OPENED.log()
  }

  // endregion chat

  // region inlay

  private val INLAY_HINT_SHOWN = GROUP.registerEvent("inlay_hint_shown")

  private val shownFilesIds = mutableSetOf<Int>()

  fun logInlayHintShown(editor: Editor) {
    val document: Document = editor.document
    (FileDocumentManager.getInstance().getFile(document) as? VirtualFileWithId)?.let {
      if (shownFilesIds.add(it.id)) {
        INLAY_HINT_SHOWN.log()
      }
    }
  }

  private val INLAY_HINT_CLICKED = GROUP.registerEvent("inlay_hint_click")

  fun logInlayHintClicked() {
    INLAY_HINT_CLICKED.log()
  }

  private val INLAY_HINT_RIGHT_CLICKED = GROUP.registerEvent("inlay_hint_right_click")

  fun logInlayHintRightClicked() {
    INLAY_HINT_RIGHT_CLICKED.log()
  }

  private val INLAY_HINT_ENABLED= GROUP.registerEvent("inlay_hint_enabled")

  fun logInlayHintEnabled() {
    INLAY_HINT_ENABLED.log()
  }

  private val INLAY_HINT_DISABLED= GROUP.registerEvent("inlay_hint_disabled")

  fun logInlayHintDisabled() {
    INLAY_HINT_DISABLED.log()
  }

  private val INLAY_HINT_HOVERED = GROUP.registerEvent("inlay_hint_hovered")

  private val hoveredFilesIds = mutableSetOf<Int>()

  fun logInlayHintHovered(editor: Editor) {
    val document: Document = editor.document
    (FileDocumentManager.getInstance().getFile(document) as? VirtualFileWithId)?.let {
      if (hoveredFilesIds.add(it.id)) {
        INLAY_HINT_HOVERED.log()
      }
    }
  }

  // endregion inlay

  // region intention

  private val INTENTION_CLICKED = GROUP.registerEvent("intention_click")

  fun logIntentionClicked() {
    INTENTION_CLICKED.log()
  }

  // endregion intention

  // region api_keys_dialog

  private val ENV_API_KEYS_DIALOG_SHOWN = GROUP.registerEvent("env_api_keys_dialog_shown")

  fun logEnvApiKeysDialogShown() {
    ENV_API_KEYS_DIALOG_SHOWN.log()
  }

  private val ENV_API_KEYS_DIALOG_CANCELED = GROUP.registerEvent("env_api_keys_dialog_canceled")

  fun logEnvApiKeysDialogCanceled() {
    ENV_API_KEYS_DIALOG_CANCELED.log()
  }

  private val ENV_API_KEYS_ADDED = GROUP.registerEvent(
    "env_api_keys_added",
    EventFields.StringList("provider_ids", ALLOWED_PROVIDERS),
  )

  fun logEnvApiKeysAdded(providerIds: List<LlmProviderId>) {
    ENV_API_KEYS_ADDED.log(
      providerIds.map { it.id }
    )
  }

  // region api_keys_startup_notification

  private val ENV_API_KEYS_STARTUP_NOTIFICATION_SHOWN = GROUP.registerEvent("env_api_keys_startup_notification_shown")

  fun logEnvApiKeysStartupNotificationShown() {
    ENV_API_KEYS_STARTUP_NOTIFICATION_SHOWN.log()
  }


  private val ENV_API_KEYS_STARTUP_NOTIFICATION_CLICKED = GROUP.registerEvent("env_api_keys_startup_notification_clicked")

  fun logEnvApiKeysStartupNotificationClicked() {
    ENV_API_KEYS_STARTUP_NOTIFICATION_CLICKED.log()
  }

  private val ENV_API_KEYS_STARTUP_NOTIFICATION_CANCELED = GROUP.registerEvent("env_api_keys_startup_notification_canceled")

  fun logEnvApiKeysStartupNotificationCanceled() {
    ENV_API_KEYS_STARTUP_NOTIFICATION_CANCELED.log()
  }

  // endregion api_keys_startup_notification

  // region banner_import_env_api_keys_in_chat

  private val BANNER_IMPORT_ENV_API_KEYS_IN_CHAT_SHOWN = GROUP.registerEvent("banner_import_env_api_keys_in_chat_shown")

  fun logBannerImportEnvApiKeysInChatShown() {
    BANNER_IMPORT_ENV_API_KEYS_IN_CHAT_SHOWN.log()
  }

  private val BANNER_IMPORT_ENV_API_KEYS_IN_CHAT_CLICKED = GROUP.registerEvent("banner_import_env_api_keys_in_chat_clicked")

  fun logBannerImportEnvApiKeysInChatClicked() {
    BANNER_IMPORT_ENV_API_KEYS_IN_CHAT_CLICKED.log()
  }

  private val BANNER_IMPORT_ENV_API_KEYS_IN_CHAT_CANCELED = GROUP.registerEvent("banner_import_env_api_keys_in_chat_canceled")

  fun logBannerImportEnvApiKeysInChatCanceled() {
    BANNER_IMPORT_ENV_API_KEYS_IN_CHAT_CANCELED.log()
  }

  // endregion banner_import_env_api_keys_in_chat

  // region banner_open_settings_in_chat

  private val BANNER_OPEN_SETTINGS_IN_CHAT_SHOWN = GROUP.registerEvent("banner_open_settings_in_chat_shown")

  fun logBannerOpenSettingsInChatShown() {
    BANNER_OPEN_SETTINGS_IN_CHAT_SHOWN.log()
  }

  private val BANNER_OPEN_SETTINGS_IN_CHAT_CLICKED = GROUP.registerEvent("banner_open_settings_in_chat_clicked")

  fun logBannerOpenSettingsInChatClicked() {
    BANNER_OPEN_SETTINGS_IN_CHAT_CLICKED.log()
  }

  private val BANNER_OPEN_SETTINGS_IN_CHAT_CANCELED = GROUP.registerEvent("banner_open_settings_in_chat_canceled")

  fun logBannerOpenSettingsInChatCanceled() {
    BANNER_OPEN_SETTINGS_IN_CHAT_CANCELED.log()
  }

  // endregion banner_open_settings_in_chat

  // region banner_choose_at_least_one_model_in_chat

  private val BANNER_CHOOSE_AT_LEAST_ONE_MODEL_IN_CHAT_SHOWN =
    GROUP.registerEvent("banner_choose_at_least_one_model_in_chat_shown")

  fun logBannerChooseAtLeastOneModelInChatShown() {
    BANNER_CHOOSE_AT_LEAST_ONE_MODEL_IN_CHAT_SHOWN.log()
  }

  private val BANNER_CHOOSE_AT_LEAST_ONE_MODEL_IN_CHAT_CLICKED =
    GROUP.registerEvent("banner_choose_at_least_one_model_in_chat_clicked")

  fun logBannerChooseAtLeastOneModelInChatClicked() {
    BANNER_CHOOSE_AT_LEAST_ONE_MODEL_IN_CHAT_CLICKED.log()
  }

  private val BANNER_CHOOSE_AT_LEAST_ONE_MODEL_IN_CHAT_CANCELED =
    GROUP.registerEvent("banner_choose_at_least_one_model_in_chat_canceled")

  fun logBannerChooseAtLeastOneModelInChatCanceled() {
    BANNER_CHOOSE_AT_LEAST_ONE_MODEL_IN_CHAT_CANCELED.log()
  }

  // endregion banner_choose_at_least_one_model_in_chat

  // region banner_import_env_api_keys_in_settings

  private val BANNER_IMPORT_ENV_API_KEYS_IN_SETTINGS_SHOWN =
    GROUP.registerEvent("banner_import_env_api_keys_in_settings_shown")

  fun logBannerImportEnvApiKeysInSettingsShown() {
    BANNER_IMPORT_ENV_API_KEYS_IN_SETTINGS_SHOWN.log()
  }

  private val BANNER_IMPORT_ENV_API_KEYS_IN_SETTINGS_CLICKED =
    GROUP.registerEvent("banner_import_env_api_keys_in_settings_clicked")

  fun logBannerImportEnvApiKeysInSettingsClicked() {
    BANNER_IMPORT_ENV_API_KEYS_IN_SETTINGS_CLICKED.log()
  }

  private val BANNER_IMPORT_ENV_API_KEYS_IN_SETTINGS_CANCELED =
    GROUP.registerEvent("banner_import_env_api_keys_in_settings_canceled")

  fun logBannerImportEnvApiKeysInSettingsCanceled() {
    BANNER_IMPORT_ENV_API_KEYS_IN_SETTINGS_CANCELED.log()
  }

  // endregion banner_import_env_api_keys_in_settings

  // endregion api_keys_dialog
}