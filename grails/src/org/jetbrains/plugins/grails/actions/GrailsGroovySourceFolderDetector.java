// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.groovy.actions.GroovySourceFolderDetector;

final class GrailsGroovySourceFolderDetector extends GroovySourceFolderDetector {
  private static final String[] GROOVY_FOLDERS =
    {"grails-app/controllers", "grails-app/domain", "grails-app/services", "grails-app/taglib", "src/groovy"};

  @Override
  public boolean isGroovySourceFolder(PsiDirectory file) {
    Module module = ModuleUtilCore.findModuleForPsiElement(file);
    VirtualFile appRoot = GrailsFramework.getInstance().findAppRoot(module);
    if (appRoot == null) return false;

    assert module != null;
    if (GrailsFramework.getInstance().getSdkRoot(module) == null) return false;

    String path = VfsUtilCore.getRelativePath(file.getVirtualFile(), appRoot, '/');
    if (path == null) return false;

    for (String groovyFolder : GROOVY_FOLDERS) {
      if (path.equals(groovyFolder) || (path.startsWith(groovyFolder) && path.startsWith("/", groovyFolder.length()))) {
        return true;
      }
    }

    return false;
  }
}
