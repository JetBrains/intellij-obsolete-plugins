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

package com.intellij.gwt.references;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class GwtModuleReferencesProvider extends PsiReferenceProvider {
  private final boolean myOutputName;

  public GwtModuleReferencesProvider(final boolean outputName) {
    myOutputName = outputName;
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, final @NotNull ProcessingContext context) {
    if (element instanceof XmlAttributeValue) {
      return new PsiReference[]{
        new GwtModuleInXmlAttributeReference((XmlAttributeValue)element, myOutputName)
      };
    }
    if (element instanceof PsiLiteralExpression) {
      return new PsiReference[]{
          new GwtModuleInStringLiteralReference((PsiLiteralExpression)element, myOutputName)
      };
    }
    return PsiReference.EMPTY_ARRAY;
  }
}
