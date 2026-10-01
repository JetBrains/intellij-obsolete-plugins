package com.intellij.dataWrangler.executor

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VirtualFile

interface DataWranglerContext {
  fun getProject(): Project
  fun getColumnNames(): List<@NlsSafe String>
  fun getTableName(): String
  fun getFile(): VirtualFile?
  fun dispose()
}