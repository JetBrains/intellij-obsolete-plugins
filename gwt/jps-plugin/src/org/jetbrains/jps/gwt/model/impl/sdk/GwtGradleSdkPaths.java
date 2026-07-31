package org.jetbrains.jps.gwt.model.impl.sdk;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.PathUtilRt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gwt.model.GwtDependenciesResolver;
import org.jetbrains.jps.gwt.model.GwtSdkPaths;
import org.jetbrains.jps.gwt.model.MavenCoordinates;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.util.List;
import java.util.Set;

public class GwtGradleSdkPaths implements GwtSdkPaths {
  public static final @NonNls String TYPE_ID = "gradle";
  private final String myBasePath;//path to the GWT group directory (com.google.gwt or org.gwtproject) under .gradle/caches
  private final String myVersion;
  private final String myDevDirPath;//path to <group>/gwt-dev/<version>/

  public GwtGradleSdkPaths(String homeDirectoryPath, final String version) {
    myDevDirPath = homeDirectoryPath;
    myBasePath = PathUtilRt.getParentPath(PathUtilRt.getParentPath(homeDirectoryPath));
    myVersion = version;
  }

  public static @NotNull String getSdkUrl(@NotNull String pathToGwtJar, boolean fromMaven) {
    File gwtJar = new File(pathToGwtJar);
    File gwtJarDir = fromMaven ? gwtJar.getParentFile() : gwtJar.getParentFile().getParentFile();
    String version = gwtJarDir.getName();
    File gwtDevDir = new File(gwtJarDir.getParentFile().getParentFile(), GWT_DEV_ARTIFACT_ID + "/" + version);
    return JpsPathUtil.pathToUrl(FileUtil.toSystemIndependentName(gwtDevDir.getAbsolutePath()));
  }

  @Override
  public @NotNull String getDevJarPath(final boolean systemIndependent) {
    return getArtifactPath(GWT_DEV_ARTIFACT_ID);
  }

  private @NotNull String getArtifactPath(String name) {
    String path = getArtifactPath(new File(myBasePath), name, myVersion, null);
    if (path != null) {
      return path;
    }
    return new File(myBasePath, name + "/" + myVersion).getAbsolutePath();
  }

  @Override
  public String getCodeServerJarPath() {
    return getArtifactPath("gwt-codeserver");
  }

  @Override
  public @NotNull String getHomeDirectoryUrl() {
    return JpsPathUtil.pathToUrl(myDevDirPath);
  }

  @Override
  public @NotNull String getServletJarPath() {
    return getArtifactPath("gwt-servlet");
  }

  private static @Nullable String getArtifactPath(@NotNull File baseDir, @NotNull String name, @NotNull String version, @Nullable String suffix) {
    File[] subDirs = new File(baseDir, name + "/" + version).listFiles();
    if (subDirs != null) {
      for (File subDir : subDirs) {
        File jar = new File(subDir, name + "-" + version + (suffix != null ? "-" + suffix : "") + ".jar");
        if (jar.exists()) {
          return jar.getAbsolutePath();
        }
      }
    }
    return null;
  }

  @Override
  public @NotNull String getUserJarPath() {
    return getArtifactPath(GWT_USER_ARTIFACT_ID);
  }

  @Override
  public List<String> getGwtUserDependencies(@NotNull GwtDependenciesResolver dependenciesResolver) {
    return resolveDependencies(dependenciesResolver, GWT_USER_ARTIFACT_ID, myVersion);
  }

  @Override
  public @NotNull List<String> getGwtDevDependencies(@NotNull GwtDependenciesResolver dependenciesResolver) {
    return resolveDependencies(dependenciesResolver, GWT_DEV_ARTIFACT_ID, myVersion);
  }

  private static List<String> resolveDependencies(@NotNull GwtDependenciesResolver dependenciesResolver,
                                                  String artifactId,
                                                  String version) {
    return dependenciesResolver.resolveAnyOf(Set.of(
      new MavenCoordinates(OLD_GROUP_ID, artifactId, version),
      new MavenCoordinates(NEW_GROUP_ID, artifactId, version)
    ));
  }
}
