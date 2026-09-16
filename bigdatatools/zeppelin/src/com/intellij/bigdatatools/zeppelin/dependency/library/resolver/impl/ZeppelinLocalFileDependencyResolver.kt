package com.intellij.bigdatatools.zeppelin.dependency.library.resolver.impl

import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.dependency.library.resolver.ZeppelinDependencyResolver
import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.ui.OrderRoot
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

class ZeppelinLocalFileDependencyResolver : ZeppelinDependencyResolver {
  override fun accept(dep: ZepDependency) = dep.isFile()

  override fun resolveRoots(project: Project?,
                            dep: ZepDependency,
                            repos: List<Repository>,
                            rootType: OrderRootType,
                            force: Boolean,
                            indicator: ProgressIndicator?): List<OrderRoot> = getRoots(dep)

  private fun getRoots(dep: ZepDependency): List<OrderRoot> {
    val file = File(dep.groupArtifactVersion)
    if (!file.exists()) {
      return emptyList()
    }
    if (file.isDirectory) {
      return emptyList()
    }
    val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
    val jarRootForLocalFile = virtualFile?.let { JarFileSystem.getInstance().getJarRootForLocalFile(it) }
    if (jarRootForLocalFile == null) {
      return emptyList()
    }

    val root = OrderRoot(jarRootForLocalFile, OrderRootType.CLASSES)
    return listOf(root)
  }
}