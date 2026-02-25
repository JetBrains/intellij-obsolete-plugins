// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ElementManipulator;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.reference.impl.CachingReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class PluginSupportFileReferenceSet extends FileReferenceSet {

  public PluginSupportFileReferenceSet(@NotNull String str,
                                       @NotNull PsiElement element,
                                       int startInElement,
                                       PsiReferenceProvider provider,
                                       boolean isCaseSensitive,
                                       boolean endingSlashNotAllowed,
                                       boolean init) {
    super(str, element, startInElement, provider, isCaseSensitive, endingSlashNotAllowed, null, init);
  }

  public MyFileReference createNonPluginFileReference(TextRange range, int index, String text) {
    return new MyFileReference(this, range, index, text);
  }

  @Override
  protected Condition<PsiFileSystemItem> getReferenceCompletionFilter() {
    return new Condition<>() {

      private final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(getElement().getProject()).getFileIndex();

      @Override
      public boolean value(PsiFileSystemItem psiFileSystemItem) {
        VirtualFile fileOrDir = psiFileSystemItem.getVirtualFile();
        if (fileOrDir != null && fileIndex.isInContent(fileOrDir)) {
          return isAcceptToCompletion(fileOrDir);
        }

        return false;
      }
    };
  }

  protected boolean isAcceptToCompletion(@NotNull VirtualFile fileOrDir) {
    return fileOrDir.isDirectory();
  }

  @Override
  public final FileReference createFileReference(final TextRange range, final int index, final String text) {
    if (index == 0 && text.equals("plugins") && isAbsolutePathReference()) {
      return new PluginDirReference(this, range, index, text);
    }

    return createNonPluginFileReference(range, index, text);
  }

  protected boolean isPluginMustHasVersion() {
    return false;
  }

  protected @Nullable VirtualFile getContextInPlugin(@NotNull VirtualFile pluginRoot) {
    return pluginRoot;
  }

  protected PsiElement doSetTextToElement(String text) {
    ElementManipulator<PsiElement> manipulator = CachingReference.getManipulator(getElement());
    return manipulator.handleContentChange(getElement(), text);
  }

  public static class MyFileReference extends FileReference {

    public MyFileReference(final @NotNull FileReferenceSet fileReferenceSet, TextRange range, int index, String text) {
      super(fileReferenceSet, range, index, text);
    }

    private @Nullable FileReference getPreviousReferenceInner() {
      return getIndex() == 0 ? null : getFileReferenceSet().getReference(getIndex() - 1);
    }

    public @Nullable FileReference getPreviousReference() {
      return getPreviousReferenceInner();
    }

    @Override
    public @NotNull PluginSupportFileReferenceSet getFileReferenceSet() {
      return (PluginSupportFileReferenceSet)super.getFileReferenceSet();
    }

    @Override
    protected final ResolveResult @NotNull [] innerResolve(boolean caseSensitive, @NotNull PsiFile containingFile) {
      FileReference contextReference = getPreviousReference();

      if (contextReference instanceof PluginDirReference) {
        Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
        if (module == null) return ResolveResult.EMPTY_ARRAY;

        String text = getText();

        VirtualFile pluginDir = null;

        if (getFileReferenceSet().isPluginMustHasVersion()) {
          for (VirtualFile root : GrailsFramework.getInstance().getAllPluginRoots(module, false)) {
            if (root.getName().equals(text)) {
              pluginDir = root;
              break;
            }
          }
        }
        else {
          pluginDir = GrailsFramework.getInstance().findPluginRoot(module, text, true);
        }

        if (pluginDir == null) return ResolveResult.EMPTY_ARRAY;
        VirtualFile contextInPlugin = getFileReferenceSet().getContextInPlugin(pluginDir);

        if (contextInPlugin == null) return ResolveResult.EMPTY_ARRAY;

        PsiDirectory psiPluginDir = getElement().getManager().findDirectory(contextInPlugin);
        if (psiPluginDir == null) return ResolveResult.EMPTY_ARRAY;

        return new ResolveResult[]{new PsiElementResolveResult(psiPluginDir)};
      }

      return super.innerResolve(caseSensitive, containingFile);
    }

    @Override
    protected final @NotNull Collection<PsiFileSystemItem> getContexts() {
      final FileReference prevRef = getPreviousReference();

      if (prevRef instanceof PluginDirReference) {
        return Collections.emptyList();
      }

      // Copied from super.getContexts()
      if (prevRef == null) {
        return getFileReferenceSet().getDefaultContexts();
      }
      ResolveResult[] resolveResults = prevRef.multiResolve(false);
      ArrayList<PsiFileSystemItem> result = new ArrayList<>(resolveResults.length);
      for (ResolveResult resolveResult : resolveResults) {
        if (resolveResult.getElement() != null) {
          result.add((PsiFileSystemItem)resolveResult.getElement());
        }
      }
      return result;
    }

    protected PsiElement doRename(VirtualFile dstVFile) {
      for (PsiFileSystemItem item : getContexts()) {
        VirtualFile vFile = item.getVirtualFile();
        if (vFile != null && VfsUtilCore.isAncestor(vFile, dstVFile, false)) {
          String text = getElement().getText().substring(getFileReferenceSet().getStartInElement(), getRangeInElement().getStartOffset());
          String path = VfsUtilCore.getRelativePath(dstVFile, vFile, '/');
          if (!text.isEmpty()) {
            if (text.endsWith("/")) {
              path = text + path;
            }
            else {
              path = text + '/' + path;
            }
          }

          assert path != null;
          String newContent = pathToString(path);

          if (newContent == null) return getElement();

          return getFileReferenceSet().doSetTextToElement(newContent);
        }
      }

      FileReference prevRef = getPreviousReferenceInner();
      if (prevRef instanceof MyFileReference) {
        return ((MyFileReference)prevRef).doRename(dstVFile);
      }

      return getElement();
    }

    protected @Nullable String pathToString(@NotNull String path) {
      return path;
    }

    @Override
    public PsiElement bindToElement(@NotNull PsiElement element, boolean absolute) throws IncorrectOperationException {
      final PsiFileSystemItem fileSystemItem = (PsiFileSystemItem)element;
      VirtualFile dstVFile = fileSystemItem.getVirtualFile();
      if (dstVFile == null) throw new IncorrectOperationException("Cannot bind to non-physical element:" + element);
      return doRename(dstVFile);
    }

    @Override
    public final Object @NotNull [] getVariants() {
      if (getPreviousReference() instanceof PluginDirReference) {
        Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
        if (module == null) return ArrayUtilRt.EMPTY_OBJECT_ARRAY;

        return GrailsUtils.createPluginVariants(module, getFileReferenceSet().isPluginMustHasVersion());
      }

      return super.getVariants();
    }
  }

  public static class PluginDirReference extends FileReference {
    public PluginDirReference(final @NotNull FileReferenceSet fileReferenceSet, TextRange range, int index, String text) {
      super(fileReferenceSet, range, index, text);
      assert index == 0;
      assert text.equals("plugins");
      assert fileReferenceSet.isAbsolutePathReference();
    }

    @Override
    protected ResolveResult @NotNull [] innerResolve(boolean caseSensitive, @NotNull PsiFile containingFile) {
      return new ResolveResult[]{new PsiElementResolveResult(getElement().getContainingFile().getOriginalFile())};
    }
  }


}
