/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.compiler.ant;

import com.intellij.compiler.ant.BuildProperties;
import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import org.jetbrains.annotations.NonNls;

public class J2MEBuildProperties {
  private J2MEBuildProperties() {
  }

  public static @NonNls String getJarSizeProperty() {
    return "mobile.size.jar";
  }

  public static @NonNls String getJarPathProperty() {
    return "mobile.path.jar";
  }

  public static @NonNls String getExtensionPathProperty(final MobileApplicationType mobileApplicationType) {
    return "mobile.build." + mobileApplicationType.getExtension() + ".path";
  }

  public static @NonNls  String getManifestPath() {
    return "mobile.build.manifest";
  }

  public static @NonNls String getMobileBuildTargetName(@NonNls String moduleName) {
    return "mobile.build."+ BuildProperties.convertName(moduleName);
  }

  public static @NonNls String getJarBuildTargetName(@NonNls String moduleName) {
    return "mobile.build.jar."+ BuildProperties.convertName(moduleName);
  }

  public static @NonNls String getPreverifyTargetName(final String name) {
    return "mobile.preverify." + BuildProperties.convertName(name);
  }
}