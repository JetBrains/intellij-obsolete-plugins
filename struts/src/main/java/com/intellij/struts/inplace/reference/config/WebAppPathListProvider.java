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

package com.intellij.struts.inplace.reference.config;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PathListReferenceProvider;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * Provides support for config file references in {@code web.xml} ActionServlet-declaration.
 *
 * @author davdeev
 */
public class WebAppPathListProvider extends PathListReferenceProvider {

  @Override
  @NotNull
  public PsiReference[] getReferencesByElement(@NotNull PsiElement element) {
    // filter cases here
    // TODO use PsiFilters instead? -- YC
    XmlTag parent = ((XmlTag)element).getParentTag();
    if (parent != null && parent.getName().equals("init-param")) {
      XmlTag nameTag = parent.findFirstSubTag("param-name");
      if (nameTag != null) {
        String paramName = nameTag.getValue().getText();
        if (paramName.equals("config") || paramName.startsWith("config/")) {

          XmlTag servletTag = parent.getParentTag();
          if (servletTag != null && servletTag.getName().equals("servlet")) {
            XmlTag classTag = servletTag.findFirstSubTag("servlet-class");
            if (classTag != null) {
              String className = classTag.getValue().getText().trim();
              GlobalSearchScope scope = GlobalSearchScope.allScope(element.getProject());
              PsiClass servletClass = JavaPsiFacade.getInstance(element.getProject()).findClass(className, scope);
              if (servletClass != null) {
                PsiClass actionServletClass =
                  JavaPsiFacade.getInstance(element.getProject()).findClass("org.apache.struts.action.ActionServlet", scope);
                if (actionServletClass != null && InheritanceUtil.isInheritorOrSelf(servletClass, actionServletClass, true)) {
                  // TODO remove all files except valid struts-config.xml files  -- YC
                  return super.getReferencesByElement(element);
                }
              }
            }
          }
        }
      }
    }
    return PsiReference.EMPTY_ARRAY;
  }
}
