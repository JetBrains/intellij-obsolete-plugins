// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrailsRootBasedFileReferenceSet extends GrailsFileReferenceSetBase {

  private static final Pattern ABSOLUTE_PATH_PATTERN = Pattern.compile("(/|[a-zA-Z]:/).*");

  public GrailsRootBasedFileReferenceSet(@NotNull String str,
                                         PsiElement element,
                                         int startInElement,
                                         PsiReferenceProvider provider,
                                         final boolean isCaseSensitive, boolean endingSlashNotAllowed) {
    super(str, element, startInElement, provider, isCaseSensitive, endingSlashNotAllowed);
  }

  @Override
  protected VirtualFile getDefaultContext(boolean isAbsolute) {
    if (isAbsolute) {
      int rootIndex = getPathString().indexOf('/');
      if (rootIndex == -1) {
        return null;
      }
      return LocalFileSystem.getInstance().findFileByPath(getPathString().substring(0, rootIndex + 1));
    }

    PsiFile file = getElement().getContainingFile().getOriginalFile();
    VirtualFile virtualFile = file.getVirtualFile();

    if (virtualFile == null) return null;

    return ProjectRootManager.getInstance(file.getProject()).getFileIndex().getContentRootForFile(virtualFile);
  }

  @Override
  public boolean isAbsolutePathReference() {
    return ABSOLUTE_PATH_PATTERN.matcher(getPathString()).matches();
  }

  @Override
  protected String makePathAbsolute(String path) {
    String pathString = getPathString();
    Matcher matcher = ABSOLUTE_PATH_PATTERN.matcher(pathString);
    if (!matcher.matches()) {
      throw new IllegalStateException("Invalid path: " + pathString);
    }
    return matcher.group(1) + path;
  }

  public static PsiReference[] createReferences(@NotNull PsiElement psiElement) {
    final TextRange range = ElementManipulators.getValueTextRange(psiElement);
    int offset = range.getStartOffset();
    final String text = range.substring(psiElement.getText());

    String trimedUrl = PathReference.trimPath(text);

    if (trimedUrl.trim().isEmpty()) return PsiReference.EMPTY_ARRAY;

    FileReferenceSet set = new GrailsRootBasedFileReferenceSet(text, psiElement, offset, null, true, true);

    return set.getAllReferences();
  }

}
