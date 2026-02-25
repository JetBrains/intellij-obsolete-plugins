// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiNameHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.structure.GrailsApplication;

import java.util.List;

public class NewGrailsScriptAction extends NewGrailsXXXAction {

  public NewGrailsScriptAction() {
    super("action.Grails.NewScript.text");
  }

  @Override
  protected @NotNull String getCommand(@NotNull GrailsApplication application) {
    return "create-script";
  }

  @Override
  protected @Nullable VirtualFile getTargetDirectory(@NotNull GrailsApplication application) {
    return application.getAppRoot().findChild("scripts");
  }

  @Override
  protected void fillGeneratedNamesList(@NotNull String name, @NotNull List<String> names) {
    names.add("scripts/" + canonicalize(name) + ".groovy");
    names.add("test/cli/" + canonicalize(name) + "Tests.groovy");
  }

  @Override
  protected @NlsContexts.DialogMessage String isValidIdentifier(String inputString, Project project) {
    if (PsiNameHelper.getInstance(project).isIdentifier(inputString)) {
      return null;
    }
    return GrailsBundle.message("dialog.message.valid.script.name.check");
  }
}
