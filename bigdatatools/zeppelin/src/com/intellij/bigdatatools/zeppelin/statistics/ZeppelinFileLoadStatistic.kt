package com.intellij.bigdatatools.zeppelin.statistics

import com.intellij.bigdatatools.zeppelin.file.getOriginalFile
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

object ZeppelinFileLoadStatistic {
  val START_INIT_TIME = Key<Long>("startInitTime")
  val FILE_EDITOR_CREATED = Key<Long>("fileEditorCreatedTime")

  fun startCreateFile(file: VirtualFile, time: Long) {
    val originFile = file.getOriginalFile()
    originFile.putUserData(START_INIT_TIME, time)
  }

  fun fileEditorCreated(file: VirtualFile) {
    val originFile = file.getOriginalFile()
    originFile.putUserData(FILE_EDITOR_CREATED, System.currentTimeMillis())
  }
}