/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.gwt.i18n;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class GwtI18nManagerImpl extends GwtI18nManager {
  private static final PropertiesFile[] EMPTY_PROPERTIES_FILE_ARRAY = new PropertiesFile[0];
  private final Project myProject;

  public GwtI18nManagerImpl(final Project project) {
    myProject = project;
  }

  private boolean isConstantsOrMessagesInterface(@NotNull PsiClass aClass) {
    if (!GwtFacet.isInModuleWithGwtFacet(myProject, getOriginalContainingFile(aClass).getVirtualFile()) || !aClass.isInterface()) {
      return false;
    }

    return isConstantsInterface(aClass) || isExtendingInterface(aClass, GwtI18nUtil.MESSAGES_INTERFACE_NAME);
  }

  private static boolean isExtendingInterface(final PsiClass aClass, final String superInterfaceName) {
    return aClass.isInterface() && InheritanceUtil.isInheritor(aClass, true, superInterfaceName);
  }

  @Override
  public boolean isConstantsInterface(final @NotNull PsiClass aClass) {
    return isExtendingInterface(aClass, GwtI18nUtil.CONSTANTS_INTERFACE_NAME);
  }

  @Override
  public boolean isLocalizableInterface(@NotNull PsiClass aClass) {
    return isExtendingInterface(aClass, GwtI18nUtil.LOCALIZABLE_INTERFACE_NAME);
  }

  @Override
  public PropertiesFile @NotNull [] getPropertiesFiles(@NotNull PsiClass anInterface) {
    PsiFile containingFile = getOriginalContainingFile(anInterface);
    final PsiDirectory mainDirectory = containingFile.getContainingDirectory();
    if (mainDirectory == null || !isConstantsOrMessagesInterface(anInterface)) {
      return EMPTY_PROPERTIES_FILE_ARRAY;
    }

    PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(mainDirectory);
    if (psiPackage == null) return EMPTY_PROPERTIES_FILE_ARRAY;

    PsiDirectory[] directories = psiPackage.getDirectories(anInterface.getResolveScope());
    List<PropertiesFile> files = new ArrayList<>();
    for (PsiDirectory directory : directories) {
    for (PsiFile psiFile : directory.getFiles()) {
        if (psiFile instanceof PropertiesFile propertiesFile) {
          final String fileName = propertiesFile.getName();
          final String interfaceName = anInterface.getName();
          if (isFileNameForInterfaceName(fileName, interfaceName)) {
            files.add(propertiesFile);
          }
        }
      }
    }
    return files.toArray(new PropertiesFile[0]);
  }

  private static PsiFile getOriginalContainingFile(final PsiClass anInterface) {
    return anInterface.getContainingFile().getOriginalFile();
  }

  private static boolean isFileNameForInterfaceName(final @Nullable String fileName, final @Nullable String interfaceName) {
    return fileName != null && interfaceName != null &&
        (fileName.equals(interfaceName + "." + PropertiesFileType.INSTANCE.getDefaultExtension()) ||
         StringUtil.startsWithConcatenation(fileName, interfaceName, "_"));
  }

  @Override
  public @Nullable PsiClass getPropertiesInterface(@NotNull PropertiesFile file) {
    final String fileName = file.getName();
    final PsiDirectory directory = file.getParent();
    final GwtFacet facet = GwtFacet.findFacetBySourceFile(myProject, file.getVirtualFile());
    if (directory == null || facet == null) {
      return null;
    }
    final PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(directory);
    if (psiPackage == null) return null;

    for (PsiClass psiClass : psiPackage.getClasses(GlobalSearchScope.moduleWithDependenciesScope(facet.getModule()))) {
      if (isFileNameForInterfaceName(fileName, psiClass.getName()) && isConstantsOrMessagesInterface(psiClass)) {
        return psiClass;
      }
    }

    return null;
  }

  @Override
  public IProperty @NotNull [] getProperties(@NotNull PsiMethod method) {
    final PsiClass aClass = method.getContainingClass();
    if (aClass == null) {
      return IProperty.EMPTY_ARRAY;
    }

    final PropertiesFile[] files = ReadAction.computeBlocking(() -> getPropertiesFiles(aClass));
    if (files.length == 0) {
      return IProperty.EMPTY_ARRAY;
    }

    final String propertyName = GwtI18nUtil.getPropertyName(method);
    List<IProperty> properties = new ArrayList<>();
    for (PropertiesFile file : files) {
      final IProperty property = file.findPropertyByKey(propertyName);
      if (property != null) {
        properties.add(property);
      }
    }
    return properties.toArray(IProperty.EMPTY_ARRAY);
  }

  @Override
  public @Nullable PsiMethod getMethod(@NotNull IProperty property) {
    final PsiClass psiClass = getPropertiesInterface(property.getPropertiesFile());
    if (psiClass == null) {
      return null;
    }

    String baseName = getBaseName(property);
    final PsiMethod[] psiMethods = psiClass.getMethods();
    for (PsiMethod psiMethod : psiMethods) {
      final String propertyName = GwtI18nUtil.getPropertyName(psiMethod);
      if (propertyName.equals(baseName)) {
        return psiMethod;
      }
    }

    return null;
  }

  private static @Nullable String getBaseName(IProperty property) {
    String key = property.getUnescapedKey();
    if (key == null) return null;
    int i = key.indexOf('[');
    return i != -1 ? key.substring(0, i) : key;
  }
}
