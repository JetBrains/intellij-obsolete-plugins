// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi;

import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;

public final class GspPsiUtil {

  private GspPsiUtil() {
  }

  public static @Nullable GspFile getGspFile(final PsiElement element) {
    if (element == null) return null;
    final PsiFile containingFile = element.getContainingFile();
    if (containingFile == null) return null;

    final FileViewProvider viewProvider = containingFile.getViewProvider();
    final PsiFile psiFile = viewProvider.getPsi(viewProvider.getBaseLanguage());
    return psiFile instanceof GspFile ? (GspFile) psiFile : null;
  }

  public static boolean isInGspFile(PsiElement element) {
    return getGspFile(element) != null;
  }

}
