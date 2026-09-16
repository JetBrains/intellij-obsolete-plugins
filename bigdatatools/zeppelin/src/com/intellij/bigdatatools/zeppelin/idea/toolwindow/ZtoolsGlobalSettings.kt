package com.intellij.bigdatatools.zeppelin.idea.toolwindow

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "BdtStateViewerGlobalSettings", storages = [Storage("BdtStateViewerGlobalSettings.xml")])
class ZtoolsGlobalSettings : PersistentStateComponent<ZtoolsGlobalSettings> {
  var offerToShowSuggestionOpenPanel = true

  override fun getState() = this

  override fun loadState(state: ZtoolsGlobalSettings) = XmlSerializerUtil.copyBean(state, this)

  companion object {
    fun getInstance(): ZtoolsGlobalSettings = service()
  }
}