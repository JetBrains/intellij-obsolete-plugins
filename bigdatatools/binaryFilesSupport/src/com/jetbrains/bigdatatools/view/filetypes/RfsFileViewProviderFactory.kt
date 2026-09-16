package com.jetbrains.bigdatatools.view.filetypes

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.source.PsiPlainTextFileImpl
import com.intellij.testFramework.LightVirtualFile

abstract class RfsFileViewProviderFactory(private val baseFileType: FileType) : FileViewProviderFactory {
  override fun createFileViewProvider(file: VirtualFile,
                                      language: Language?,
                                      manager: PsiManager,
                                      eventSystemEnabled: Boolean): FileViewProvider {
    val p = file !is LightVirtualFile && file.fileSystem !is NonPhysicalFileSystem

    return object : SingleRootFileViewProvider(manager, file, true, file.fileType) {
      override fun correspondsToRealFile(): Boolean = p

      override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? =
        if (file.fileType == baseFileType) PsiPlainTextFileImpl(this) else super.createFile(project, file, fileType)

      override fun createFile(file: VirtualFile, fileType: FileType, language: Language): PsiFile =
        if (file.fileType == baseFileType) PsiPlainTextFileImpl(this) else super.createFile(file, fileType, language)
    }
  }
}