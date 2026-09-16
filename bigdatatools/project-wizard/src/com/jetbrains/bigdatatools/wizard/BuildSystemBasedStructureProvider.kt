package com.jetbrains.bigdatatools.wizard

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.ZipUtil
import com.jetbrains.bigdatatools.common.rfs.util.withSlash
import com.jetbrains.bigdatatools.wizard.util.WizardMessageBundle
import org.jetbrains.annotations.NotNull
import java.io.File
import java.io.InputStream
import java.util.Locale

abstract class BuildSystemBasedStructureProvider(
  protected val projectDir: File,
  protected val errorHandler: (Exception) -> Unit) {
  protected abstract val simpleName: String
  protected abstract val buildFiles: Set<String>

  open fun copyProjectTemplate(progressIndicator: ProgressIndicator,
                               templateAdditionalParameters: Map<String, String>,
                               groupId: String,
                               artifactName: String,
                               projectType: SparkProjectType,
                               languageHandler: SparkProjectLanguageHandler) {
    val extractedArchivePath = setupSkeletonRoot(progressIndicator) ?: return

    val templateParams = mapOf(
      WizardTemplateConst.GROUP_ID to groupId,
      WizardTemplateConst.ARTIFACT to artifactName,
      WizardTemplateConst.SCALA_VERSIONS_SHORT to WizardTemplateConst.SCALA_VERSION_SHORT_DEFAULT,
      WizardTemplateConst.SCALA_FULL_VERSION to WizardTemplateConst.SCALA_FULL_VERSION_DEFAULT,
    )

    val params = (templateParams + templateAdditionalParameters).toMutableMap()
    params[WizardTemplateConst.SPARK_VERSION] = WizardTemplateConst.getSparkVersionFor(params[WizardTemplateConst.SCALA_VERSIONS_SHORT]!!)

    FileUtil.copyDir(extractedArchivePath, projectDir,
                     TemplateBuildDescriptorProcessor(buildFiles, extractedArchivePath, descriptorParentDir(), params))

    val artifactRegex = Regex("[a-z]+")
    val artifactPrefix = artifactRegex.find(artifactName.lowercase(Locale.US))?.value
    val packageName = groupId + (artifactPrefix?.let { ".$it" } ?: "")
    val srcDir = projectDir.resolve(prefixSrc().withSlash() + packageName.replace(".", "/"))

    val language = languageHandler.language
    val templateName = when (language) {
      SparkLanguageType.JAVA -> when (projectType) {
        SparkProjectType.STREAMING -> "QueueStream.java"
        SparkProjectType.BATCH -> "SparkPi.java"
      }

      SparkLanguageType.SCALA -> when (projectType) {
        SparkProjectType.STREAMING -> "QueueStream.scala"
        SparkProjectType.BATCH -> "SparkPi.scala"
      }
    }

    val template = this.javaClass.getResourceAsStream("/template/$templateName")?.bufferedReader()?.readText()!!
    srcDir.mkdirs()
    val sourceFile = srcDir.resolve(templateName)
    sourceFile.createNewFile()
    val formattedText = template.replace("\$\$\$${WizardTemplateConst.PACKAGE}\$\$\$", packageName)
    sourceFile.writeText(formattedText)
  }

  abstract fun prefixSrc(): String

  abstract fun linkProject(project: Project, contentEntryPath: String, progressIndicator: ProgressIndicator)

  abstract fun originalSkeletonUrl(): String?
  abstract fun resourceSkeletonUrl(): String

  protected open fun descriptorParentDir(): File = projectDir

  protected open fun setupSkeletonRoot(progressIndicator: ProgressIndicator): File? {
    try {
      progressIndicator.isIndeterminate = true
      progressIndicator.text = WizardMessageBundle.message("bdide.project.wizard.downloading.message")

      val originalUrl = originalSkeletonUrl()

      if (originalUrl != null) {
        val fileDescription = DownloadableFileService.getInstance().createFileDescription(
          originalUrl, "$simpleName-bdide-template"
        )

        val downloader = DownloadableFileService.getInstance().createDownloader(
          listOf(fileDescription), "$simpleName template"
        )

        val targetDir = FileUtil.createTempDirectory("$simpleName-bdide-template", null)

        downloader.download(targetDir).firstOrNull()?.let { pair ->
          return unzip(pair.first)
        }
      }

      this.javaClass.classLoader.getResourceAsStream(resourceSkeletonUrl())?.let { stream ->
        return unzip(copyFromStream(stream))
      }
    }
    catch (e: Exception) {
      errorHandler(e)
      return null
    }

    errorHandler(RuntimeException("No template found"))
    return null
  }

  private fun copyFromStream(stream: InputStream): @NotNull File {
    val tempFile = FileUtil.createTempFile("bdide-template", ".zip", true)
    stream.copyTo(tempFile.outputStream())

    return tempFile
  }

  private fun unzip(file: File): File {
    val outputDir = FileUtil.createTempDirectory("unzipped-$simpleName-template", null)
    ZipUtil.extract(file.toPath(), outputDir.toPath(), null)
    return outputDir
  }
}