package com.intellij.bigdatatools.zeppelin.dependency.library.resolver.impl

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinTimeouts
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.dependency.library.resolver.ZeppelinDependencyResolver
import com.intellij.bigdatatools.zeppelin.models.interpreter.ZepDependency
import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.jarRepository.RemoteRepositoriesConfiguration
import com.intellij.jarRepository.RemoteRepositoryDescription
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.ui.OrderRoot
import org.jetbrains.idea.maven.aether.ArtifactKind
import org.jetbrains.idea.maven.aether.ArtifactRepositoryManager
import org.jetbrains.idea.maven.aether.ProgressConsumer
import org.jetbrains.jps.model.library.JpsMavenRepositoryLibraryDescriptor
import java.io.File
import java.util.concurrent.TimeoutException

class ZeppelinMavenDependencyResolver : ZeppelinDependencyResolver {
  private val timeout = ZeppelinTimeouts.DOWNLOAD_DEP_TIMEOUT
  override fun accept(dep: ZepDependency) = dep.isMaven()

  override fun resolveRoots(project: Project?,
                            dep: ZepDependency,
                            repos: List<Repository>,
                            rootType: OrderRootType,
                            force: Boolean,
                            indicator: ProgressIndicator?): List<OrderRoot> {
    ApplicationManager.getApplication().assertIsNonDispatchThread()
    if (!canBeDownloaded(repos, dep, project))
      return emptyList()

    val artifactKinds = resolveArtifactKind(rootType)

    val lipDesc = JpsMavenRepositoryLibraryDescriptor(dep.group, dep.id, dep.version, dep.transitive, dep.exclusions)
    val repositories = createIdeaRepositoryDescriptorsWithoutSnapshots(repos, project)

    if (indicator?.isCanceled == true)
      return emptyList()

    var roots = try {
      JarRepositoryManager.loadDependenciesAsync(project ?: ProjectManager.getInstance().defaultProject,
                                                 lipDesc,
                                                 artifactKinds,
                                                 repositories,
                                                 null).blockingGet(timeout + 30000)
    }
    catch (t: TimeoutException) {
      emptyList<OrderRoot>()
    }

    roots ?: error("Maven roots not found")

    if (roots.isEmpty() && lipDesc.isIncludeTransitiveDependencies && rootType == OrderRootType.CLASSES) {
      if (indicator?.isCanceled == true)
        return emptyList()


      val libWithoutTransitive = JpsMavenRepositoryLibraryDescriptor(
        dep.group,
        dep.id,
        dep.version,
        false,
        emptyList()
      )
      roots = JarRepositoryManager.loadDependenciesAsync(
        project ?: ProjectManager.getInstance().defaultProject,
        libWithoutTransitive,
        artifactKinds,
        repositories,
        null
      ).blockingGet(timeout) ?: error("Maven roots not found")
    }
    return roots
  }

  /**
   * We need to use reflection because in 211 there is a conflict of platform libs and CastException can be thrown
   */
  @Suppress("UNCHECKED_CAST")
  private fun resolveArtifactKind(rootType: OrderRootType): Set<ArtifactKind> {
    val list = JarRepositoryManager::class.java
    val cl = list.classLoader
    val m = ClassLoader::class.java.getDeclaredMethod("findLoadedClass", String::class.java)
    m.isAccessible = true
    val cls = m.invoke(cl, "org.jetbrains.idea.maven.aether.ArtifactKind")

    val enumConstants = (cls as Class<*>).enumConstants
    val artifactKindsClasses = setOf(enumConstants[0]) as Set<ArtifactKind>
    val artifactKindsSources = setOf(enumConstants[1]) as Set<ArtifactKind>

    return when (rootType) {
      OrderRootType.CLASSES -> artifactKindsClasses
      OrderRootType.SOURCES -> artifactKindsSources
      else -> setOf()
    }
  }

  private fun canBeDownloaded(repositories: List<Repository>, dep: ZepDependency, project: Project?): Boolean {
    val repos = createIdeaRepositoryDescriptorsWithoutSnapshots(repositories, project).map {
      try {
        ArtifactRepositoryManager.createRemoteRepository(it.id, it.url, true)
      }
      catch (t: Throwable) {
        ArtifactRepositoryManager.createRemoteRepository(it.id, it.url)
      }
    }
    val manager = ArtifactRepositoryManager(JarRepositoryManager.getLocalRepositoryPath(),
                                            repos,
                                            object : ProgressConsumer {
                                              override fun consume(message: String) {}
                                              override fun isCanceled(): Boolean = false
                                            })
    val files = try {
      manager.resolveDependency(dep.group, dep.id, dep.version, false, emptyList())
    }
    catch (e: Exception) {
      emptyList<File>()
    }

    return files.isNotEmpty()
  }

  //Old version return noSuchMethod so use old implementation
  private fun createIdeaRepositoryDescriptorsWithoutSnapshots(repos: List<Repository>,
                                                              project: Project?): List<RemoteRepositoryDescription> {
    val repositories = createRepositories(repos, project)
    return try {
      repositories.map { RemoteRepositoryDescription(it.id, it.name, it.url, false) }
    }
    catch (t: Throwable) {
      repositories
    }
  }

  private fun createRepositories(repos: List<Repository>, project: Project?): List<RemoteRepositoryDescription> {
    //We need filter http addresses because it is unavailable https://blog.sonatype.com/central-repository-moving-to-https
    val filteredUrls = setOf("http://repo1.maven.org/maven2", "http://repository.jboss.org/nexus/content/repositories/public/")
    val defaultRepos = listOf(
      RemoteRepositoryDescription(
        "central",
        "Maven Central repository",
        "https://repo1.maven.org/maven2"
      ),
      RemoteRepositoryDescription(
        "spark-packages",
        "Spark Packages Repository",
        "https://repos.spark-packages.org/"
      )
    )
    val projectForResolve = project ?: ProjectManager.getInstance().openProjects.firstOrNull()
                            ?: ProjectManager.getInstance().defaultProject
    val ideaRepos = RemoteRepositoriesConfiguration.getInstance(projectForResolve).repositories
    val reposDesc = repos.map {
      RemoteRepositoryDescription(it.id, it.id, it.url.trim())
    }
    return defaultRepos + (reposDesc + ideaRepos).filter { repo ->
      filteredUrls.all { !repo.url.contains(it) }
    }
  }
}