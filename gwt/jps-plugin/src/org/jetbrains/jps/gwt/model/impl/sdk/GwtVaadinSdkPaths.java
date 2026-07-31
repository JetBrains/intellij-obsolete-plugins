package org.jetbrains.jps.gwt.model.impl.sdk;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.GwtDependenciesResolver;
import org.jetbrains.jps.gwt.model.GwtSdkPaths;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class GwtVaadinSdkPaths implements GwtSdkPaths {
  public static final String TYPE_ID = "Vaadin";
  private final String myVaadinHome;

  public GwtVaadinSdkPaths(@NotNull String vaadinHome) {
    myVaadinHome = vaadinHome;
  }

  @Override
  public @NotNull String getDevJarPath(boolean systemIndependent) {
    return findJarByBaseName("vaadin-client-compiler-");
  }

  @Override
  public @NotNull String getServletJarPath() {
    return findJarByBaseName("vaadin-server-");
  }

  private @NotNull String findJarByBaseName(final String namePrefix) {
    File home = new File(myVaadinHome);
    File[] children = home.listFiles();
    if (children != null) {
      for (File file : children) {
        String fileName = file.getName();
        if (fileName.startsWith(namePrefix) && fileName.endsWith(".jar") && Character.isDigit(fileName.charAt(namePrefix.length()))) {
          return file.getAbsolutePath();
        }
      }
    }
    return namePrefix + "snapshot";
  }

  @Override
  public @NotNull String getUserJarPath() {
    return findJarByBaseName("vaadin-client-");
  }

  @Override
  public List<String> getGwtUserDependencies(@NotNull GwtDependenciesResolver dependenciesResolver) {
    return GwtSdkPathUtil.findValidationJars(new File(myVaadinHome, "lib"));
  }

  @Override
  public String getCodeServerJarPath() {
    return null;
  }

  @Override
  public @NotNull List<String> getGwtDevDependencies(@NotNull GwtDependenciesResolver dependenciesResolver) {
    return Collections.emptyList();
  }

  @Override
  public @NotNull String getHomeDirectoryUrl() {
    return JpsPathUtil.pathToUrl(FileUtil.toSystemIndependentName(myVaadinHome));
  }
}
