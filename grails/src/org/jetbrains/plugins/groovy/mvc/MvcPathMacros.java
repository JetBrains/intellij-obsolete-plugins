// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.groovy.mvc;

import com.intellij.openapi.application.PathMacroContributor;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public final class MvcPathMacros implements PathMacroContributor {
  @Override
  public void registerPathMacros(@NotNull Map<String, String> macros, @NotNull Map<String, String> legacyMacros) {
    Set<String> macroNames = macros.keySet();
    for (String framework : ContainerUtil.ar("grails", "griffon")) {
      String name = "USER_HOME_" + StringUtil.toUpperCase(framework);
      // OK, it may appear/disappear during the application lifetime, but we ignore that for now. Restart will help anyway
      if (!macroNames.contains(name)) {
        legacyMacros.put(name, StringUtil.trimEnd(getSdkWorkDirParent(framework), "/"));
      }
    }
  }

  public static @NotNull String getSdkWorkDirParent(String framework) {
    String grailsWorkDir = System.getProperty(framework + ".work.dir");
    if (StringUtil.isNotEmpty(grailsWorkDir)) {
      grailsWorkDir = FileUtil.toSystemIndependentName(grailsWorkDir);
      if (!grailsWorkDir.endsWith("/")) {
        grailsWorkDir += "/";
      }
      return grailsWorkDir;
    }

    return StringUtil.trimEnd(FileUtil.toSystemIndependentName(SystemProperties.getUserHome()), "/") + "/." + framework + "/";
  }
}
