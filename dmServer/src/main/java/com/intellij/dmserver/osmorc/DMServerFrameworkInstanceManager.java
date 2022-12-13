package com.intellij.dmserver.osmorc;

import com.intellij.dmserver.install.impl.DMServerInstallationImpl;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.osmorc.frameworkintegration.FrameworkInstanceDefinition;
import org.osmorc.frameworkintegration.FrameworkInstanceManager;
import org.osmorc.run.ui.SelectedBundle;

import java.util.Collection;
import java.util.Collections;

public final class DMServerFrameworkInstanceManager implements FrameworkInstanceManager {
  @NotNull
  @Override
  public Collection<SelectedBundle> getFrameworkBundles(@NotNull FrameworkInstanceDefinition instance, @NotNull FrameworkBundleType type) {
    return Collections.emptyList();
  }

  @Override
  public String checkValidity(@NotNull FrameworkInstanceDefinition frameworkInstanceDefinition) {
    String instanceName = frameworkInstanceDefinition.getName();
    if (instanceName == null || instanceName.trim().length() == 0) {
      return DmServerBundle.message("DMServerFrameworkInstanceManager.error.instance.name.required");
    }

    VirtualFile serverRootFolder = LocalFileSystem.getInstance().findFileByPath(frameworkInstanceDefinition.getBaseFolder());
    if (serverRootFolder == null || !serverRootFolder.isDirectory()) {
      return DmServerBundle.message("DMServerFrameworkInstanceManager.error.folder.not.exists.or.not.folder",
                                    frameworkInstanceDefinition.getBaseFolder());
    }

    ValidationResult installationValid = new DMServerInstallationImpl(serverRootFolder).validate();
    if (!installationValid.isOk()) {
      return installationValid.getErrorMessage();
    }
    return null;
  }
}
