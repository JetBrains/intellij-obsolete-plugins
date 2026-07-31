/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.application.ReadAction;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;
import org.jetbrains.annotations.NotNull;

public final class PropertiesSearcher implements QueryExecutor<PsiElement, PsiElement> {
  @Override
  public boolean execute(final @NotNull PsiElement sourceElement, final @NotNull Processor<? super PsiElement> consumer) {
    if (sourceElement instanceof PsiMethod) {
      IProperty[] properties = ReadAction.computeBlocking(() ->
        GwtI18nManager.getInstance(sourceElement.getProject()).getProperties((PsiMethod)sourceElement)
      );
      for (IProperty property : properties) {
        if (!consumer.process(property.getPsiElement())) {
          return false;
        }
      }
      return true;
    }
    else if (sourceElement instanceof PsiClass) {
      final PropertiesFile[] files =
        ReadAction.computeBlocking(() -> GwtI18nManager.getInstance(sourceElement.getProject()).getPropertiesFiles((PsiClass)sourceElement));
      for (PropertiesFile file : files) {
        if (!consumer.process(file.getContainingFile())) {
          return false;
        }
      }
      return true;
    }
    return true;
  }
}
