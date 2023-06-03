package org.intellij.j2ee.web.resin;

import com.intellij.execution.ExecutionException;
import com.intellij.javaee.appServers.appServerIntegrations.*;
import org.intellij.j2ee.web.resin.resin.ResinInstallation;
import org.intellij.j2ee.web.resin.ui.SelectResinLocationEditor;

import java.io.File;

public class ResinApplicationServerHelper implements ApplicationServerHelper {

  @Override
  public ApplicationServerInfo getApplicationServerInfo(ApplicationServerPersistentData persistentData)
    throws CantFindApplicationServerJarsException {
    try {
      final ResinPersistentData resinPersistentData = (ResinPersistentData)persistentData;

      ResinInstallation resinInstallation = ResinInstallation.create(resinPersistentData.RESIN_HOME);

      File[] resinLib = resinInstallation.getLibFiles(resinPersistentData.INCLUDE_ALL_JARS);
      String version = resinInstallation.getDisplayName();
      return new ApplicationServerInfo(resinLib, version);
    }
    catch (ExecutionException e) {
      throw new CantFindApplicationServerJarsException(e.getMessage());
    }
  }

  @Override
  public ApplicationServerPersistentData createPersistentDataEmptyInstance() {
    return new ResinPersistentData();
  }

  @Override
  public ApplicationServerPersistentDataEditor createConfigurable() {
    return new SelectResinLocationEditor();
  }
}
