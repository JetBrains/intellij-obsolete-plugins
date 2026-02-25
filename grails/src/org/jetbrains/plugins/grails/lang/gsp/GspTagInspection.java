// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.xml.XmlTagRuleProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag.GspTagRuleProvider;

public final class GspTagInspection extends LocalInspectionTool {

  private final GspTagRuleProvider ruleProvider = new GspTagRuleProvider();

  @Override
  public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, final boolean isOnTheFly) {
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (element instanceof GspGrailsTag tag) {
          for (XmlTagRuleProvider.Rule rule : ruleProvider.getTagRule(tag)) {
            rule.annotate(tag, holder);
          }
        }
      }
    };
  }
}
