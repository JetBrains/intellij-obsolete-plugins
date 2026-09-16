package com.intellij.bigdatatools.zeppelin.dependency.library.resolver.impl

import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.dependency.library.resolver.ZeppelinDependencyResolver
import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.ui.OrderRoot

class ZeppelinModuleDependencyResolver : ZeppelinDependencyResolver {
  override fun accept(dep: ZepDependency) = dep.isModule()

  override fun resolveRoots(project: Project?,
                            dep: ZepDependency,
                            repos: List<Repository>,
                            rootType: OrderRootType,
                            force: Boolean,
                            indicator: ProgressIndicator?): List<OrderRoot> = emptyList()
}