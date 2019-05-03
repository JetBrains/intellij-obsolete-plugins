/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
