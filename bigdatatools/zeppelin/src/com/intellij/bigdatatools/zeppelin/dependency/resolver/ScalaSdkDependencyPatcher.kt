package com.intellij.bigdatatools.zeppelin.dependency.resolver

import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.roots.libraries.ui.OrderRoot
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties

interface ScalaSdkDependencyPatcher {
  fun setupLibrary(name: String, roots: List<OrderRoot>, dep: ZepDependency,
                   modifiableLibraryModel: LibraryTable.ModifiableModel,
                   type: PersistentLibraryKind<RepositoryLibraryProperties>?
                      )

  companion object {
    private const val NAME: String = "com.intellij.bigdatatools.zeppelin.scalaSdkDependencyPatcher"
    private val EP_NAME: ExtensionPointName<ScalaSdkDependencyPatcher> = ExtensionPointName.create(NAME)

    fun getDependencyPatcher(): ScalaSdkDependencyPatcher = EP_NAME.extensionList.firstOrNull() ?: DefaultSdkDependencyPatcher()
  }
}

open class DefaultSdkDependencyPatcher : ScalaSdkDependencyPatcher {
  override fun setupLibrary(name: String,
                            roots: List<OrderRoot>,
                            dep: ZepDependency,
                            modifiableLibraryModel: LibraryTable.ModifiableModel,
                            type: PersistentLibraryKind<RepositoryLibraryProperties>?) {
    val lib = modifiableLibraryModel.createLibrary(name, type)
    val libModifiableModel = lib.modifiableModel as LibraryEx.ModifiableModelEx

    if (dep.isMaven()) {
      libModifiableModel.properties = RepositoryLibraryProperties(dep.group, dep.id, dep.version, dep.transitive, dep.exclusions)
    }

    addAndCommit(roots, libModifiableModel)
  }

  protected fun addAndCommit(roots: List<OrderRoot>, libModifiableModel: LibraryEx.ModifiableModelEx) {
    roots.forEach {
      libModifiableModel.addRoot(it.file, it.type)
    }
    libModifiableModel.commit()
  }
}