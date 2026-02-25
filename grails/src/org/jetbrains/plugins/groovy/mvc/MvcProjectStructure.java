// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.mvc;

import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class MvcProjectStructure {
  protected final Module myModule;
  private final boolean myAuxModule;
  private final String myUserHomeSdkRoot;
  private final String mySdkWorkDirPath;

  public MvcProjectStructure(Module module, boolean auxModule, String userHomeSdkRoot, final File sdkWorkDir) {
    myAuxModule = auxModule;
    myModule = module;
    myUserHomeSdkRoot = userHomeSdkRoot;
    final VirtualFile sdkWorkDirVFile = MvcModuleStructureUtil.refreshAndFind(sdkWorkDir);
    mySdkWorkDirPath = sdkWorkDirVFile == null ? "" : sdkWorkDirVFile.getPath() + "/";
  }

  public boolean isValidContentRoot(@NotNull VirtualFile file) {
    if (file.getPath().startsWith(myUserHomeSdkRoot)) {
      if (!myAuxModule) {
        return false;
      }
      if (!file.getPath().startsWith(mySdkWorkDirPath)) {
        return false;
      }
    }
    return true;
  }

  public abstract @NotNull String getUserLibraryName();

  public abstract MultiMap<JpsModuleSourceRootType<?>, String> getSourceFolders();

  public abstract String[] getInvalidSourceFolders();

  public abstract String[] getExcludedFolders();

  public List<VirtualFile> getExcludedFolders(@NotNull VirtualFile root) {
    List<VirtualFile> res = new ArrayList<>();

    for (final String excluded : getExcludedFolders()) {
      VirtualFile dir = root.findFileByRelativePath(excluded);
      if (dir != null) {
        res.add(dir);
      }
    }

    return res;
  }

  public void setupFacets(Collection<Consumer<ModifiableFacetModel>> actions, Collection<VirtualFile> roots) {

  }
}
