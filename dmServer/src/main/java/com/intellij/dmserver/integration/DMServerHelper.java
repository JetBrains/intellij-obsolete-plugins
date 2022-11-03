package com.intellij.dmserver.integration;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.javaee.appServers.appServerIntegrations.*;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;

public class DMServerHelper implements ApplicationServerHelper {
  @Override
  public ApplicationServerInfo getApplicationServerInfo(ApplicationServerPersistentData applicationServerPersistentData)
    throws CantFindApplicationServerJarsException {
    DMServerIntegrationData persistentDataImpl = (DMServerIntegrationData)applicationServerPersistentData;
    DMServerInstallation installation = persistentDataImpl.getInstallation();
    if (installation == null) {
      throw new CantFindApplicationServerJarsException("Can't find dmServer home");
    }
    return createApplicationServerInfo(installation);
  }

  public static ApplicationServerInfo createApplicationServerInfo(DMServerInstallation installation) {
    String defaultName = DmServerBundle.message("DMServerHelper.server.info.default.name", installation.getVersionName());
    return new ApplicationServerInfo(unwrapVirtualFiles(installation.getSharedLibraries()), defaultName);
  }

  @Override
  public ApplicationServerPersistentData createPersistentDataEmptyInstance() {
    return new DMServerIntegrationData("");
  }

  @Override
  public ApplicationServerPersistentDataEditor createConfigurable() {
    return new DMServerIntegrationEditor();
  }

  private static File[] unwrapVirtualFiles(VirtualFile[] vFiles) {
    File[] result = new File[vFiles.length];
    for (int i = 0; i < vFiles.length; i++) {
      result[i] = VfsUtilCore.virtualToIoFile(vFiles[i]);
    }
    return result;
  }
}
