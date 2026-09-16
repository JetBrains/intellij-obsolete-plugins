package com.intellij.bigdatatools.plugin.spark.maven.projectwizard

import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import com.jetbrains.bigdatatools.wizard.BuildSystemBasedStructureProvider
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.idea.maven.utils.MavenUtil
import java.io.File

internal class MavenBasedStructureProvider(
  projectDir: File, errorHandler: (Exception) -> Unit
) : BuildSystemBasedStructureProvider(projectDir, errorHandler) {
  override val simpleName: String = "Maven"
  override val buildFiles: Set<String> = setOf("pom.xml")

  override fun prefixSrc() = "src/main/java"

  override fun linkProject(project: Project, contentEntryPath: String, progressIndicator: ProgressIndicator) {
    val pom = LocalFileSystem.getInstance().refreshAndFindFileByPath(File(projectDir, "pom.xml").absolutePath)
    if (pom == null) return

    val mavenManager = MavenProjectsManager.getInstance(project)
    mavenManager.addManagedFilesOrUnignoreNoUpdate(listOf(pom))
    mavenManager.forceUpdateAllProjectsOrFindAllAvailablePomFiles()

    MavenUtil.invokeLater(project, ModalityState.nonModal()) {
      PsiManager.getInstance(project).findFile(pom)?.let { EditorHelper.openInEditor(it) }
    }
  }

  override fun originalSkeletonUrl(): String? = null

  override fun resourceSkeletonUrl(): String = "templates/maven-template.zip"
}