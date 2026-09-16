package com.intellij.bigdatatools.zeppelin.ztools.completion

import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.intellij.bigdatatools.zeppelin.ztools.dataframe.SPARK_DATA_FRAME_KEY
import com.intellij.bigdatatools.zeppelin.ztools.dataframe.SparkDataFrameSchema
import com.intellij.bigdatatools.zeppelin.ztools.dataframe.ZtoolsDataFrameStorage
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

open class ZeppelinColumnCompletionContributor : CompletionContributor() {
  companion object {
    @JvmStatic
    fun createTestStorage(): ZtoolsDataFrameStorage {
      val storage = ZtoolsDataFrameStorage()
      storage.addDataFrame(
        "test",
        SparkDataFrameSchema(listOf(
          ColumnInfo("id", IntColumnType, false),
          ColumnInfo("name", StringColumnType, true),
          ColumnInfo("type", BooleanColumnType, false),
          ColumnInfo("ratata", StringColumnType, false)
        ))
      )
      storage.addDataFrame(
        "table",
        SparkDataFrameSchema(listOf(
          ColumnInfo("guid", IntColumnType, false),
          ColumnInfo("title", StringColumnType, true),
          ColumnInfo("blah", BooleanColumnType, false),
          ColumnInfo("tratata", StringColumnType, false)
        ))
      )
      return storage
    }

    @JvmStatic
    protected fun schemaCommon(fullText: String, file: PsiFile): Collection<ColumnInfo> {
      val storage = file.originalFile.virtualFile.getCopyableUserData(SPARK_DATA_FRAME_KEY) ?: return emptyList()
      return storage.getColumns(fullText)
    }
  }

  protected fun extendWithClass(clazz: Class<out PsiElement>, provider: CompletionProvider<CompletionParameters>) {
    extend(
      CompletionType.BASIC,
      PlatformPatterns.psiElement().inside(clazz).inVirtualFile(PlatformPatterns.virtualFile().ofType(ZeppelinFileType)),
      provider
    )
  }
}