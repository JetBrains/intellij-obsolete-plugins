package com.intellij.bigdatatools.zeppelin.dependency.library.resolver

import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.ui.OrderRoot

interface ZeppelinDependencyResolver : Disposable {
  fun accept(dep: ZepDependency): Boolean

  fun resolveRoots(project: Project?,
                   dep: ZepDependency,
                   repos: List<Repository>,
                   rootType: OrderRootType,
                   force: Boolean = false,
                   indicator: ProgressIndicator? = null): List<OrderRoot>

  override fun dispose() {}
}