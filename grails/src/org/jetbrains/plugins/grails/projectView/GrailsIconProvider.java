// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.projectView;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.artefact.api.GrailsArtefactHandler;
import org.jetbrains.plugins.grails.artefact.api.IconOwner;
import org.jetbrains.plugins.grails.artefact.impl.UtilKt;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;

import javax.swing.Icon;

final class GrailsIconProvider extends IconProvider {
  @Override
  public @Nullable Icon getIcon(@NotNull PsiElement element, @Iconable.IconFlags int flags) {
    if (element instanceof GrClassDefinition) {
      GrailsArtefactHandler handler = UtilKt.getArtefactHandler((PsiClass)element);
      if (handler instanceof IconOwner) {
        return IconManager.getInstance().createLayeredIcon(element, ((IconOwner)handler).getIcon(), flags);
      }
    }
    return null;
  }
}
