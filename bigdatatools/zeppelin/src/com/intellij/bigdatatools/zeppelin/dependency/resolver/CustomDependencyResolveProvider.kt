package com.intellij.bigdatatools.zeppelin.dependency.resolver

import com.intellij.bigdatatools.zeppelin.dependency.model.NoteDependency
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module

interface CustomDependencyResolveProvider {
  fun runDependencyResolve(module: Module, customDeps: List<NoteDependency>)

  companion object {
    private const val NAME: String = "com.intellij.bigdatatools.zeppelin.customDependencyResolveProvider"
    private val EP_NAME: ExtensionPointName<CustomDependencyResolveProvider> = ExtensionPointName.create(NAME)

    fun getAll(): List<CustomDependencyResolveProvider> = EP_NAME.extensionList
  }
}