package com.intellij.dataWrangler.executor

import com.intellij.dataWrangler.operations.DataWranglerCommand
import com.intellij.openapi.fileTypes.LanguageFileType

interface CodePreviewProvider<C : DataWranglerContext> {

  fun isCommandApplicable(command: DataWranglerCommand<C>): Boolean

  suspend fun getCodePreview(context: C, command: DataWranglerCommand<C>): String

  fun getFileType(): LanguageFileType

  suspend fun getTransformationCode(
    context: C,
    commands: List<DataWranglerCommand<C>>
  ): String
}