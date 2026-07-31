package org.jetbrains.jps.gwt.model.impl.sdk;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.PathUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.GwtDependenciesResolver;
import org.jetbrains.jps.gwt.model.GwtSdkPaths;
import org.jetbrains.jps.util.JpsPathUtil;

import java.util.Collections;
import java.util.List;

public class GwtVaadinMavenSdkPaths implements GwtSdkPaths {
  public static final String TYPE_ID = "Vaadin-Maven";
  private final String myBasePath;//path to repository/com/vaadin
  private final String myClientDirPath;
  private final String myVersion;

  public GwtVaadinMavenSdkPaths(@NotNull String homePath) {
    myClientDirPath = homePath;
    myBasePath = PathUtilRt.getParentPath(PathUtilRt.getParentPath(homePath));
    myVersion = getVersion(homePath);
  }

  public String getVersion() {
    return myVersion;
  }

  public static String getVersion(String homePath) {
    return PathUtilRt.getFileName(homePath);
  }

  @Override
  public @NotNull String getDevJarPath(boolean systemIndependent) {
    return getJarPath("vaadin-client-compiler");
  }

  private String getJarPath(String artifactName) {
    return FileUtil.toSystemDependentName(myBasePath + "/" + artifactName + "/" + myVersion + "/" + artifactName + "-" + myVersion + ".jar");
  }

  @Override
  public @NotNull String getServletJarPath() {
    return getJarPath("vaadin-server");
  }

  @Override
  public @NotNull String getUserJarPath() {
    return getJarPath("vaadin-client");
  }

  @Override
  public List<String> getGwtUserDependencies(@NotNull GwtDependenciesResolver dependenciesResolver) {
    return Collections.emptyList();
  }

  @Override
  public String getCodeServerJarPath() {
    return null;
  }

  @Override
  public @NotNull List<String> getGwtDevDependencies(@NotNull GwtDependenciesResolver dependenciesResolver) {
    return dependenciesResolver.resolveDependencies("com.vaadin", "vaadin-client-compiler", myVersion);
  }

  @Override
  public @NotNull String getHomeDirectoryUrl() {
    return JpsPathUtil.pathToUrl(FileUtil.toSystemIndependentName(myClientDirPath));
  }
}
