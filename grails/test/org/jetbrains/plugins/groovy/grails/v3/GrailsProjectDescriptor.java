// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.v3;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.ex.temp.TempFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.LibraryLightProjectDescriptor;

import java.io.IOException;

import static org.jetbrains.plugins.groovy.GroovyProjectDescriptors.LIB_GROOVY_LATEST;

public class GrailsProjectDescriptor extends LibraryLightProjectDescriptor {
  private final String mySourceRootPath;

  public GrailsProjectDescriptor(String sourceRootPath) {
    super(LIB_GROOVY_LATEST);
    mySourceRootPath = sourceRootPath;
  }

  @Override
  public VirtualFile createDirForSources(@NotNull Module module) {
    return createSourceRoot(module, mySourceRootPath);
  }

  @Override
  protected VirtualFile doCreateSourceRoot(VirtualFile root, String srcPath) {
    try {
      TempFileSystem tempFs = (TempFileSystem)root.getFileSystem();
      for (String each : StringUtil.split(srcPath, "/")) {
        VirtualFile child = root.findChild(each);
        if (child != null && tempFs.exists(child)) child.delete(this);
        root = root.createChildDirectory(this, each);
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    return root;
  }
}
