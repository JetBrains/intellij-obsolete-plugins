// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spring.osgi.patterns;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.spring.model.actions.generate.SpringBeanGenerateProvider;
import com.intellij.spring.model.xml.beans.SpringBean;
import com.intellij.util.xml.DomElement;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public class OsgiSpringBeanGenerateProvider extends SpringBeanGenerateProvider {
  public static final String OSGI_NS_PREFIX = "OSGI_NS_PREFIX";

  public OsgiSpringBeanGenerateProvider(@NlsActions.ActionText String text, @NonNls String template) {
    super(text, template);
  }

  @Override
  protected Map<String, String> getPredefinedVars(@Nullable DomElement parentDomElement,
                                                  @Nullable SpringBean springBean,
                                                  @NotNull Editor editor,
                                                  @NotNull PsiFile file) {
    Map<String, String> map = super.getPredefinedVars(parentDomElement, springBean, editor,
                                                      file);

    return addOsgiNamespacePrefix(file, map);
  }

  public static Map<String, String> addOsgiNamespacePrefix(@NotNull PsiFile file,@NotNull Map<String, String> map) {
    if (file instanceof XmlFile) {
      String prefix = getOsgiNamespacePrefix((XmlFile)file);
      if (!StringUtil.isEmptyOrSpaces(prefix)) {
           map.put(OSGI_NS_PREFIX, prefix+":");
      }
    }
    return map;
  }

  public static @Nullable String getOsgiNamespacePrefix(@NotNull XmlFile xmlFile) {
    return XmlUtil.findNamespacePrefixByURI(xmlFile, "http://www.springframework.org/schema/osgi");
  }
}
