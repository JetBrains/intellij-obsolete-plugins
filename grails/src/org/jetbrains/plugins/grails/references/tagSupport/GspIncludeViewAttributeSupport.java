// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.common.GrailsFileReferenceSetBase;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;
import org.jetbrains.plugins.grails.util.GrailsUtils;

public class GspIncludeViewAttributeSupport extends TagAttributeReferenceProvider {
  protected GspIncludeViewAttributeSupport() {
    super("view", "g", new String[]{"include"});
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    final VirtualFile viewsDirectory = GrailsUtils.findViewsDirectory(element);
    if (viewsDirectory == null) return PsiReference.EMPTY_ARRAY;

    GrailsFileReferenceSetBase set = new GrailsFileReferenceSetBase(text, element, offset, null, true, true) {
      @Override
      protected VirtualFile getDefaultContext(boolean isAbsolute) {
        return viewsDirectory;
      }
    };

    return set.getAllReferences();
  }
}
