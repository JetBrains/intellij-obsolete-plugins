/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.inject;

import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.lang.javascript.injections.JSInXmlLanguagesInjector;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.struts.inplace.Filters;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Support JavaScript for all applicable attributes of Struts HTML-taglib.
 *
 * @author Dmitry Avdeev
 */
public class StrutsJavascriptInjector implements MultiHostInjector {

  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement host) {
    // operate only in JSP(X) files
    final FileType fileType = host.getContainingFile().getFileType();
    if (fileType != StdFileTypes.JSP &&
        fileType != StdFileTypes.JSPX) {
      return;
    }

    if (host instanceof XmlAttributeValue) {
      final PsiElement tag = host.getParent().getParent();
      if (Filters.NAMESPACE_TAGLIB_STRUTS_HTML.isAcceptable(tag, null)) {
        @NonNls final String name = ((XmlAttribute) host.getParent()).getName();
        if (name.startsWith("on")) {
          JSInXmlLanguagesInjector.injectJSIntoAttributeValue(registrar, (XmlAttributeValue)host, false);
        }
      }
    }
  }

  @Override
  @NotNull
  public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Arrays.asList(XmlAttributeValue.class);
  }
}
