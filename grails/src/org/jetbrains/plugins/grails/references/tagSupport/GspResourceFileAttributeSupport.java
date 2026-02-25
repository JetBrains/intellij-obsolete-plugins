// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.common.GroovyGspTagWrapper;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;
import org.jetbrains.plugins.grails.references.common.ResourceDirAttributeFileReferenceSet;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentReferenceProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GspResourceFileAttributeSupport extends TagAttributeReferenceProvider implements GroovyNamedArgumentReferenceProvider {

  public GspResourceFileAttributeSupport() {
    super("file", "g", GspResourceDirAttributeSupport.TAGS);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    return createReferences(element, gspTagWrapper);
  }

  public static PsiReference[] createReferences(@NotNull PsiElement psiElement, final @NotNull GspTagWrapper gspTagWrapper) {
    final TextRange range = ElementManipulators.getValueTextRange(psiElement);
    int offset = range.getStartOffset();
    String text = range.substring(psiElement.getText());

    String trimedUrl = PathReference.trimPath(text);

    if (trimedUrl.trim().isEmpty()) return PsiReference.EMPTY_ARRAY;

    FileReferenceSet set = new ResourceDirAttributeFileReferenceSet(trimedUrl, psiElement, offset, null, true, true) {

      @Override
      protected PsiElement getPluginElement() {
        return gspTagWrapper.getAttributeValue("plugin");
      }

      @Override
      protected PsiElement getContextPathElement() {
        return gspTagWrapper.getAttributeValue("contextPath");
      }

      @Override
      public @NotNull Collection<PsiFileSystemItem> computeDefaultContexts() {
        FileReference dirFileReference = extractReference(gspTagWrapper.getAttributeValue("dir"));

        if (dirFileReference != null) {
          if (dirFileReference instanceof PluginDirReference) return Collections.emptyList();

          List<PsiFileSystemItem> res = new ArrayList<>();

          for (ResolveResult resolveResult : dirFileReference.multiResolve(false)) {
            PsiElement item = resolveResult.getElement();
            if (item instanceof PsiFileSystemItem) {
              res.add((PsiFileSystemItem)item);
            }
          }

          return res;
        }

        return super.computeDefaultContexts();
      }

      @Override
      protected boolean isAcceptToCompletion(@NotNull VirtualFile fileOrDir) {
        return true;
      }
    };

    return set.getAllReferences();
  }

  @Override
  public PsiReference[] createRef(@NotNull PsiElement element,
                                  @NotNull GrNamedArgument namedArgument,
                                  @NotNull GroovyResolveResult resolveResult,
                                  @NotNull ProcessingContext context) {
    return createReferences(element, new GroovyGspTagWrapper((GrNamedArgumentsOwner)namedArgument.getParent(), null));
  }
}
