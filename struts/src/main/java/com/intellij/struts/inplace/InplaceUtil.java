/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.jsp.jspXml.JspExpression;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * @author Dmitry Avdeev
 */
public class InplaceUtil {

  private InplaceUtil() {
  }

  /**
   * Checks whether the given PsiElement contains any EL or JSP expressions.
   *
   * @param psiElement Element to check.
   * @return true if no expressions present.
   */
  public static boolean isSimpleText(@NotNull final PsiElement psiElement) {
    return PsiTreeUtil.getChildOfType(psiElement, OuterLanguageElement.class) == null &&
           PsiTreeUtil.getChildOfType(psiElement, JspExpression.class) == null;
  }

  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(".*\\{\\d}.*");

  public static boolean containsPlaceholderReference(String text) {
    return StringUtil.isNotEmpty(text) &&
           PLACEHOLDER_PATTERN.matcher(text).matches();
  }
}
