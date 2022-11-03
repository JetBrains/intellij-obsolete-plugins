package com.intellij.dmserver.test;

import com.intellij.dmserver.integration.DMServerIntegrationData;
import com.intellij.dmserver.integration.DMServerIntegrationDataNoRepository;
import org.jdom.Element;

public class DMSerializationCompatibilityTest extends DMTestBase {

  public void testServerConfigCompatibility() {
    final String INSTALLATION_HOME = "/mock/dm-server-home";
    final boolean SHELL_ENABLED = false;
    final int SHELL_PORT = 777;
    final int DEPLOYMENY_TIMEOUT_SECS = 500;
    final String PICKUP_FOLDER = "mock/pickup";
    final String DUMPS_FOLDER = "mock/dumps";
    final boolean WRAP_SYSTEM_OUT = false;
    final boolean WRAP_SYSTEM_ERR = false;

    Element element = new Element("root");

    DMServerIntegrationDataNoRepository oldIntegrationData = new DMServerIntegrationDataNoRepository();

    oldIntegrationData.setInstallationHome(INSTALLATION_HOME);
    oldIntegrationData.setShellEnabled(SHELL_ENABLED);
    oldIntegrationData.setShellPort(SHELL_PORT);
    oldIntegrationData.setDeploymentTimeoutSecs(DEPLOYMENY_TIMEOUT_SECS);
    oldIntegrationData.setPickupFolder(PICKUP_FOLDER);
    oldIntegrationData.setDumpsFolder(DUMPS_FOLDER);
    oldIntegrationData.setWrapSystemOut(WRAP_SYSTEM_OUT);
    oldIntegrationData.setWrapSystemErr(WRAP_SYSTEM_ERR);

    oldIntegrationData.writeExternal(element);

    DMServerIntegrationData newIntegrationData = new DMServerIntegrationData("");
    newIntegrationData.readExternal(element);

    assertTrue(newIntegrationData.isReloadRequired());

    assertEquals(INSTALLATION_HOME, newIntegrationData.getInstallationHome());
    assertEquals(SHELL_ENABLED, newIntegrationData.isShellEnabled());
    assertEquals(SHELL_PORT, newIntegrationData.getShellPort());
    assertEquals(DEPLOYMENY_TIMEOUT_SECS, newIntegrationData.getDeploymentTimeoutSecs());
    assertEquals(PICKUP_FOLDER, newIntegrationData.getPickupFolder());
    assertEquals(DUMPS_FOLDER, newIntegrationData.getDumpsFolder());
    assertEquals(WRAP_SYSTEM_OUT, newIntegrationData.isWrapSystemOut());
    assertEquals(WRAP_SYSTEM_ERR, newIntegrationData.isWrapSystemErr());
  }
}
