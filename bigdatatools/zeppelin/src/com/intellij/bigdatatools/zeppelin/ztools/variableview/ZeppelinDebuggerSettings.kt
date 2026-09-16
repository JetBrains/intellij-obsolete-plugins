package com.intellij.bigdatatools.zeppelin.ztools.variableview

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.Getter
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.xdebugger.settings.DebuggerSettingsCategory
import com.intellij.xdebugger.settings.XDebuggerSettings

class ZeppelinDebuggerSettings : XDebuggerSettings<ZeppelinDebuggerSettings>("zeppelin"), Getter<ZeppelinDebuggerSettings> {
  var isSimplifiedView = true

  override fun getState(): ZeppelinDebuggerSettings = this

  override fun loadState(state: ZeppelinDebuggerSettings) = XmlSerializerUtil.copyBean(state, this)

  override fun isTargetedToProduct(configurable: Configurable): Boolean = true

  override fun createConfigurables(category: DebuggerSettingsCategory): Collection<Configurable> = emptyList()

  override fun get(): ZeppelinDebuggerSettings = this

  companion object {
    val instance: ZeppelinDebuggerSettings
      get() = getInstance(ZeppelinDebuggerSettings::class.java)
  }
}
