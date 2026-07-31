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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;

public class CreateGwtEntryPointAction extends GwtCreateActionBase {
  @Override
  protected boolean requireGwtModule() {
    return true;
  }

  @Override
  protected String getDialogPrompt() {
    return GwtBundle.message("new.entry.point.dlg.prompt");
  }

  @Override
  protected String getDialogTitle() {
    return GwtBundle.message("new.entry.point.dlg.title");
  }

  @Override
  protected PsiElement @NotNull [] doCreate(String name, PsiDirectory directory, final GwtModule gwtModule) throws Exception {
    final PsiClass entryPointClass = createClassFromTemplate(directory, name, JavaFileType.INSTANCE, GwtTemplates.GWT_ENTRY_POINT_JAVA);

    XmlFile xml = gwtModule.getModuleXmlFile();
    if (xml == null) return PsiElement.EMPTY_ARRAY;

    gwtModule.addEntryPoint().getEntryClass().setValue(entryPointClass.getQualifiedName());

    return new PsiElement[]{entryPointClass.getContainingFile()};
  }


  @Override
  protected PsiFile[] getAffectedFiles(final GwtModule gwtModule) {
    return new PsiFile[]{gwtModule.getModuleXmlFile()};
  }

  @Override
  protected @NotNull String getActionName(@NotNull PsiDirectory directory, @NotNull String newName) {
    return GwtBundle.message("new.entry.point.progress.text", newName);
  }
}