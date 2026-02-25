// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.openapi.paths.PathReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;
import org.jetbrains.plugins.grails.references.common.WebAppFolderFileReferenceSet;

import java.util.Collection;
import java.util.Collections;

public class GspResourceContextPathAttributeSupport extends TagAttributeReferenceProvider {

  protected GspResourceContextPathAttributeSupport() {
    super("contextPath", "g", GspResourceDirAttributeSupport.TAGS);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    String trimedUrl = PathReference.trimPath(text);

    if (trimedUrl.trim().isEmpty()) return PsiReference.EMPTY_ARRAY;

    final FileReferenceSet set = new WebAppFolderFileReferenceSet(trimedUrl, element, offset, null, true, true) {
      @Override
      public @NotNull Collection<PsiFileSystemItem> computeDefaultContexts() {
        if (!isAbsolutePathReference()) {
          return Collections.emptySet();
        }

        return super.computeDefaultContexts();
      }
    };

    return set.getAllReferences();
  }

}
