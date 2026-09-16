package com.intellij.bigdatatools.emr.ui.component

import com.intellij.bigdatatools.emr.dependend.drivers.EmrDriversProvider
import com.intellij.openapi.project.Project
import com.jetbrains.bigdatatools.common.rfs.util.withPrefixSlash
import com.jetbrains.spark.submit.model.FilePath
import com.jetbrains.spark.submit.model.FilePathSerializer
import com.jetbrains.spark.submit.model.FileSelectorContext
import com.jetbrains.spark.submit.model.FileSelectorType
import com.jetbrains.spark.submit.model.FileType
import com.jetbrains.spark.submit.run.common.ui.TextFieldFileSelector
import org.jetbrains.annotations.Nls

interface EmrFileSelectorContext : FileSelectorContext {
  val driversProvider: EmrDriversProvider
}

// TODO inline
fun EmrFileSelector(
  @Nls(capitalization = Nls.Capitalization.Title) title: String,
  project: Project,
  driversProvider: EmrDriversProvider,
  withS3: Boolean,
  fileSelectorType: FileSelectorType = if (withS3) EmrFileSelectorUtils.EMR_UPLOAD_WITH_S3 else EmrFileSelectorUtils.EMR_UPLOAD_WITHOUT_S3,
) = TextFieldFileSelector(fileSelectorType, EmrTypesSerializer, EmrFileSelectorContextImpl(project, title, driversProvider), defaultText = "")

object EmrTypesSerializer : FilePathSerializer {
  override fun toText(path: FilePath): String {
    return when {
      path.path.isBlank() -> ""
      path.type == FileType.S3 -> path.toString()
      path.type == FileType.SERVER -> path.path.withPrefixSlash()
      else -> path.path
    }
  }
  override fun fromText(text: String): FilePath {
    return FilePath.fromPathWithScheme(text)
  }
}