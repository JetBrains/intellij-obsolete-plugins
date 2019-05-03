/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.generate;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.ui.actions.generate.GenerateDomElementAction;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Dmitry Avdeev
 */
public class GenerateMappingAction<T extends DomElement> extends GenerateDomElementAction {

  public GenerateMappingAction(final GenerateMappingProvider<T> provider, final Icon icon) {
    super(provider);
    getTemplatePresentation().setIcon(icon);
  }

  @Override
  protected boolean isValidForFile(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {
    return ((GenerateMappingProvider)myProvider).getParentDomElement(project, editor, file) != null;
  }
}
