// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails;

import com.intellij.codeInsight.daemon.ProblemHighlightFilter;
import com.intellij.lang.Language;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.GroovyLanguage;

public final class TemplateHighlightErrorFilter extends ProblemHighlightFilter {

  @Override
  public boolean shouldHighlight(@NotNull PsiFile psiFile) {
    Language language = psiFile.getLanguage();

    if (language == GroovyLanguage.INSTANCE || language == GspLanguage.INSTANCE) {
      VirtualFile file = psiFile.getOriginalFile().getVirtualFile();
      if (file != null) {
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(psiFile.getProject()).getFileIndex();
        if (!fileIndex.isInSource(file)) {
          VirtualFile templateDirectory = GrailsUtils.findParent(file, GrailsUtils.TEMPLATES_DIR);
          if (templateDirectory != null) {
            VirtualFile tempParent = templateDirectory.getParent();
            if (tempParent != null && tempParent.getName().equals("src")) {
              VirtualFile root = tempParent.getParent();
              if (Comparing.equal(root, fileIndex.getContentRootForFile(file))) {
                Module module = fileIndex.getModuleForFile(file);

                GrailsFramework framework = GrailsFramework.getInstance();

                if (module != null && (framework.hasSupport(module) || framework.isAuxModule(module))) {
                  return false;
                }
              }
            }
          }
        }
      }
    }

    return true;
  }
}
