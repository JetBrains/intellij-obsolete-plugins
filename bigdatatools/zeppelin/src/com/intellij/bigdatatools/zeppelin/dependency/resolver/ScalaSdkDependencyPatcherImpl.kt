package com.intellij.bigdatatools.zeppelin.dependency.resolver

import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.LibraryKindRegistry
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.roots.libraries.ui.OrderRoot
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties
import org.jetbrains.plugins.scala.project.`ScalaLibraryProperties$`
import scala.Option
import scala.jdk.javaapi.CollectionConverters
import java.nio.file.Path
import java.nio.file.Paths

class ScalaSdkDependencyPatcherImpl : DefaultSdkDependencyPatcher() {
  companion object {
    private const val SDK_MARKER = "catalyst"
    private const val SCALA_KIND_ID = "Scala"
    private const val DEFAULT_SCALA_VERSION = "2.12.18"
  }

  override fun setupLibrary(
    name: String,
    roots: List<OrderRoot>,
    dep: ZepDependency,
    modifiableLibraryModel: LibraryTable.ModifiableModel,
    type: PersistentLibraryKind<RepositoryLibraryProperties>?,
  ) {
    val isSdkLib = name.contains(SDK_MARKER) && LibraryKindRegistry.getInstance().findKindById(SCALA_KIND_ID) != null
    val lib = modifiableLibraryModel.createLibrary(
      name, if (isSdkLib) LibraryKindRegistry.getInstance().findKindById(SCALA_KIND_ID) as PersistentLibraryKind<*> else type
    )
    val libModifiableModel = lib.modifiableModel as LibraryEx.ModifiableModelEx

    if (isSdkLib) {
      val version = roots.find {
        it.file.presentableName.contains("scala-library")
      }?.file?.presentableName?.removePrefix("scala-library-")?.removeSuffix(".jar") ?: DEFAULT_SCALA_VERSION

      val compilerJars =
        roots.filter { root -> root.file.name.contains("scala-") }
          .map { root -> Paths.get(root.file.path.removeSuffix("!/")) }

      try {
        libModifiableModel.properties = `ScalaLibraryProperties$`.`MODULE$`.apply(
          Option.apply(version),
          CollectionConverters.asScala(compilerJars).toSeq(),
          CollectionConverters.asScala(emptyList<Path>()).toSeq()
        )
      }
      catch (t: Throwable) {
        thisLogger().error("Cannot setup Scala sdk", t)
      }
    }
    else if (dep.isMaven()) {
      libModifiableModel.properties = RepositoryLibraryProperties(dep.group, dep.id, dep.version, dep.transitive, dep.exclusions)
    }

    addAndCommit(roots, libModifiableModel)
  }
}