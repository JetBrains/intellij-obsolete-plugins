/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.emulator;

import com.intellij.j2meplugin.emulator.midp.uei.UnifiedEmulatorType;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Comparing;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EmulatorUtil {
  private EmulatorUtil() {
  }

  @Nullable
  public static EmulatorType getEmulatorTypeByName(String name) {
    List<EmulatorType> knownEmulatorTypes = EmulatorType.EMULATOR_TYPE_EXTENSION.getExtensionList();
    for (EmulatorType type : knownEmulatorTypes) {
      if (Comparing.strEqual(type.getName(), name)) return type;
    }
    return null;
  }

  @Nullable
  public static EmulatorType getValidEmulatorType(String home){
    EmulatorType defaultEmulatorType = null;
    for (EmulatorType emulatorType : EmulatorType.EMULATOR_TYPE_EXTENSION.getExtensionList()) {
      if (emulatorType instanceof UnifiedEmulatorType) {
        defaultEmulatorType = emulatorType;
        continue;
      }
      if (emulatorType.isValidHomeDirectory(home)) return emulatorType;
    }
    return defaultEmulatorType != null && defaultEmulatorType.isValidHomeDirectory(home) ? defaultEmulatorType : null;
  }

  @Nullable
  public static String findFirstJavaSdk(){
    ProjectJdkTable table = ProjectJdkTable.getInstance();
    final Sdk[] allJdks = table.getAllJdks();
    for (Sdk jdk : allJdks) {
      if (Comparing.equal(jdk.getSdkType(), JavaSdk.getInstance())){
        return jdk.getName();
      }
    }
    return null;
  }
}
