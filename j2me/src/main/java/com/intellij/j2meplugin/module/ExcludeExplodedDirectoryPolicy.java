/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.module;

import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.impl.DirectoryIndexExcludePolicy;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public class ExcludeExplodedDirectoryPolicy implements DirectoryIndexExcludePolicy {
  @NotNull
  @Override
  public String[] getExcludeUrlsForProject() {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @NotNull
  @Override
  public VirtualFilePointer[] getExcludeRootsForModule(@NotNull ModuleRootModel rootModel) {
    final J2MEModuleExtension extension = rootModel.getModuleExtension(J2MEModuleExtension.class);
    if (extension != null && extension.isExcludeExplodedDirectory()) {
      final VirtualFilePointer pointer = extension.getExplodedDirectoryPointer();
      if (pointer != null) {
        return new VirtualFilePointer[]{pointer};
      }
    }
    return VirtualFilePointer.EMPTY_ARRAY;
  }
}
