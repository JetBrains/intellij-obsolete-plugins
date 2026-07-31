package org.jetbrains.jps.gwt.model.impl.sdk;

import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class GwtSdkPathUtil {
  public static final @NonNls String GWT_DEV_JAR = "gwt-dev.jar";
  private static final @NonNls String GWT_DEV_WINDOWS_JAR = "gwt-dev-windows.jar";
  private static final @NonNls String GWT_DEV_LINUX_JAR = "gwt-dev-linux.jar";
  private static final @NonNls String GWT_DEV_MAC_JAR = "gwt-dev-mac.jar";

  public static @NotNull List<String> findValidationJars(File parent) {
    File[] files = parent.listFiles();
    List<String> paths = new ArrayList<>();
    if (files != null) {
      for (File file : files) {
        String name = file.getName();
        if (name.startsWith("validation-api-") && name.endsWith(".jar")) {
          paths.add(file.getAbsolutePath());
        }
      }
    }
    return paths;
  }

  public static String getCodeServerJarPath(String homePath) {
    return homePath + File.separator + "gwt-codeserver.jar";
  }

  public static String getUserJarPath(String homePath) {
    return homePath + File.separator + "gwt-user.jar";
  }

  public static String getSystemDependentDevJarName() {
    final String jarName;
    if (SystemInfo.isWindows) {
      jarName = GWT_DEV_WINDOWS_JAR;
    }
    else if (SystemInfo.isMac) {
      jarName = GWT_DEV_MAC_JAR;
    }
    else {
      jarName = GWT_DEV_LINUX_JAR;
    }
    return jarName;
  }

  public static String getDevJarPath(String homePath) {
    String newPath = getSystemIndependentDevJarPath(homePath);
    if (new File(newPath).exists()) {
      return newPath;
    }
    String oldPath = getSystemDependentDevJarPath(homePath);
    if (new File(oldPath).exists()) {
      return oldPath;
    }
    return newPath;
  }

  public static String getSystemIndependentDevJarPath(String homePath) {
    return homePath + File.separator + GWT_DEV_JAR;
  }

  public static String getSystemDependentDevJarPath(String homePath) {
    return homePath + File.separator + getSystemDependentDevJarName();
  }
}
