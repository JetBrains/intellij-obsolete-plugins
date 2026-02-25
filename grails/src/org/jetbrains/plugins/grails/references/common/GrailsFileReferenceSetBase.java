// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiFileSystemItemUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public abstract class GrailsFileReferenceSetBase extends FileReferenceSet {


  public GrailsFileReferenceSetBase(@NotNull String str,
                                    @NotNull PsiElement element,
                                    int startInElement,
                                    PsiReferenceProvider provider,
                                    final boolean isCaseSensitive, boolean endingSlashNotAllowed) {
    super(str, element, startInElement, provider, isCaseSensitive, endingSlashNotAllowed);
  }

  protected abstract @Nullable VirtualFile getDefaultContext(boolean isAbsolute);

  private @Nullable PsiDirectory getDefaultContextPsiFile(boolean isAbsolute) {
    VirtualFile context = getDefaultContext(isAbsolute);
    if (context == null) return null;

    return getElement().getManager().findDirectory(context);
  }

  @Override
  public @NotNull Collection<PsiFileSystemItem> computeDefaultContexts() {
    PsiDirectory res = getDefaultContextPsiFile(isAbsolutePathReference());
    return ContainerUtil.createMaybeSingletonList(res);
  }

  @Override
  public RenamableFileReference createFileReference(TextRange range, int index, String text) {
    return new RenamableFileReference(this, range, index, text);
  }

  protected String makePathAbsolute(String path) {
    return '/' + path;
  }

  protected static class RenamableFileReference extends FileReference {

    public RenamableFileReference(final @NotNull GrailsFileReferenceSetBase fileReferenceSet, TextRange range, int index, String text) {
      super(fileReferenceSet, range, index, text);
    }

    public RenamableFileReference(final FileReference original) {
      super(original);
    }

    @Override
    public @NotNull GrailsFileReferenceSetBase getFileReferenceSet() {
      return (GrailsFileReferenceSetBase)super.getFileReferenceSet();
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element, boolean absolute) throws IncorrectOperationException {
      if (!(element instanceof PsiFileSystemItem)) throw new IncorrectOperationException("Cannot bind to element, should be instanceof PsiFileSystemItem: " + element);

      if (!absolute) {
        PsiDirectory context = getFileReferenceSet().getDefaultContextPsiFile(false);
        if (context != null) {
          String path = PsiFileSystemItemUtil.getRelativePathFromAncestor((PsiFileSystemItem)element, context);
          if (path != null) {
            return rename(path);
          }
        }
      }

      PsiDirectory context = getFileReferenceSet().getDefaultContextPsiFile(true);
      if (context != null) {
        String path = PsiFileSystemItemUtil.getRelativePathFromAncestor((PsiFileSystemItem)element, context);
        if (path != null) {
          return rename(getFileReferenceSet().makePathAbsolute(path));
        }
      }

      return getElement();
    }
  }

}
