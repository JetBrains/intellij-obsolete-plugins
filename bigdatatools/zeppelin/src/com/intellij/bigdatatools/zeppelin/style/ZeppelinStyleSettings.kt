package com.intellij.bigdatatools.zeppelin.style

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "ZeppelinStyleSettings", storages = [Storage("ZeppelinStyleSettings.xml")])
class ZeppelinStyleSettings : PersistentStateComponent<ZeppelinStyleSettings> {
  var cellsFolding = true

  override fun getState() = this

  override fun loadState(state: ZeppelinStyleSettings) = XmlSerializerUtil.copyBean(state, this)

  companion object {
    fun getInstance(): ZeppelinStyleSettings = service()
  }
}