/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.idea.devkit.ant;

import com.intellij.compiler.ant.BuildProperties;
import org.jetbrains.annotations.NonNls;

/**
 * @author nik
 */
public class PluginBuildProperties {
  private PluginBuildProperties() {
  }

  @NonNls
  public static String getBuildJarTargetName(final String configurationName) {
    return "plugin.build.jar." + BuildProperties.convertName(configurationName);
  }

  @NonNls
  public static String getJarPathProperty(final String configurationName) {
    return BuildProperties.convertName(configurationName) + ".plugin.path.jar";
  }

}
