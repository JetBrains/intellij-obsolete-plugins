package com.intellij.dmserver.install;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osmorc.frameworkintegration.FrameworkInstanceDefinition;

import java.util.List;

public abstract class DMServerInstallationManager {
  public static DMServerInstallationManager getInstance() {
    return ApplicationManager.getApplication().getService(DMServerInstallationManager.class);
  }

  @NotNull
  public abstract DMServerInstallation findInstallation(VirtualFile home);

  @Nullable
  public abstract DMServerInstallation findInstallation(String homePath);

  @Nullable
  public abstract DMServerInstallation findInstallation(FrameworkInstanceDefinition framework);

  @NotNull
  public abstract List<? extends DMServerInstallation> getValidInstallations();

  @Nullable
  public abstract FrameworkInstanceDefinition findFramework(DMServerInstallation installation);

  @Nullable
  public abstract FrameworkInstanceDefinition findFramework(DMServerInstallation installation, boolean create);
}
