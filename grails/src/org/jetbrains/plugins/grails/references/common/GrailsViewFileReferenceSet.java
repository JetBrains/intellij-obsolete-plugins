// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentReferenceProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.Collection;

public final class GrailsViewFileReferenceSet extends GrailsFileReferenceSetBase {
  private final Function<? super VirtualFile, ? extends VirtualFile> myContextFinder;

  public GrailsViewFileReferenceSet(@Nullable Function<? super VirtualFile, ? extends VirtualFile> contextFinder,
                                    @NotNull String str,
                                    PsiElement element,
                                    int startInElement,
                                    PsiReferenceProvider provider,
                                    final boolean isCaseSensitive, boolean endingSlashNotAllowed) {
    super(str, element, startInElement, provider, isCaseSensitive, endingSlashNotAllowed);
    myContextFinder = contextFinder;
  }

  @Override
  protected Condition<PsiFileSystemItem> getReferenceCompletionFilter() {
    return new Condition<>() {

      private final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(getElement().getProject()).getFileIndex();

      @Override
      public boolean value(PsiFileSystemItem psiFileSystemItem) {
        if (psiFileSystemItem instanceof PsiDirectory) {
          VirtualFile file = psiFileSystemItem.getVirtualFile();
          return fileIndex.isInContent(file);
        }

        String name = psiFileSystemItem.getName();

        return name.endsWith(".gsp") || name.endsWith(".jsp"); // Don't use instanceof JspFile, because dependency to javaee is soft!!!
      }
    };
  }

  @Override
  protected @Nullable VirtualFile getDefaultContext(boolean isAbsolute) {
    VirtualFile viewDir = GrailsUtils.findViewsDirectory(getElement());

    if (isAbsolute) {
      return viewDir;
    }

    if (viewDir == null) return null;
    if (myContextFinder == null) return null;

    return myContextFinder.fun(viewDir);
  }

  @Override
  public RenamableFileReference createFileReference(TextRange range, int index, String text) {
    return new RenamableFileReference(this, range, index, text) {
      @Override
      protected void innerResolveInContext(@NotNull String text,
                                           @NotNull PsiFileSystemItem context,
                                           @NotNull Collection<? super ResolveResult> result,
                                           boolean caseSensitive) {
        int size = result.size();
        super.innerResolveInContext(isLast() ? text + ".gsp" : text, context, result, caseSensitive);
        if (result.size() == size) {
          super.innerResolveInContext(isLast() ? text + ".jsp" : text, context, result, caseSensitive);
        }
      }

      @Override
      protected Object createLookupItem(PsiElement candidate) {
        if (candidate instanceof PsiFile file) {

          String name = file.getName();
          int idx = name.lastIndexOf('.');
          if (idx != -1) {
            name = name.substring(0, idx);
          }

          return LookupElementBuilder.create(name).withIcon(file.getIcon(0));
        }

        return super.createLookupItem(candidate);
      }

      @Override
      protected PsiElement rename(String newName) throws IncorrectOperationException {
        if (newName.endsWith(".gsp") || newName.endsWith(".jsp")) {
          newName = newName.substring(0, newName.length() - 4);
        }
        return super.rename(newName);
      }
    };
  }

  public static PsiReference[] createReferences(@NotNull PsiElement element,
                                                @Nullable Function<? super VirtualFile, ? extends VirtualFile> contextFinder) {
    final TextRange range = ElementManipulators.getValueTextRange(element);
    int offset = range.getStartOffset();
    String text = range.substring(element.getText());

    String trimedUrl = PathReference.trimPath(text);

    final FileReferenceSet set;
    set = new GrailsViewFileReferenceSet(contextFinder, trimedUrl, element, offset, null, true, true);

    return set.getAllReferences();
  }

  public static final class Provider implements GroovyNamedArgumentReferenceProvider {
    @Override
    public PsiReference[] createRef(@NotNull PsiElement element,
                                    final @NotNull GrNamedArgument namedArgument,
                                    @NotNull GroovyResolveResult method,
                                    @NotNull ProcessingContext context) {
      return createReferences(element, viewDir -> {
        PsiClass aClass = PsiUtil.getContainingNotInnerClass(namedArgument);

        if (GrailsArtifact.CONTROLLER.isInstance(aClass)) {
          //noinspection ConstantConditions
          return viewDir.findChild(GrailsArtifact.CONTROLLER.getArtifactName(aClass));
        }
        else {
          return null;
        }
      });
    }
  }
}
