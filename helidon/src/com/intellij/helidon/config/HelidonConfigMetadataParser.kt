// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.config

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.text.CharSequenceReader

/**
 * Processes Helidon 'config-metadata.json' file and returns [ModuleMetadata].
 *
 * In fact, it is just a deserializer.
 *
 * @see com.intellij.helidon.config.HelidonConfigMetadataBuilder
 */
internal class HelidonConfigMetadataParser {

  internal fun parse(configMetadataJson: PsiFile): ModuleMetadata? {
    val resolveScope = configMetadataJson.resolveScope
    return parseRootArray(configMetadataJson)
      ?.let { rootArray -> parseModuleConfigs(rootArray, resolveScope) }
      ?.let { moduleConfigs -> ModuleMetadata(moduleConfigs, resolveScope) }
  }

  private fun parseRootArray(configMetadataJson: PsiFile): JsonArray? {
    val configFilePath = configMetadataJson.virtualFile.path
    try {
      createReader(configMetadataJson).use { reader ->
        reader.isLenient = true
        return JsonParser.parseReader(reader).asJsonArraySafe()
      }
    }
    catch (ignored: ProcessCanceledException) {
      return null
    }
    catch (e: Throwable) {
      // Do not log as an error since user may work with invalid metadata file.
      thisLogger().info("Error parsing Helidon metadata JSON from $configFilePath", e)
      return null
    }
  }

  private fun createReader(configMetadataFile: PsiFile): JsonReader {
    return JsonReader(CharSequenceReader(VfsUtilCore.loadText(configMetadataFile.virtualFile)))
  }

  private fun parseModuleConfigs(moduleConfigs: JsonArray, resolveScope: GlobalSearchScope): List<ModuleConfig> {
    return moduleConfigs.mapNotNull { moduleConfigJson ->
      moduleConfigJson.asJsonObjectSafe()?.let { parseModuleConfig(it, resolveScope) }
    }
  }

  private fun parseModuleConfig(moduleConfig: JsonObject, resolveScope: GlobalSearchScope): ModuleConfig? {
    val name = moduleConfig.getStringSafe(HELIDON_METADATA_MODULE) ?: HELIDON_CONFIG_METADATA_UNNAMED_MODULE
    val types = moduleConfig.getArraySafe(HELIDON_METADATA_TYPES) ?: return null

    return ModuleConfig(parseModuleConfigTypes(name, types, resolveScope))
  }

  private fun parseModuleConfigTypes(moduleName: String, moduleConfig: JsonArray, resolveScope: GlobalSearchScope): List<ConfigType> {
    return moduleConfig.mapNotNull { configType ->
      configType.asJsonObjectSafe()?.let { parseConfigType(it, moduleName, resolveScope) }
    }
  }

  private fun parseConfigType(configType: JsonObject, moduleName: String, resolveScope: GlobalSearchScope): ConfigType? {
    val type = configType.getStringSafe(HELIDON_METADATA_TYPE) ?: return null
    val standalone = configType.getBooleanSafe(HELIDON_METADATA_STANDALONE) ?: false
    val prefix = configType.getStringSafe(HELIDON_METADATA_PREFIX) ?: ""
    val inherits = configType.getArraySafe(HELIDON_METADATA_INHERITS)?.mapNotNull { it.asStringSafe() } ?: emptyList()
    val options = configType.getArraySafe(HELIDON_METADATA_OPTIONS)?.mapNotNull { configOption ->
      configOption.asJsonObjectSafe()?.let { parseConfigOption(it) }
    } ?: emptyList()

    return ConfigType(type, standalone, prefix, inherits, options, moduleName, resolveScope)
  }

  private fun parseConfigOption(configOption: JsonObject): ConfigOption? {
    val key = configOption.getStringSafe(HELIDON_METADATA_OPTION_KEY) ?: return null
    val type = configOption.getStringSafe(HELIDON_METADATA_OPTION_TYPE) ?: CommonClassNames.JAVA_LANG_STRING
    val description = configOption.getStringSafe(HELIDON_METADATA_OPTION_DESCRIPTION) ?: ""
    val kind = configOption.getStringSafe(HELIDON_METADATA_OPTION_KIND)?.let {
      ConfigOptionKind.values().find { kind -> kind.name == it }
    } ?: ConfigOptionKind.VALUE
    val method = configOption.getStringSafe(HELIDON_METADATA_OPTION_METHOD)
    val deprecated = configOption.getBooleanSafe(HELIDON_METADATA_OPTION_DEPRECATED) ?: false
    val defaultValue = configOption.getStringSafe(HELIDON_METADATA_OPTION_DEFAULT_VALUE)

    return ConfigOption(key, type, description, kind, method, deprecated, defaultValue)
  }

  private fun JsonElement?.asJsonPrimitiveSafe(): JsonPrimitive? = this?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive
  private fun JsonElement?.asJsonObjectSafe(): JsonObject? = this?.takeIf { it.isJsonObject }?.asJsonObject
  private fun JsonElement?.asJsonArraySafe(): JsonArray? = this?.takeIf { it.isJsonArray }?.asJsonArray

  private fun JsonElement?.asStringSafe(): String? = this?.asJsonPrimitiveSafe()?.takeIf { it.isString }?.asString
  private fun JsonElement?.asBooleanSafe(): Boolean? = this?.asJsonPrimitiveSafe()?.takeIf { it.isBoolean }?.asBoolean

  private fun JsonObject?.getStringSafe(name: String): String? = this?.get(name)?.asStringSafe()
  private fun JsonObject?.getArraySafe(name: String): JsonArray? = this?.get(name)?.asJsonArraySafe()
  private fun JsonObject?.getBooleanSafe(name: String): Boolean? = this?.get(name)?.asBooleanSafe()
}

internal data class ModuleMetadata(val moduleConfigs: List<ModuleConfig>,
                                   val resolveScope: GlobalSearchScope)

internal data class ModuleConfig(val types: List<ConfigType>)

internal data class ConfigType(val type: String,
                               val standalone: Boolean,
                               val prefix: String,
                               val inherits: List<String>,
                               val options: List<ConfigOption>,
                               val moduleName: String,
                               val resolveScope: GlobalSearchScope)

internal data class ConfigOption(val key: String,
                                 val type: String,
                                 val description: String,
                                 val kind: ConfigOptionKind,
                                 val method: String?,
                                 val deprecated: Boolean,
                                 val defaultValue: String?)

internal enum class ConfigOptionKind {
  VALUE, LIST, MAP
}
