package com.intellij.bigdatatools.databricks.util

import com.databricks.sdk.core.ConfigLoader
import com.databricks.sdk.core.DatabricksConfig
import com.databricks.sdk.core.DatabricksException
import com.intellij.openapi.diagnostic.thisLogger
import org.ini4j.Ini
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Paths

/**
 * Profile is a file looking like:
 *
 * [First user]
 * host  = https://somehost.azuredatabricks.net
 * token = long_token
 *
 * [DEFAULT]
 * host  = https://someotherhost.azuredatabricks.net
 * token = another_long_token
 */
object DatabricksProfiles {
  fun getPropertiesOrEmpty(): Array<String> {
    return try {
      getProfiles()?.toTypedArray() ?: emptyArray()
    }
    catch (t: Throwable) {
      thisLogger().warn(t)
      arrayOf("<NOT FOUND>")
    }
  }

  fun getProfileHost(profileName: String): String? {
    return try {
      getConfigs()?.get(profileName)?.get("host")
    }
    catch (t: Throwable) {
      null
    }
  }

  private fun getProfiles(): List<String>? {
    return getConfigs()?.keys?.toList()
  }

  private fun getConfigs(): Ini? {
    val cfg = DatabricksConfig().resolve()
    var userHome: String? = cfg.env.get("HOME")
    if (ConfigLoader.isNullOrEmpty(userHome)) {
      userHome = System.getProperty("user.home")
    }

    userHome ?: return null

    var configFile: String? = cfg.configFile
    var isDefaultConfig = false
    if (ConfigLoader.isNullOrEmpty(configFile)) {
      configFile = Paths.get(userHome, ".databrickscfg").toString()
      isDefaultConfig = true
    }
    else {
      configFile = configFile?.replaceFirst("^~".toRegex(), userHome)
    }
    configFile ?: return null
    return parseDatabricksCfg(configFile, isDefaultConfig)
  }

  private fun parseDatabricksCfg(configFile: String, isDefaultConfig: Boolean): Ini? {
    val ini = Ini()
    try {
      ini.load(File(configFile))
    }
    catch (e: FileNotFoundException) {
      if (isDefaultConfig) {
        return null
      }
    }
    catch (e: IOException) {
      throw DatabricksException("Cannot load $configFile", e)
    }
    return ini
  }
}