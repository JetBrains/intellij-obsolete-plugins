// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInspection.incomplete;

import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesUtil;
import com.intellij.lang.properties.ResourceBundle;
import com.intellij.lang.properties.codeInspection.IncompletePropertyInspection;
import com.intellij.lang.properties.editor.ResourceBundleEditorBundle;
import com.intellij.lang.properties.editor.inspections.ResourceBundleEditorProblemDescriptor;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class IncompletePropertyUtil {
  public static @Nullable IncompletePropertyInspection getInspection(@NotNull PsiElement element) {
    InspectionProfile profile = InspectionProjectProfileManager.getInstance(element.getProject()).getCurrentProfile();
    return (IncompletePropertyInspection)profile.getUnwrappedTool(IncompletePropertyInspection.TOOL_KEY, element);
  }

  public static boolean isPropertyComplete(@NotNull IncompletePropertyInspection inspection,
                                           @NotNull String key,
                                           @NotNull ResourceBundle resourceBundle) {
    IProperty[] properties = resourceBundle.getPropertiesFiles().stream()
      .map(f -> f.findPropertyByKey(key))
      .filter(Objects::nonNull)
      .toArray(IProperty[]::new);
    return isPropertyComplete(inspection, properties, resourceBundle);
  }

  public static @NotNull Function<IProperty[], ResourceBundleEditorProblemDescriptor[]> buildPropertyGroupVisitor(
    @NotNull ResourceBundle resourceBundle,
    @NotNull IncompletePropertyInspection inspection) {
    return properties -> isPropertyComplete(inspection, properties, resourceBundle)
                         ? null
                         : new ResourceBundleEditorProblemDescriptor[]{
                           new ResourceBundleEditorProblemDescriptor(
                             ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                             ResourceBundleEditorBundle.message("incomplete.property.inspection.description", properties[0].getName()))};
  }

  private static boolean isPropertyComplete(@NotNull IncompletePropertyInspection inspection,
                                            IProperty @NotNull [] properties,
                                            @NotNull ResourceBundle resourceBundle) {
    Set<PropertiesFile> existed = ContainerUtil.map2Set(properties, IProperty::getPropertiesFile);
    Set<String> ignoredSuffixes = getIgnoredSuffixes(inspection);
    for (PropertiesFile file : resourceBundle.getPropertiesFiles()) {
      if (!existed.contains(file) && !ignoredSuffixes.contains(PropertiesUtil.getSuffix(file))) {
        return false;
      }
    }
    return true;
  }

  private static @NotNull Set<String> getIgnoredSuffixes(@NotNull IncompletePropertyInspection inspection) {
    List<String> suffixes = inspection.suffixes;
    return suffixes == null ? Collections.emptySet() : new HashSet<>(suffixes);
  }

  private IncompletePropertyUtil() {
  }
}
