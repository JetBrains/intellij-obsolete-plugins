// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.references.common.GrailsFileReferenceSetBase;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;
import org.jetbrains.plugins.grails.util.GrailsUtils;

public class GspRResourceUriAttributeSupport extends TagAttributeReferenceProvider {


  protected GspRResourceUriAttributeSupport() {
    super("uri", "r", new String[]{"resource", "resourceLink", "external", "img"});
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    if (text.contains(":/") || !text.startsWith("/")) return PsiReference.EMPTY_ARRAY;

    VirtualFile root = GrailsFramework.getInstance().findAppRoot(element);
    if (root == null) return PsiReference.EMPTY_ARRAY;

    final VirtualFile webApp = root.findChild(GrailsUtils.webAppDir);
    if (webApp == null) return PsiReference.EMPTY_ARRAY;

    GrailsFileReferenceSetBase set = new GrailsFileReferenceSetBase(text, element, offset, null, true, true) {
      @Override
      protected VirtualFile getDefaultContext(boolean isAbsolute) {
        return webApp;
      }
    };

    return set.getAllReferences();
  }
}
