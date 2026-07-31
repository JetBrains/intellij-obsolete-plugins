package org.jetbrains.jps.gwt.model.impl.sdk;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.GwtDependenciesResolver;
import org.jetbrains.jps.gwt.model.GwtSdkPaths;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class GwtSdkPathsImpl implements GwtSdkPaths {
  private final String myHomeDirectoryUrl;

  public GwtSdkPathsImpl(String homeDirectoryUrl) {
    myHomeDirectoryUrl = homeDirectoryUrl;
  }

  @Override
  public @NotNull String getDevJarPath(final boolean systemIndependent) {
    if (systemIndependent) {
      return GwtSdkPathUtil.getSystemIndependentDevJarPath(getHomeDirectoryPath());
    }
    return GwtSdkPathUtil.getSystemDependentDevJarPath(getHomeDirectoryPath());
  }

  public String getHomeDirectoryPath() {
    return FileUtil.toSystemDependentName(JpsPathUtil.urlToPath(myHomeDirectoryUrl));
  }

  @Override
  public @NotNull String getServletJarPath() {
    return getHomeDirectoryPath() + File.separator + "gwt-servlet.jar";
  }

  @Override
  public @NotNull String getUserJarPath() {
    return GwtSdkPathUtil.getUserJarPath(getHomeDirectoryPath());
  }

  @Override
  public List<String> getGwtUserDependencies(@NotNull GwtDependenciesResolver dependenciesResolver) {
    return GwtSdkPathUtil.findValidationJars(new File(getHomeDirectoryPath()));
  }

  @Override
  public String getCodeServerJarPath() {
    return GwtSdkPathUtil.getCodeServerJarPath(getHomeDirectoryPath());
  }

  @Override
  public @NotNull List<String> getGwtDevDependencies(@NotNull GwtDependenciesResolver dependenciesResolver) {
    return Collections.emptyList();
  }

  @Override
  public @NotNull String getHomeDirectoryUrl() {
    return myHomeDirectoryUrl;
  }
}
