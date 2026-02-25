// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.html.impl;

import com.intellij.psi.PsiFile;
import com.intellij.xml.HtmlXmlExtension;

public final class GspHtmlXmlExtension extends HtmlXmlExtension {

  @Override
  public boolean isAvailable(PsiFile file) {
    return file instanceof GspHtmlFileImpl;
  }

}
