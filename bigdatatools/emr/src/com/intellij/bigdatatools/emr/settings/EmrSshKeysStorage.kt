package com.intellij.bigdatatools.emr.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import java.io.File

@State(name = "EmrSshKeyPaths", storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE)])
class EmrSshKeysStorage : PersistentStateComponent<EmrSshKeysStorage> {
  private var keyMap: MutableMap<String, String> = mutableMapOf()

  fun getPathForKey(keyName: String?): String? {
    val path = keyMap[keyName] ?: return null
    if (!File(path).exists()) {
      keyMap.remove(keyName)
      return null
    }
    return path
  }

  fun put(keyName: String, path: String) {
    keyMap[keyName] = path
  }

  override fun getState(): EmrSshKeysStorage = this
  override fun loadState(state: EmrSshKeysStorage) = XmlSerializerUtil.copyBean(state, this)

  companion object {
    fun getInstance(): EmrSshKeysStorage = service()
  }
}