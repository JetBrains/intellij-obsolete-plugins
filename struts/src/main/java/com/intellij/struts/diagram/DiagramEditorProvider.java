/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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