package com.intellij.bigdatatools.zeppelin.ztools.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "BdtStateViewer", storages = [Storage("BdtStateViewer.xml")])
class ZtoolsPersistentService : PersistentStateComponent<ZtoolsPersistentService> {
  var doNotOfferForConnIds = listOf<String>()

  override fun getState() = this

  override fun loadState(state: ZtoolsPersistentService) = XmlSerializerUtil.copyBean(state, this)

  companion object {
    fun getInstance(): ZtoolsPersistentService = service()
  }
}