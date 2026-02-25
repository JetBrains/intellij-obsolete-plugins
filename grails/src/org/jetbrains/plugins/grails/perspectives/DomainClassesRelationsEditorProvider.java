// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.perspectives;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.xml.ui.PerspectiveFileEditor;
import com.intellij.util.xml.ui.PerspectiveFileEditorProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;

final class DomainClassesRelationsEditorProvider extends PerspectiveFileEditorProvider {
  @Override
  public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
    if (Registry.is("grails.advanced.mode")) {
      return false;
    }

    //noinspection SSBasedInspection
    return ApplicationManager.getApplication().runReadAction((Computable<Boolean>)() -> {
      PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
      if (!(psiFile instanceof GroovyFile)) {
        return false;
      }

      for (GrTypeDefinition grTypeDefinition : ((GroovyFile)psiFile).getTypeDefinitions()) {
        if (GormUtils.isGormBean(grTypeDefinition)) {
          return true;
        }
      }

      return false;
    });
  }

  @Override
  public boolean isDumbAware() {
    return false;
  }

  @Override
  public @NotNull PerspectiveFileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
    return new DomainClassesRelationsEditor(project, virtualFile);
  }
}
