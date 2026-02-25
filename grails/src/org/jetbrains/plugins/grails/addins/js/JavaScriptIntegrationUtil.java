// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.addins.js;

import com.intellij.lang.javascript.JSElementType;
import com.intellij.lang.javascript.psi.JSElement;
import com.intellij.lang.javascript.psi.JSEmbeddedContent;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.plugins.grails.addins.GrailsIntegrationUtil;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterHtmlElement;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;

public final class JavaScriptIntegrationUtil {

  private static final String JS_GRAILS_TAG_NAME1 = "g:javascript";
  private static final String JS_GRAILS_TAG_NAME2 = "r:script";

  private JavaScriptIntegrationUtil() {
  }

  public static boolean isJSEmbeddedContent(final PsiElement element) {
    return element instanceof JSEmbeddedContent;
  }

  public static boolean isJSElementType(IElementType type) {
    return GrailsIntegrationUtil.isJsSupportEnabled() && type instanceof JSElementType;
  }

  public static boolean isJSElement(PsiElement element) {
    return GrailsIntegrationUtil.isJsSupportEnabled() && element instanceof JSElement;
  }

  public static boolean isInjectAvailable(PsiElement element) {
    return element instanceof GspOuterHtmlElement;
  }

  public static boolean isJsInjectionTag(String tagName) {
    return JS_GRAILS_TAG_NAME1.equals(tagName) || JS_GRAILS_TAG_NAME2.equals(tagName);
  }

  public static boolean isJavaScriptInjection(PsiElement element) {
    if (isInjectAvailable(element) && GrailsIntegrationUtil.isJsSupportEnabled()) {
      PsiElement parent = element.getParent();
      if (!(parent instanceof GspGrailsTag)) return false;

      return isJsInjectionTag(((GspGrailsTag)parent).getName());
    }
    return false;
  }
}
