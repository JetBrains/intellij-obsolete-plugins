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
