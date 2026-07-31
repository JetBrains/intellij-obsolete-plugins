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

package com.intellij.gwt.actions;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.templates.GwtTemplates;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class CreateGwtSerializableClassAction extends GwtCreateActionBase {
  @Override
  protected boolean requireGwtModule() {
    return true;
  }

  @Override
  protected String getDialogPrompt() {
    return GwtBundle.message("new.serial.dlg.prompt");
  }

  @Override
  protected String getDialogTitle() {
    return GwtBundle.message("new.serial.dlg.title");
  }

  @Override
  protected @NotNull String getActionName(@NotNull PsiDirectory directory, @NotNull String newName) {
    return GwtBundle.message("new.serial.progress.text", newName);
  }

  @Override
  protected PsiElement @NotNull [] doCreate(String name, PsiDirectory directory, final GwtModule gwtModule) throws Exception {
    return new PsiElement[]{
      createClassFromTemplate(directory, name, JavaFileType.INSTANCE, GwtTemplates.GWT_SERIAL_CLASS_JAVA).getContainingFile()
    };
  }
}
