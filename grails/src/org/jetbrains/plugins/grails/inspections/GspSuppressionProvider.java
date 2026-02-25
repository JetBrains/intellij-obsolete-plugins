// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.inspections;

import com.intellij.codeInspection.DefaultXmlSuppressionProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;

final class GspSuppressionProvider extends DefaultXmlSuppressionProvider {
  @Override
  public boolean isProviderAvailable(@NotNull PsiFile file) {
    return file instanceof GspFile;
  }

  @Override
  protected PsiElement findFileSuppression(PsiElement anchor, String id, PsiElement originalElement) {
    final PsiFile file = anchor.getContainingFile();
    if (file instanceof XmlFile) {
      final XmlDocument document = ((XmlFile)file).getDocument();
      final XmlTag rootTag = document != null ? document.getRootTag() : null;
      PsiElement leaf = rootTag != null ? rootTag.getFirstChild() : file.findElementAt(0);
      return findSuppressionLeaf(leaf, id, 0);
    }
    return null;
  }

  @Override
  protected String getPrefix() {
    return "<%--" +
           DefaultXmlSuppressionProvider.SUPPRESS_MARK +
           " ";
  }

  @Override
  protected String getSuffix() {
    return " --%>";
  }
}
