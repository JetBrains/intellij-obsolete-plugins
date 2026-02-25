// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReferenceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsFramework;

import java.util.Collection;
import java.util.Collections;

/**
 * @author user
 */
public class WebAppFolderFileReferenceSet extends PluginSupportFileReferenceSet {


  public WebAppFolderFileReferenceSet(@NotNull String str,
                                      @NotNull PsiElement element,
                                      int startInElement,
                                      PsiReferenceProvider provider,
                                      final boolean isCaseSensitive, boolean endingSlashNotAllowed) {
    super(str, element, startInElement, provider, isCaseSensitive, endingSlashNotAllowed, true);
  }

  @Override
  protected boolean isPluginMustHasVersion() {
    return true;
  }

  @Override
  protected VirtualFile getContextInPlugin(@NotNull VirtualFile pluginRoot) {
    return pluginRoot.findChild("web-app");
  }

  @Override
  public @NotNull Collection<PsiFileSystemItem> computeDefaultContexts() {
    VirtualFile rootDir = GrailsFramework.getInstance().findAppRoot(getElement());
    if (rootDir == null) return Collections.emptySet();

    VirtualFile webApp = rootDir.findChild("web-app");
    if (webApp == null) return Collections.emptySet();

    PsiDirectory psiDirectory = getElement().getManager().findDirectory(webApp);
    if (psiDirectory == null) return Collections.emptySet();

    return Collections.singleton(psiDirectory);
  }
}
