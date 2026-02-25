// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.references.common.GrailsFileReferenceSetBase;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;

import java.util.List;

public class GspSrcJsAttributeSupport extends TagAttributeReferenceProvider {
  protected GspSrcJsAttributeSupport() {
    super("src", "g", new String[]{"javascript"});
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    List<String> attributeNames = gspTagWrapper.getAttributeNames();
    
    if (attributeNames.size() != 1) return PsiReference.EMPTY_ARRAY;

    VirtualFile root = GrailsFramework.getInstance().findAppRoot(element);
    final VirtualFile jsFolder = VfsUtil.findRelativeFile(root, "web-app", "js");
    if (jsFolder == null) return PsiReference.EMPTY_ARRAY;

    GrailsFileReferenceSetBase set = new GrailsFileReferenceSetBase(text, element, offset, null, true, true) {
      @Override
      protected VirtualFile getDefaultContext(boolean isAbsolute) {
        return jsFolder;
      }
    };

    return set.getAllReferences();
  }
}
