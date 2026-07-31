package org.jetbrains.jps.gwt.model;

import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * This interface is used to resolve dependencies of GWT artifacts.
 */
public interface GwtDependenciesResolver {
  /**
   * Returns paths to JAR files of dependencies for the specified artifact. If the project was imported from Maven, the actual dependencies
   * of corresponding artifacts in Maven project are returned, otherwise all transitive dependencies of the specified artifacts are resolved
   * and downloaded from Maven repositories.
   *
   * @see org.jetbrains.jps.gwt.model.impl.sdk.JpsGwtDependenciesStorage
   */
  @NotNull
  List<String> resolveDependencies(@NlsSafe @NotNull String groupId, @NlsSafe @NotNull String artifactId, @NlsSafe @NotNull String version);

  @NotNull
  List<String> resolveAnyOf(@NotNull Collection<MavenCoordinates> alternatives);
}
