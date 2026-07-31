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

package com.intellij.gwt.references.search;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.lang.xhtml.XHTMLLanguage;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

public final class GwtToHtmlTagIdReferencesSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
  public GwtToHtmlTagIdReferencesSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
    final PsiElement element = queryParameters.getElementToSearch();
    if (!(element instanceof XmlAttributeValue)) return;

    final PsiElement parent = element.getParent();
    if (!(parent instanceof XmlAttribute) ||
        !"id".equals(((XmlAttribute)parent).getLocalName())) return;
    String id = ((XmlAttributeValue)element).getValue();
    if (StringUtil.isEmpty(id)) return;

    final PsiElement tag = parent.getParent();
    if (!(tag instanceof XmlTag)) return;

    final PsiFile file = parent.getContainingFile();
    if (!file.getLanguage().equals(HTMLLanguage.INSTANCE) && !file.getLanguage().equals(XHTMLLanguage.INSTANCE)) return;

    final VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return;

    final Module module = ModuleUtilCore.findModuleForPsiElement(file);
    if (module == null || GwtFacet.getInstance(module) == null) return;

    queryParameters.getOptimizer().searchWord(id, queryParameters.getEffectiveSearchScope(), UsageSearchContext.IN_STRINGS, true, tag);
  }
}
