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

package com.intellij.gwt.jsinject;

import com.intellij.gwt.jsinject.parser.GwtLanguageDialect;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageNamesValidation;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.lang.refactoring.NamesValidator;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class JsInjector implements MultiHostInjector {
  public static final String JSNI_COMMENT_PREFIX = "/*-{";
  public static final String JSNI_COMMENT_SUFFIX = "}-*/";

  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement host) {
    if (((PsiComment)host).getTokenType() == JavaTokenType.C_STYLE_COMMENT) {
      PsiComment comment = (PsiComment)host;
      String text = comment.getText();

      if (!isJsniCommentText(text)) return;

      final PsiElement parent = host.getParent();
      if (parent instanceof PsiMethod method) {
        if (method.getModifierList().hasExplicitModifier(PsiModifier.NATIVE)) {
          @NonNls StringBuilder prefix = new StringBuilder();
          Language language = GwtLanguageDialect.GWT_DIALECT;
          final NamesValidator namesValidator = LanguageNamesValidation.INSTANCE.forLanguage(language);
          prefix.append("function ");
          String funName = method.getName();
          if (namesValidator.isKeyword(funName, host.getProject())) {
            funName = "_" + funName;
          }
          prefix.append(funName);
          prefix.append(" ( ");
          final PsiParameter[] parameters = method.getParameterList().getParameters();
          for (int i = 0; i != parameters.length; ++i) {
            prefix.append(parameters[i].getName());
            prefix.append(",");
          }

          prefix.append("/*Window*/$wnd,/*Document*/$doc,$entry) ");

          String suffix = "";
          TextRange range = new TextRange(3, text.length() - 3);
          registrar.startInjecting(language)
            .addPlace(prefix.toString(), suffix, (PsiLanguageInjectionHost)host, range)
            .doneInjecting();
        }
      }
    }
  }

  private static boolean isJsniCommentText(String text) {
    return text.startsWith(JSNI_COMMENT_PREFIX) && text.endsWith(JSNI_COMMENT_SUFFIX);
  }

  public static boolean isJsniMethod(@NotNull PsiMethod psiMethod) {
    if (!psiMethod.getModifierList().hasExplicitModifier(PsiModifier.NATIVE)) {
      return false;
    }

    final PsiElement[] elements = psiMethod.getChildren();
    for (PsiElement element : elements) {
      if (element instanceof PsiComment && ((PsiComment)element).getTokenType() == JavaTokenType.C_STYLE_COMMENT
          && isJsniCommentText(element.getText())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Collections.singletonList(PsiComment.class);
  }
}
