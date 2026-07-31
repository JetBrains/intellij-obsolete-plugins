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
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class GwtToHtmlTagReference extends BaseGwtReference<PsiLiteralExpression> {
  public GwtToHtmlTagReference(final PsiLiteralExpression element) {
    super(element);
  }

  @Override
  public PsiElement resolve() {
    final Object value = myElement.getValue();
    if (!(value instanceof String id)) return null;

    XmlFile[] htmlFiles = getHtmlFilesForModule();

    for (XmlFile htmlFile : htmlFiles) {
      final PsiElement tag = myGwtModulesManager.findTagById(htmlFile, id);
      if (tag != null) {
        return tag;
      }
    }
    return null;
  }

  @Override
  public String @NotNull [] getVariants() {
    final XmlFile[] files = getHtmlFilesForModule();
    Set<String> variants = new LinkedHashSet<>();
    for (XmlFile file : files) {
      variants.addAll(Arrays.asList(myGwtModulesManager.getAllIds(file)));
    }
    return ArrayUtilRt.toStringArray(variants);
  }
}
