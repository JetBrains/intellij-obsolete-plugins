package com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase


open class DfAttachable : UserDataHolderBase() {
  fun <T> putUserDataMap(key: Key<MutableSet<T>>, t: T) {
    val set = getUserData(key)
    if (set == null) putUserData(key, mutableSetOf(t)) else set.add(t)
  }

  override fun copyUserDataTo(other: UserDataHolderBase) {
    for (key in userMap.keys) {
      @Suppress("UNCHECKED_CAST")
      other.putUserData(key as Key<Any>, userMap.get(key))
    }
  }
}

