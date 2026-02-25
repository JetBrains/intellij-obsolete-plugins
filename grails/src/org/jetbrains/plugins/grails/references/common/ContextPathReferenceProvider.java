// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ContextPathReferenceProvider extends PsiReferenceProvider {

  public static PsiReference[] createReferences(@NotNull PsiElement element) {
    TextRange range = ElementManipulators.getValueTextRange(element);
    int offset = range.getStartOffset();
    String text = range.substring(element.getText());
    
    return createReferences(element, text, offset);
  }
  
  public static PsiReference[] createReferences(@NotNull PsiElement element, @NotNull String text, int offset) {
    String trimedUrl = PathReference.trimPath(text);

    if (trimedUrl.trim().isEmpty()) return PsiReference.EMPTY_ARRAY;

    final FileReferenceSet set = new PluginSupportFileReferenceSet(trimedUrl, element, offset, null, true, false, true) {
      @Override
      public @NotNull Collection<PsiFileSystemItem> computeDefaultContexts() {
        if (!isAbsolutePathReference()) {
          return Collections.emptySet();
        }

        VirtualFile appDir = GrailsFramework.getInstance().findAppDirectory(getElement());
        if (appDir == null) return Collections.emptySet();

        PsiManager manager = getElement().getManager();
        List<PsiFileSystemItem> res = new ArrayList<>(2);

        VirtualFile view = appDir.findChild(GrailsUtils.VIEWS_DIRECTORY);
        if (view != null) {
          ContainerUtil.addIfNotNull(res, manager.findDirectory(view));
        }

        VirtualFile root = appDir.getParent();
        if (root != null) {
          ContainerUtil.addIfNotNull(res, manager.findDirectory(root));
        }

        return res;
      }
    };

    return set.getAllReferences();
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    return createReferences(element);
  }
}
