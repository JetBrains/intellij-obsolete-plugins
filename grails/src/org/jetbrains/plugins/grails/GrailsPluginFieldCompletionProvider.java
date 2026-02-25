// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.EqTailType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.TailTypeDecorator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsUtils;

public class GrailsPluginFieldCompletionProvider extends CompletionProvider<CompletionParameters> {

  // #CHECK# Find usages of GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue() in DefaultGrailsPlugin
  public static final String[] VARIANTS = {
    "autor", "title", "description", "grailsVersion", "version", "documentation", "pluginExcludes", "dependsOn", "loadAfter",
    "watchedResources", "artefacts", "doWithSpring", "doWithDynamicMethods", "doWithApplicationContext", "onChange",
    "onConfigChangeListener", "onShutdownListener", "influences", "observe", "scopes", "environments", "evict", "loadBefore", "status",
    "providedArtefacts", "typeFilters"
  };

  @Override
  protected void addCompletions(@NotNull CompletionParameters parameters,
                                @NotNull ProcessingContext context,
                                @NotNull CompletionResultSet result) {
    PsiFile file = parameters.getOriginalFile();
    if (!file.getName().endsWith("GrailsPlugin.groovy")) return;

    PsiClass aClass = PsiTreeUtil.getParentOfType(parameters.getPosition(), PsiClass.class);

    if (!GrailsUtils.isGrailsPluginClass(aClass)) return;
    assert aClass != null;

    for (String variant : VARIANTS) {
      if (aClass.findFieldByName(variant, false) == null) {
        result.addElement(TailTypeDecorator.withTail(LookupElementBuilder.create(variant), EqTailType.INSTANCE));
      }
    }
  }
}
