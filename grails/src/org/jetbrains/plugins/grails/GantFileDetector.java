// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.extensions.GroovyScriptTypeDetector;
import org.jetbrains.plugins.groovy.gant.GantScriptType;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

final class GantFileDetector extends GroovyScriptTypeDetector {
  private GantFileDetector() {
    super(GantScriptType.INSTANCE);
  }

  @Override
  public boolean isSpecificScriptFile(@NotNull GroovyFile script) {
    VirtualFile file = script.getVirtualFile();
    if (file == null) return false;

    VirtualFile parent = file.getParent();
    if (parent == null || !parent.getName().equals("scripts")) return false;

    VirtualFile root = parent.getParent();
    if (root == null) return false;

    if (root.findChild("grails-app") == null) return false;

    return true;
  }
}
