package com.intellij.gwt.clientBundle.jam;

import com.intellij.jam.JamConverter;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;

public class SourceFileJamConverter extends JamConverter<PsiFile> {
  private final Condition<PsiFileSystemItem> myCompletionFilter;

  public SourceFileJamConverter(final Condition<PsiFileSystemItem> completionFilter) {
    myCompletionFilter = completionFilter;
  }

  @Override
  public PsiFile fromString(@Nullable String s, JamStringAttributeElement<PsiFile> context) {
    PsiLanguageInjectionHost literal = context.getLanguageInjectionHost();
    if (literal == null) {
      return findFile(context);
    }

    final FileReferenceSet set = getReferenceSet(context, literal);
    if (set != null) {
      final FileReference lastReference = set.getLastReference();
      if (lastReference != null) {
        final PsiFileSystemItem resolved = lastReference.resolve();
        if (resolved instanceof PsiFile) {
          return (PsiFile)resolved;
        }
      }
    }
    return null;
  }

  private static @Nullable PsiFile findFile(JamStringAttributeElement<PsiFile> context) {
    final String path = context.getStringValue();
    final PsiAnnotationMemberValue psiElement = context.getPsiElement();
    if (path == null || psiElement == null) return null;

    final PsiFile file = psiElement.getContainingFile();
    if (file == null) return null;
    final VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return null;
    final VirtualFile target = virtualFile.getParent().findFileByRelativePath(path);
    if (target == null) return null;
    return context.getPsiManager().findFile(target);
  }

  @Override
  public PsiReference @NotNull [] createReferences(@NotNull JamStringAttributeElement<PsiFile> context,
                                                   @NotNull PsiLanguageInjectionHost injectionHost) {
    final FileReferenceSet set = getReferenceSet(context, injectionHost);
    return set != null ? set.getAllReferences() : PsiReference.EMPTY_ARRAY;
  }

  private @Nullable FileReferenceSet getReferenceSet(JamStringAttributeElement<PsiFile> context,
                                                     @NotNull PsiLanguageInjectionHost injectionHost) {
    final String text = context.getStringValue();
    if (text == null) return null;
    return new FileReferenceSet(text, injectionHost, 1, null,
                                injectionHost.getContainingFile().getViewProvider().getVirtualFile().isCaseSensitive(), true) {
      @Override
      protected Condition<PsiFileSystemItem> getReferenceCompletionFilter() {
        return myCompletionFilter;
      }

      @Override
      public @NotNull Collection<PsiFileSystemItem> computeDefaultContexts() {
        // GWT resolves @Source both relative to the ClientBundle's package and as an absolute path from a
        // source/classpath root, so a value like "com/example/client/Style.css" must resolve too (IDEA-61461).
        Collection<PsiFileSystemItem> contexts = new LinkedHashSet<>(super.computeDefaultContexts());
        addSourceRoots(contexts, getElement());
        return contexts;
      }
    };
  }

  private static void addSourceRoots(@NotNull Collection<PsiFileSystemItem> contexts, @Nullable PsiElement element) {
    if (element == null) return;
    PsiFile file = element.getContainingFile();
    VirtualFile virtualFile = file != null ? file.getVirtualFile() : null;
    if (virtualFile == null) return;
    Module module = ModuleUtilCore.findModuleForFile(virtualFile, file.getProject());
    if (module == null) return;
    PsiManager psiManager = file.getManager();
    for (VirtualFile root : ModuleRootManager.getInstance(module).getSourceRoots()) {
      PsiDirectory directory = psiManager.findDirectory(root);
      if (directory != null) {
        contexts.add(directory);
      }
    }
  }
}
