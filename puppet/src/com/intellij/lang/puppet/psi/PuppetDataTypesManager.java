package com.intellij.lang.puppet.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PuppetDataTypesManager implements PuppetDataTypes {
  private static final Key<Map<String, PsiElement>> PUPPET_DATA_TYPES_ELEMENTS_KEY = new Key<>("puppet.data.types.map");

  public static @Nullable PsiElement getDataTypeLightElementByName(@NotNull Project project, @NotNull String name) {
    Map<String, PsiElement> puppetDataTypesMap = project.getUserData(PUPPET_DATA_TYPES_ELEMENTS_KEY);

    if (puppetDataTypesMap == null) {
      puppetDataTypesMap = createDataTypeLightElementsMap(project);
    }
    return puppetDataTypesMap.get(name);
  }

  private static @NotNull Map<String, PsiElement> createDataTypeLightElementsMap(@NotNull Project project) {
    PsiManager psiManager = PsiManager.getInstance(project);
    Map<String, PsiElement> newMap = new HashMap<>();

    for (@NonNls String typeName : ALL_DATA_TYPES) {
      newMap.put(StringUtil.toLowerCase(typeName), new PuppetDataTypeLightElement(psiManager, typeName));
    }

    project.putUserData(PUPPET_DATA_TYPES_ELEMENTS_KEY, newMap);
    return newMap;
  }
}
