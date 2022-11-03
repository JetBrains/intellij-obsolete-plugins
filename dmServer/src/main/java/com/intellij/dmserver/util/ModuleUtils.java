package com.intellij.dmserver.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public final class ModuleUtils {

  public static VirtualFile getModuleRoot(@NotNull ModuleRootModel rootModel) {
    VirtualFile[] contentRoots = rootModel.getContentRoots();
    if (contentRoots.length == 0) {
      return null;
    }
    return contentRoots[0];
  }


  public static VirtualFile getModuleRoot(@NotNull Module module) {
    return getModuleRoot(ModuleRootManager.getInstance(module));
  }
}
