// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.i18n;

import com.intellij.codeInspection.i18n.JavaI18nizeQuickFixDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.uast.UExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

abstract class GrailsI18nizeQuickFixDialog extends JavaI18nizeQuickFixDialog<UExpression> {

  GrailsI18nizeQuickFixDialog(@NotNull Project project,
                              final @NotNull PsiFile context,
                              @NotNull String defaultPropertyValue) {
    super(project, context, null, defaultPropertyValue, null, false, true);
  }

  protected abstract @Nullable String getArgs();

  @Override
  protected void addAdditionalAttributes(Map<String, String> attributes) {
    attributes.put("ARGS_KEY", getArgs());
    attributes.put(PROPERTY_VALUE_ATTR, myDefaultPropertyValue);
  }

  @TestOnly
  public String getDefaultPropertyValue() {
    return myDefaultPropertyValue;
  }

  @Override
  protected List<String> defaultSuggestPropertiesFiles() {
    VirtualFile i18nFile = GrailsUtils.findI18nDirectory(myContext);
    if (i18nFile == null) return super.defaultSuggestPropertiesFiles();

    List<String> res = new ArrayList<>();

    for (VirtualFile virtualFile : i18nFile.getChildren()) {
      if (virtualFile.getName().endsWith(".properties")) {
        res.add(virtualFile.getPath());
      }
    }

    Collections.sort(res);

    return res;
  }

  @Override
  protected abstract @NotNull String getTemplateName();

}
