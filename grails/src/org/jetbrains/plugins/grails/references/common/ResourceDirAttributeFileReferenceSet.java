// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author user
 */
public abstract class ResourceDirAttributeFileReferenceSet extends WebAppFolderFileReferenceSet {

  public ResourceDirAttributeFileReferenceSet(@NotNull String str,
                                              @NotNull PsiElement element,
                                              int startInElement,
                                              PsiReferenceProvider provider,
                                              final boolean isCaseSensitive, boolean endingSlashNotAllowed) {
    super(str, element, startInElement, provider, isCaseSensitive, endingSlashNotAllowed);
  }

  protected abstract @Nullable PsiElement getPluginElement();

  protected abstract @Nullable PsiElement getContextPathElement();

  protected static @Nullable FileReference extractReference(@Nullable PsiElement contextElement) {
    if (contextElement != null) {
      for (PsiReference ref : contextElement.getReferences()) {
        if (ref instanceof FileReference) {
          return ((FileReference)ref).getLastFileReference();
        }
      }
    }

    return null;
  }


  @Override
  public @NotNull Collection<PsiFileSystemItem> computeDefaultContexts() {
    PsiElement pluginElement = getPluginElement();

    if (pluginElement != null) {
      PsiReference pluginRef = pluginElement.getReference();
      if (pluginRef == null) return Collections.emptySet();

      PsiElement psiPluginRoot = pluginRef.resolve();
      if (!(psiPluginRoot instanceof PsiDirectory)) return Collections.emptySet();

      VirtualFile pluginContext = getContextInPlugin(((PsiDirectory)psiPluginRoot).getVirtualFile());
      if (pluginContext == null) return Collections.emptySet();

      PsiDirectory directory = psiPluginRoot.getManager().findDirectory(pluginContext);
      if (directory == null) return Collections.emptySet();

      return Collections.singleton(directory);
    }

    List<PsiFileSystemItem> res = new ArrayList<>();

    FileReference contextPathRef = extractReference(getContextPathElement());
    if (contextPathRef != null) {
      if (contextPathRef instanceof PluginDirReference) return Collections.emptyList();

      for (ResolveResult resolveResult : contextPathRef.multiResolve(false)) {
        PsiElement item = resolveResult.getElement();
        if (item instanceof PsiFileSystemItem) {
          res.add((PsiFileSystemItem)item);
        }
      }

      return res;
    }

    return super.computeDefaultContexts();
  }

}
