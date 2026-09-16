// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.psi

import com.intellij.bigdatatools.notebooks.core.impl.psi.NotebookPsiFile
import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.intellij.bigdatatools.zeppelin.language.ZeppelinLanguage
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Key
import com.intellij.psi.FileViewProvider

class ZeppelinPsiFile(viewProvider: FileViewProvider) : NotebookPsiFile(viewProvider, ZeppelinLanguage) {

  override fun getFileType(): ZeppelinFileType = ZeppelinFileType

  override fun toString() = "Zeppelin Notebook"

  override fun <T : Any?> getUserData(key: Key<T>): T? {

    if (key == ModuleUtilCore.KEY_MODULE) {
      val vf = virtualFile
      if (vf != null) {
        val module = ZeppelinCustomPsiFileFactory.findZeppelinModule(vf, project)
        if (module != null) return module as T
      }
    }

    return super.getUserData(key)
  }
}