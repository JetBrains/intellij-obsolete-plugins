package com.jetbrains.bigdatatools.dataproc.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import java.io.File

@State(name = "DataprocSshKeyPaths", storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE)])
class DataprocSshKeysStorage : PersistentStateComponent<DataprocSshKeysStorage> {
  var keyMap: MutableMap<String, String> = mutableMapOf()

  fun getPathForKey(keyName: String): String? {
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

  override fun getState(): DataprocSshKeysStorage = this
  override fun loadState(state: DataprocSshKeysStorage) = XmlSerializerUtil.copyBean(state, this)

  companion object {
    fun getInstance(): DataprocSshKeysStorage = service()
  }
}