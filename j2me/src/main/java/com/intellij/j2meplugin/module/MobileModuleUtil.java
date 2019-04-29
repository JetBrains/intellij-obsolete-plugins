/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module;

import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MobileModuleUtil {
  private MobileModuleUtil() {
  }
  /*public static MobileModuleSettings getMobileModuleSettings(Module module, MobileApplicationType mobileApplicationType) {
    MobileModuleSettings[] existSettings = module.getComponents(MobileModuleSettings.class);
    for (int i = 0; existSettings != null && i < existSettings.length; i++) {
      if (existSettings[i].getMobileApplicationType().equals(mobileApplicationType)) {
        return existSettings[i];
      }
    }
    return null;
  }*/

  @NotNull
  public static List<MobileApplicationType> getExistingMobileApplicationTypes() {
    return MobileApplicationType.MOBILE_APPLICATION_TYPE.getExtensionList();
  }

  @Nullable
  public static MobileApplicationType getMobileApplicationTypeByName(String name) {
    for (MobileApplicationType applicationType : getExistingMobileApplicationTypes()) {
      if (Comparing.strEqual(name, applicationType.getName())) {
        return applicationType;
      }
    }
    return null;
  }

  public static boolean isExecutable(@Nullable PsiClass psiClass, @NotNull final Module module) {
    if (psiClass != null) {
      if (psiClass.getNameIdentifier() == null) return false; //exclude anonymous classes
      if (module == ModuleUtilCore.findModuleForPsiElement(psiClass)) {
        final J2MEModuleProperties moduleProperties = J2MEModuleProperties.getInstance(module);
        if (moduleProperties != null) {
          final Project project = module.getProject();
          final MobileApplicationType applicationType = moduleProperties.getMobileApplicationType();
          if (applicationType != null) {
            final String fqName = applicationType.getBaseClassName();
            final PsiClass baseClass = JavaPsiFacade.getInstance(project).findClass(fqName, GlobalSearchScope.allScope(project));
            if (baseClass != null && psiClass.isInheritor(baseClass, true)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

}
