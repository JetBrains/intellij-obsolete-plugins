// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.GspFileViewProvider;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.Collection;

public class GrailsLayoutFileReferenceSet extends FileReferenceSet {
  private final VirtualFile myLayoutDir;

  public GrailsLayoutFileReferenceSet(@NotNull String trimedUrl, @NotNull PsiElement element, int offset, @NotNull VirtualFile layoutDir) {
    super(trimedUrl, element, offset, null, true, false);
    myLayoutDir = layoutDir;
  }

  @Override
  protected Condition<PsiFileSystemItem> getReferenceCompletionFilter() {
    return new Condition<>() {

      private final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(getElement().getProject()).getFileIndex();

      @Override
      public boolean value(PsiFileSystemItem psiFileSystemItem) {
        VirtualFile file = psiFileSystemItem.getVirtualFile();
        if (file == null || !fileIndex.isInContent(file)) {
          return false;
        }

        if (!(psiFileSystemItem instanceof PsiFile) || ((PsiFile)psiFileSystemItem).getViewProvider() instanceof GspFileViewProvider) {
          return true;
        }

        return false;
      }
    };
  }

  @Override
  public FileReference createFileReference(TextRange range, int index, String text) {
    return new LayoutFileReference(this, range, index, text);
  }

  @Override
  public @NotNull Collection<PsiFileSystemItem> computeDefaultContexts() {
    PsiDirectory directory = getElement().getManager().findDirectory(myLayoutDir);
    return ContainerUtil.createMaybeSingletonList(directory);
  }

  private static class LayoutFileReference extends FileReference {

    LayoutFileReference(final @NotNull FileReferenceSet fileReferenceSet, TextRange range, int index, String text) {
      super(fileReferenceSet, range, index, text);
    }

    @Override
    public void innerResolveInContext(final @NotNull String text,
                                      final @NotNull PsiFileSystemItem context,
                                      final @NotNull Collection<? super ResolveResult> result,
                                      final boolean caseSensitive) {
      super.innerResolveInContext((isLast() && !text.endsWith(".gsp")) ? text + ".gsp" : text, context, result, caseSensitive);
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element, boolean absolute) throws IncorrectOperationException {
      final PsiFileSystemItem fileSystemItem = (PsiFileSystemItem)element;
      VirtualFile dstVFile = fileSystemItem.getVirtualFile();
      if (dstVFile == null) throw new IncorrectOperationException("Cannot bind to non-physical element:" + element);

      VirtualFile layoutDir = ((GrailsLayoutFileReferenceSet)getFileReferenceSet()).myLayoutDir;

      String path = VfsUtilCore.getRelativePath(dstVFile, layoutDir, '/');

      if (path == null) return getElement();

      path = StringUtil.trimEnd(path, ".gsp");

      return rename(path);
    }

    @Override
    protected Object createLookupItem(PsiElement candidate) {
      if (candidate instanceof GspFile file) {

        String templateName = StringUtil.trimEnd(file.getName(), ".gsp");

        return LookupElementBuilder.create(templateName).withIcon(file.getIcon(0));
      }

      return super.createLookupItem(candidate);
    }
  }

  public static PsiReference[] createReferences(@NotNull PsiElement element) {
    VirtualFile viewsDirectory = GrailsUtils.findViewsDirectory(element);
    if (viewsDirectory == null) return PsiReference.EMPTY_ARRAY;

    VirtualFile layoutDir = viewsDirectory.findChild("layouts");
    if (layoutDir == null) return PsiReference.EMPTY_ARRAY;

    final TextRange range = ElementManipulators.getValueTextRange(element);
    int offset = range.getStartOffset();
    String text = range.substring(element.getText());
    String trimedUrl = PathReference.trimPath(text);

    final FileReferenceSet set = new GrailsLayoutFileReferenceSet(trimedUrl, element, offset, layoutDir);

    return set.getAllReferences();
  }

}
