/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.diagram;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.dom.StrutsConfig;
import com.intellij.util.xml.ui.PerspectiveFileEditor;
import com.intellij.util.xml.ui.PerspectiveFileEditorProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Adds "Web Flow" editor tab to struts-config.xml files.
 *
 * @author Dmitry Avdeev
 */
public class DiagramEditorProvider extends PerspectiveFileEditorProvider {

  @Override
  public boolean accept(@NotNull final Project project, @NotNull final VirtualFile file) {
    if (project.isDisposed() || !file.isValid()) return false;
    final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    return psiFile instanceof XmlFile &&
           psiFile.isValid() &&
           StrutsManager.getInstance().isStrutsConfig((XmlFile)psiFile);
  }

  @Override
  public boolean isDumbAware() {
    return false;
  }

  @Override
  @NotNull
  public PerspectiveFileEditor createEditor(@NotNull final Project project, @NotNull final VirtualFile file) {
    final PsiFile configFile = PsiManager.getInstance(project).findFile(file);
    //noinspection ConstantConditions
    final StrutsConfig strutsConfig = StrutsManager.getInstance().getStrutsConfig(configFile).createStableCopy();

    return new StrutsGraphEditor(project, file, strutsConfig);
  }
}