/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * @author Irina.Chernushina on 11/11/2014.
 */
public class JscsCreateConfigAndExcludeFix implements HighPriorityAction, IntentionAction {
  @NotNull
  private final String myFileName;
  // relative path to the project root
  @NotNull
  private final String myRelativePath;

  public JscsCreateConfigAndExcludeFix(@NotNull String name, @NotNull String path) {
    myFileName = name;
    myRelativePath = path;
  }

  @NotNull
  @Override
  public String getText() {
    return "Create " + JscsConfigHelper.CONFIG_JSCS_JSON + " and exclude " + myFileName + " from JSCS analysis there.";
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return "Create " + JscsConfigHelper.CONFIG_JSCS_JSON + " and exclude file(s) from JSCS analysis there.";
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final VirtualFile config = JscsConfigHelper.createConfigBasedOnPreset(project);
    if (config != null) {
      new JscsExcludeFileInConfigFix(config, myFileName, myRelativePath).invoke(project, editor, file);
    }
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
