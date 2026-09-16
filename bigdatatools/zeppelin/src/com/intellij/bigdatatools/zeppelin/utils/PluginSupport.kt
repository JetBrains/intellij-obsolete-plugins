package com.intellij.bigdatatools.zeppelin.utils

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId

object PluginSupport  {
  fun isJavaSupport(): Boolean {
    val plugin = PluginManager.getInstance().findEnabledPlugin(PluginId.getId("com.intellij.java"))
    return plugin?.isEnabled == true
  }
}