package com.intellij.bigdatatools.zeppelin.dependency.collector.interpreter

import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.RepositoryAuth
import com.intellij.bigdatatools.zeppelin.dependency.model.ZeppelinLibrary
import com.intellij.bigdatatools.zeppelin.dependency.model.ZeppelinLibraryType
import com.intellij.bigdatatools.zeppelin.models.interpreter.InterpreterSettings
import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency

object InterpreterDependenciesCollector {
  fun collectDependencyFromInterpreterSettings(settings: InterpreterSettings): List<ZeppelinLibrary> {
    val excludes = getPropertiesList(settings, "spark.jars.excludes")
    val livyExcludes = getPropertiesList(settings, "livy.spark.jars.excludes")

    val mvnJars = getDeps(settings, "spark.jars.packages", excludes)
    val mvnLivyJars = getDeps(settings, "livy.spark.jars.packages", livyExcludes)

    val jars = getDeps(settings, "spark.jars", emptyList())
    val livyJars = getDeps(settings, "livy.spark.jars", emptyList())

    val zepDeps = settings.dependencies.map { ZeppelinLibrary(it, ZeppelinLibraryType.INTERPRETER, from = "${settings.name}: dependencies") }
    return mvnJars + mvnLivyJars + jars + livyJars + zepDeps
  }

  fun getConfRepositories(settings: InterpreterSettings): List<Repository> {
    val sparkRepos = getPropertiesList(settings, "spark.jars.repositories")
    val livyRepos = getPropertiesList(settings, "livy.spark.jars.repositories")
    val rawRepos = sparkRepos + livyRepos
    return rawRepos.map {
      Repository(it.trim(), it.trim(), RepositoryAuth())
    }
  }

  private fun getDeps(settings: InterpreterSettings, propertyName: String, excludes: List<String>): List<ZeppelinLibrary> {
    val depsRaw = getPropertiesList(settings, propertyName).filter { it.isNotBlank() }
    val deps = depsRaw.map { ZepDependency(it, excludes) }
    return deps.map { ZeppelinLibrary(it, ZeppelinLibraryType.INTERPRETER, "${settings.name}: $propertyName") }
  }

  private fun getPropertiesList(settings: InterpreterSettings, property: String): List<String> {
    val propertyRaw = settings.properties[property]?.value as? String ?: return emptyList()
    return propertyRaw.split(",").map { it.trim() }
  }
}