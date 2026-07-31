package org.jetbrains.jps.gwt.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface GwtSdkPaths {
  String OLD_GROUP_ID = "com.google.gwt";
  String NEW_GROUP_ID = "org.gwtproject";

  String GWT_DEV_ARTIFACT_ID = "gwt-dev";
  String GWT_USER_ARTIFACT_ID = "gwt-user";

  @NotNull
  String getDevJarPath(final boolean systemIndependent);

  @NotNull
  String getServletJarPath();

  @NotNull
  String getUserJarPath();

  /**
   * Returns paths to JAR files of dependencies of gwt-user artifact.
   */
  List<String> getGwtUserDependencies(@NotNull GwtDependenciesResolver dependenciesResolver);

  @Nullable
  String getCodeServerJarPath();

  /**
   * Returns paths to JAR files of dependencies of gwt-dev artifact.
   */
  @NotNull
  List<String> getGwtDevDependencies(@NotNull GwtDependenciesResolver dependenciesResolver);

  @NotNull
  String getHomeDirectoryUrl();

  static boolean isGwtMavenGroupId(String group) {
    return NEW_GROUP_ID.equals(group) || OLD_GROUP_ID.equals(group);
  }
}
