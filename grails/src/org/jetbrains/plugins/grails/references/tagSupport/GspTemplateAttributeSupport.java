// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.GspFileViewProvider;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;
import org.jetbrains.plugins.grails.references.common.TemplateFileReferenceSet;
import org.jetbrains.plugins.grails.util.GrailsUtils;

public class GspTemplateAttributeSupport extends TagAttributeReferenceProvider {

  protected GspTemplateAttributeSupport() {
    super("template", "g", null);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         final @NotNull String text,
                                                         final int offset,
                                                         final @NotNull GspTagWrapper gspTagWrapper) {

    final String controllerName;

    PsiFile psiFile = element.getContainingFile();
    VirtualFile file;
    if (psiFile != null && psiFile.getViewProvider() instanceof GspFileViewProvider && (file = psiFile.getOriginalFile().getVirtualFile()) != null) {
      controllerName = GrailsUtils.getExistingControllerNameDirByGsp(file, psiFile.getProject());
    }
    else {
      controllerName = null;
    }

    if (!text.startsWith("/") && controllerName == null) {
      return PsiReference.EMPTY_ARRAY;
    }

    PsiElement pluginAttribute = gspTagWrapper.getAttributeValue("plugin");

    if (pluginAttribute != null) {
      if (gspTagWrapper.getAttributeText(pluginAttribute) == null) {
        return PsiReference.EMPTY_ARRAY;
      }
    }
    else {
      PsiElement contextPathAttribute = gspTagWrapper.getAttributeValue("contextPath");
      if (contextPathAttribute != null) {
        if (gspTagWrapper.getAttributeText(contextPathAttribute) == null) {
          return PsiReference.EMPTY_ARRAY;
        }
      }
    }

    TemplateFileReferenceSet set = new TemplateFileReferenceSet(controllerName, PathReference.trimPath(text), element, offset, null, true, true, gspTagWrapper);

    return set.getAllReferences();
  }

}
