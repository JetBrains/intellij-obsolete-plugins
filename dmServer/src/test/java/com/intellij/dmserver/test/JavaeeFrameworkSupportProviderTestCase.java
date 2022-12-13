package com.intellij.dmserver.test;

import com.intellij.ide.util.frameworkSupport.FrameworkSupportProviderTestCase;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServer;
import com.intellij.javaee.appServers.serverInstances.ApplicationServersManager;
import com.intellij.openapi.application.ApplicationManager;

import java.util.List;

public abstract class JavaeeFrameworkSupportProviderTestCase extends FrameworkSupportProviderTestCase {
  public static void deleteApplicationServers() {
    final ApplicationServersManager manager = ApplicationServersManager.getInstance();
    final List<ApplicationServer> servers = manager.getApplicationServers();
    final ApplicationServersManager.ApplicationServersManagerModifiableModel model = manager.createModifiableModel();
    for (ApplicationServer server : servers) {
      model.deleteApplicationServer(server);
    }
    ApplicationManager.getApplication().runWriteAction(() -> model.commit());
  }


  @Override
  protected void tearDown() throws Exception {
    try {
      deleteApplicationServers();
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }
}
