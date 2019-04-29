/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.i18n;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizableStringList;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jdom.Element;

import java.util.Iterator;

public class ResourceBundlesBean {
  public JDOMExternalizableStringList myResourceBundles = new JDOMExternalizableStringList();

  private final Project myProject;

  public ResourceBundlesBean(final Project project) {
    myProject = project;
  }

  public void registerResourceBundle(PsiClass resourceBundle) {
    myResourceBundles.add(resourceBundle.getQualifiedName());
  }

  public PsiClass getResourceBundle() {
    final PsiManager psiManager = PsiManager.getInstance(myProject);
    for (Iterator it = myResourceBundles.iterator(); it.hasNext();) {
      String bundle = (String)it.next();
      final PsiClass psiClass = JavaPsiFacade.getInstance(psiManager.getProject()).findClass(bundle, GlobalSearchScope.allScope(myProject));
      if (psiClass != null) {
        return psiClass;
      }
      it.remove(); //do not store outdated resource bundles
    }
    return null;
  }

  public void readExternal(Element element) throws InvalidDataException {
    DefaultJDOMExternalizer.readExternal(this, element);
  }

  public void writeExternal(Element element) {
    if (!myResourceBundles.isEmpty()) {
      DefaultJDOMExternalizer.writeExternal(this, element);
    }
  }
}
