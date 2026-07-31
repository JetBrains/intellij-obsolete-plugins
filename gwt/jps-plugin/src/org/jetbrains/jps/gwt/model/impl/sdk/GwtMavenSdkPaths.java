package org.jetbrains.jps.gwt.model.impl.sdk;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.GwtDependenciesResolver;
import org.jetbrains.jps.gwt.model.GwtSdkPaths;
import org.jetbrains.jps.gwt.model.MavenCoordinates;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.util.List;
import java.util.Set;

import static com.intellij.util.PathUtilRt.getParentPath;

public class GwtMavenSdkPaths implements GwtSdkPaths {
  public static final @NonNls String TYPE_ID = "maven";
  private final String myBasePath;//path to repository/com/google/gwt
  private final String myVersion;
  private final String myDevDirPath;

  public GwtMavenSdkPaths(@NotNull String homeDirectoryPath, @NotNull String version) {
    myDevDirPath = homeDirectoryPath;
    myBasePath = getParentPath(getParentPath(homeDirectoryPath));
    myVersion = version;
  }

  public static String getPlatformId() {
    return SystemInfo.isMac ? "mac" : (SystemInfo.isLinux ? "linux" : "windows");
  }

  @Override
  public @NotNull String getDevJarPath(final boolean systemIndependent) {
    final String platformSuffix = VersionComparatorUtil.compare(myVersion, "2.0") >= 0 ? "" : "-" + getPlatformId();
    return new File(myDevDirPath, "gwt-dev-" + myVersion + platformSuffix + ".jar").getAbsolutePath();
  }

  @Override
  public String getCodeServerJarPath() {
    return getJarPath("gwt-codeserver");
  }

  @Override
  public @NotNull String getHomeDirectoryUrl() {
    return JpsPathUtil.pathToUrl(myDevDirPath);
  }

  @Override
  public @NotNull String getServletJarPath() {
    return getJarPath("gwt-servlet");
  }

  private String getJarPath(final String name) {
    return new File(myBasePath, name + "/" + myVersion + "/" + name + "-" + myVersion + ".jar").getAbsolutePath();
  }

  @Override
  public @NotNull String getUserJarPath() {
    return getJarPath(GWT_USER_ARTIFACT_ID);
  }

  @Override
  public List<String> getGwtUserDependencies(@NotNull GwtDependenciesResolver dependenciesResolver) {
    return resolveDependencies(dependenciesResolver, GWT_USER_ARTIFACT_ID, myVersion);
  }

  public String getVersion() {
    return myVersion;
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
