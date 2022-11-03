package com.intellij.dmserver.artifacts;

import com.intellij.dmserver.util.ModuleUtils;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class ConfigFileManager {

  private static final Logger LOG = Logger.getInstance(ConfigFileManager.class);

  private final Module myModule;

  public ConfigFileManager(Module module) {
    myModule = module;
  }

  private String getFileName() {
    return myModule.getName();
  }

  private String getFileNameWithExt() {
    return getFileName() + DMConfigArtifactType.DOT_PROPERTIES_EXTENSION;
  }

  private VirtualFile getConfigFileDirectory(@NotNull ModuleRootModel rootModel) {
    return ModuleUtils.getModuleRoot(rootModel);
  }

  public VirtualFile findConfigFile() {
    VirtualFile configFileDirectory = getConfigFileDirectory(ModuleRootManager.getInstance(myModule));
    if (configFileDirectory == null) {
      return null; // indicates the module is being created, need a better solution
    }
    return configFileDirectory.findChild(getFileNameWithExt());
  }

  @Nullable
  public VirtualFile createConfigFile(@NotNull final ModuleRootModel rootModel) {
    try {
      return WriteAction.compute(()-> doCreateConfigFile(rootModel));
    }
    catch (IOException e) {
      LOG.error(e);
    }

    return null;
  }

  private VirtualFile doCreateConfigFile(@NotNull ModuleRootModel rootModel) throws IOException {
    VirtualFile configFileDirectory = getConfigFileDirectory(rootModel);
    VirtualFile result = configFileDirectory.findChild(getFileNameWithExt());
    if (result == null) {
      result = configFileDirectory.createChildData(this, getFileNameWithExt());
    }
    return result;
  }
}
