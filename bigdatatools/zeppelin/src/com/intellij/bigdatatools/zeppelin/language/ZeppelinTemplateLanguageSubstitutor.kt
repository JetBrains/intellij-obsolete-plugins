// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.bigdatatools.zeppelin.language

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.LanguageSubstitutor

/**
 * Velocity file provider breaks on template (.ft) files like .zpln.ft and .inote.ft
 * in case there is no such substitutor. This substitutor ensures all template files
 * are handled as plain text to prevent indexing issues.
 */
class ZeppelinTemplateLanguageSubstitutor : LanguageSubstitutor() {
  override fun getLanguage(file: VirtualFile, project: Project): Language? {
    if (file.extension == "ft") {
      return PlainTextLanguage.INSTANCE
    }
    return null
  }
}