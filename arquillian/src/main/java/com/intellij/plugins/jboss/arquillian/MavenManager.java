package com.intellij.plugins.jboss.arquillian;

import com.intellij.execution.ExecutionException;
import com.intellij.jarRepository.JarRepositoryManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.JBIterable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.utils.library.RepositoryLibraryProperties;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class MavenManager {
  static MavenManager instance = new MavenManager();
  ConcurrentHashMap<String, List<String>> artifactDependentFilesMap = new ConcurrentHashMap<>();

  private MavenManager() {
  }

  static public MavenManager getInstance() {
    return instance;
  }

  private static boolean checkExistence(List<String> jars) {
    for (String jar : jars) {
      File file = new File(jar);
      if (!file.exists() || !file.isFile()) {
        return false;
      }
    }
    return true;
  }

  public List<String> getOrLoadMavenArtifactJars(@NotNull Project project,
                                                 @NotNull final String groupId,
                                                 @NotNull final String artifactId,
                                                 @NotNull String version,
                                                 boolean downloadSources,
                                                 boolean downloadJavaDocs) throws ExecutionException {
    String key = groupId + ":" + artifactId + ":" + version;
    List<String> jars = artifactDependentFilesMap.get(key);
    if (jars != null && checkExistence(jars)) {
      return jars;
    }

    final Collection<OrderRoot> roots = JarRepositoryManager.loadDependenciesModal(
      project,
      new RepositoryLibraryProperties(groupId, artifactId, version),
      downloadSources,
      downloadJavaDocs,
      null, null
    );

    if (roots == null || roots.isEmpty()) {
      throw new ExecutionException(MavenDependenciesBundle.message("arquillian.maven.dependencies.load.failed", key));
    }

    jars = JBIterable.from(roots).transform(root -> PathUtil.toPresentableUrl(root.getFile().getUrl())).toList();
    artifactDependentFilesMap.put(key, jars);
    return jars;
  }

  public List<String> getMavenArtifactJars(@NotNull String groupId,
                                           @NotNull String artifactId,
                                           @NotNull String version) {
    String key = groupId + ":" + artifactId + ":" + version;
    return artifactDependentFilesMap.get(key);
  }
}
