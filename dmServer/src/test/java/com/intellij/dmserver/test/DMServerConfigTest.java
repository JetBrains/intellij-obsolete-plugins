package com.intellij.dmserver.test;

import com.intellij.dmserver.install.impl.DMServerConfigSupport10;
import com.intellij.dmserver.install.impl.DMServerConfigSupport20;
import com.intellij.dmserver.integration.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

public class DMServerConfigTest extends DMTestBase {

  @NonNls private static final String PICKUP_FOLDER = "Test/Pickup/Folder";
  @NonNls private static final String DUMP_FOLDER = "Test/Dump/Folder";
  private static final int DMSHELL_PORT = 1234;
  private static final boolean DMSHELL_ENABLED = false;
  private static final int DEPLOYMENT_TIMEOUT = 5678;
  @NonNls private static final String INSTALLATION_HOME = "Test/Installation/Home";
  private static final boolean WRAP_SYS_OUT = false;
  private static final boolean WRAP_SYS_ERR = false;
  @NonNls private static final String EXTERNAL_ITEM_SEARCH_PATTERN = "repository/ext/{artifact}";
  @NonNls private static final String WATCHED_ITEM_WATCH_DIRECTORY = "repository/usr";
  @NonNls private static final String WATCHED_ITEM_WATCHED_INTERVAL = "123";

  private String path2store(String path) {
    return FileUtil.toSystemDependentName(path).replace("\\", "\\\\").replace("/", "\\/");
  }

  public void testReadConfig10() {
    VirtualFile home = getTempDir().createVirtualDir();

    VirtualFile configDir = createChildDirectory(home, "config");
    VirtualFile serverConfigFile = createChildData(configDir, "server.config");
    VirtualFile deployerConfigFile = createChildData(configDir, "deployer.config");
    setFileText(serverConfigFile, "{\n" +
                                  "\t\"serviceability\": {\n" +
                                  "\t    \"trace\": {\n" +
                                  "\t        \"directory\": \"serviceability/trace\",\n" +
                                  "\t        \"levels\": {\n" +
                                  "\t            \"*\" : \"info\"\n" +
                                  "\t        }\n" +
                                  "\t    },\n" +
                                  "\t    \"logs\": {\n" +
                                  "\t        \"directory\": \"serviceability/logs\"\n" +
                                  "\t    },\n" +
                                  "\t    \"dump\": {\n" +
                                  "\t        \"directory\": \"" + path2store(DUMP_FOLDER) + "\"\n" +
                                  "\t    }\n" +
                                  "\t},\n" +
                                  "\t\"pickupDirectory\" : \"" + path2store(PICKUP_FOLDER) + "\",\n" +
                                  "\t\"workDirectory\": \"work\",\n" +
                                  "\t\"configPaths\": [\n" +
                                  "\t    \"config\"\n" +
                                  "\t],\n" +
                                  "\t\"osgiConsole\": {\n" +
                                  "\t    \"enabled\": " + DMSHELL_ENABLED + ",\n" +
                                  "\t    \"port\": " + DMSHELL_PORT + "\n" +
                                  "\t},\n" +
                                  "\t\"provisioning\":{\n" +
                                  "\t\"searchPaths\":\n[" +
                                  "\t\"repository\\/bundles\\/subsystems\\/{name}\\/{bundle}.jar\",\n" +
                                  "\t\"repository\\/bundles\\/ext\\/{bundle}\",\n" +
                                  "\t\"repository\\/bundles\\/usr\\/{bundle}\",\n" +
                                  "\t\"repository\\/libraries\\/ext\\/{library}\",\n" +
                                  "\t\"repository\\/libraries\\/usr\\/{library}\"]}\n" +
                                  "} ");
    setFileText(deployerConfigFile, "{\n" +
                                    "\t\"deployer\" : {\n" +
                                    "\t\t\"version\" : 1.0,\n" +
                                    "\t\t\"deploymentTimeoutSeconds\" : " +
                                    DEPLOYMENT_TIMEOUT +
                                    " // Note: use 0 to disable deployment timeouts\n" +
                                    "\t}\n" +
                                    "}");

    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    DMServerConfigSupport10 configSupport10 = new DMServerConfigSupport10(home);
    DMServerIntegrationData integrationData = new DMServerIntegrationData(INSTALLATION_HOME);
    configSupport10.readFromServer(integrationData);

    assertEquals(INSTALLATION_HOME, integrationData.getInstallationHome());
    assertEquals(DMSHELL_ENABLED, integrationData.isShellEnabled());
    assertEquals(DMSHELL_PORT, integrationData.getShellPort());
    assertEquals(DEPLOYMENT_TIMEOUT, integrationData.getDeploymentTimeoutSecs());
    assertEquals(PICKUP_FOLDER, integrationData.getPickupFolder());
    assertEquals(DUMP_FOLDER, integrationData.getDumpsFolder());
    assertEquals(true, integrationData.isWrapSystemOut());
    assertEquals(true, integrationData.isWrapSystemErr());
    List<DMServerRepositoryItem> repositoryItems = integrationData.getRepositoryItems();
    assertEquals(5, repositoryItems.size());
    DMServerRepositoryItem10 repositoryItem = assertInstanceOf(repositoryItems.get(0), DMServerRepositoryItem10.class);
    assertEquals("repository/bundles/subsystems/{name}/{bundle}.jar", repositoryItem.getPath());
    repositoryItem = assertInstanceOf(repositoryItems.get(1), DMServerRepositoryItem10.class);
    assertEquals("repository/bundles/ext/{bundle}", repositoryItem.getPath());
    repositoryItem = assertInstanceOf(repositoryItems.get(2), DMServerRepositoryItem10.class);
    assertEquals("repository/bundles/usr/{bundle}", repositoryItem.getPath());
    repositoryItem = assertInstanceOf(repositoryItems.get(3), DMServerRepositoryItem10.class);
    assertEquals("repository/libraries/ext/{library}", repositoryItem.getPath());
    repositoryItem = assertInstanceOf(repositoryItems.get(4), DMServerRepositoryItem10.class);
    assertEquals("repository/libraries/usr/{library}", repositoryItem.getPath());
  }

  public void testWriteConfig10() throws IOException {
    VirtualFile home = getTempDir().createVirtualDir();

    VirtualFile configDir = createChildDirectory(home, "config");
    VirtualFile serverConfigFile = createChildData(configDir, "server.config");
    VirtualFile deployerConfigFile = createChildData(configDir, "deployer.config");
    setFileText(serverConfigFile, """
      {
      \t"serviceability": {
      \t    "trace": {
      \t        "directory": "serviceability/trace",
      \t        "levels": {
      \t            "*" : "info"
      \t        }
      \t    },
      \t    "logs": {
      \t        "directory": "serviceability/logs"
      \t    },
      \t    "dump": {
      \t        "directory": "Dump/Folder/To/Overwrite"
      \t    }
      \t},
      \t"pickupDirectory" : "Pickup/Folder/To/Overwrite",
      \t"workDirectory": "work",
      \t"configPaths": [
      \t    "config"
      \t],
      \t"osgiConsole": {
      \t    "enabled": true,
      \t    "port": 4321
      \t}
      }\s""");
    setFileText(deployerConfigFile, """
      {
      \t"deployer" : {
      \t\t"version" : 1.0,
      \t\t"deploymentTimeoutSeconds" : 8765 // Note: use 0 to disable deployment timeouts
      \t}
      }""");
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    final DMServerIntegrationData integrationData = new DMServerIntegrationData(INSTALLATION_HOME);
    integrationData.setInstallationHome(INSTALLATION_HOME);
    integrationData.setShellEnabled(DMSHELL_ENABLED);
    integrationData.setShellPort(DMSHELL_PORT);
    integrationData.setDeploymentTimeoutSecs(DEPLOYMENT_TIMEOUT);
    integrationData.setPickupFolder(PICKUP_FOLDER);
    integrationData.setDumpsFolder(DUMP_FOLDER);
    List<DMServerRepositoryItem> repositoryItems = integrationData.getRepositoryItems();
    DMServerRepositoryItem repositoryItem = new DMServerRepositoryItem10();
    repositoryItem.setPath("repository/bundles/subsystems/{name}/{bundle}.jar");
    repositoryItems.add(repositoryItem);
    repositoryItem = new DMServerRepositoryItem10();
    repositoryItem.setPath("repository/bundles/ext/{bundle}");
    repositoryItems.add(repositoryItem);
    repositoryItem = new DMServerRepositoryItem10();
    repositoryItem.setPath("repository/bundles/usr/{bundle}");
    repositoryItems.add(repositoryItem);
    repositoryItem = new DMServerRepositoryItem10();
    repositoryItem.setPath("repository/libraries/ext/{library}");
    repositoryItems.add(repositoryItem);
    repositoryItem = new DMServerRepositoryItem10();
    repositoryItem.setPath("repository/libraries/usr/{library}");
    repositoryItems.add(repositoryItem);

    final DMServerConfigSupport10 configSupport10 = new DMServerConfigSupport10(home);
    ApplicationManager.getApplication().runWriteAction(() -> configSupport10.writeToServer(integrationData));


    String serverConfigContent = VfsUtilCore.loadText(serverConfigFile);
    assertEquals(
      "{\"serviceability\":" +
      "{\"trace\":" +
      "{\"directory\":\"serviceability\\/trace\"," +
      "\"levels\":{\"*\":\"info\"}}," +
      "\"logs\":" +
      "{\"directory\":\"serviceability\\/logs\"}," +
      "\"dump\":{\"directory\":\"" +
      path2store(DUMP_FOLDER) +
      "\"}}," +
      "\"pickupDirectory\":\"" +
      path2store(PICKUP_FOLDER) +
      "\"," +
      "\"workDirectory\":\"work\"," +
      "\"configPaths\":[\"config\"]," +
      "\"osgiConsole\":{\"enabled\":" +
      DMSHELL_ENABLED +
      ",\"port\":" +
      DMSHELL_PORT +
      "}," +
      "\"provisioning\":{\"searchPaths\":[\"repository\\/bundles\\/subsystems\\/{name}\\/{bundle}.jar\",\"repository\\/bundles\\/ext\\/{bundle}\",\"repository\\/bundles\\/usr\\/{bundle}\",\"repository\\/libraries\\/ext\\/{library}\",\"repository\\/libraries\\/usr\\/{library}\"]}" +
      "}",
      serverConfigContent);

    String deployerConfigContent = VfsUtilCore.loadText(deployerConfigFile);
    assertEquals("{\"deployer\":{\"version\":1,\"deploymentTimeoutSeconds\":" + DEPLOYMENT_TIMEOUT + "}}",
                 deployerConfigContent);
  }

  public void testReadConfig20() {
    VirtualFile home = getTempDir().createVirtualDir();

    VirtualFile configDir = createChildDirectory(home, "config");
    VirtualFile kernelConfigFile = createChildData(configDir, "com.springsource.kernel.properties");
    VirtualFile medicConfigFile = createChildData(configDir, "com.springsource.osgi.medic.properties");
    VirtualFile repositoryConfigFile = createChildData(configDir, "com.springsource.repository.properties");
    setFileText(kernelConfigFile, "########################\n" +
                                  "# Deployer Configuration\n" +
                                  "########################\n" +
                                  "# Note: use 0 to disable deployment timeouts\n" +
                                  "deployer.timeout=\t\t\t" + DEPLOYMENT_TIMEOUT + "\n" +
                                  "deployer.pickupDirectory=\t" + PICKUP_FOLDER + "\n" +
                                  "\n" +
                                  "##########################\n" +
                                  "# OSGi Shell Configuration\n" +
                                  "##########################\n" +
                                  "shell.enabled=\t\t\t\t" + DMSHELL_ENABLED + "\n" +
                                  "shell.port=\t\t\t\t\t" + DMSHELL_PORT + "\n");
    setFileText(medicConfigFile, "dump.root.directory=" +
                                 DUMP_FOLDER +
                                 "\n" +
                                 "log.wrapSysOut=" +
                                 WRAP_SYS_OUT +
                                 "\n" +
                                 "log.wrapSysErr=" +
                                 WRAP_SYS_ERR +
                                 "\n" +
                                 "log.dump.level=DEBUG\n" +
                                 "log.dump.bufferSize=10000\n" +
                                 "log.dump.pattern=[%d{yyyy-MM-dd HH:mm:ss.SSS}] %-28.28thread %-64.64logger{64} %X{medic.eventCode} %msg %ex%n");
    setFileText(repositoryConfigFile, "ext.type=external\n" +
                                      "ext.searchPattern=" + EXTERNAL_ITEM_SEARCH_PATTERN + "\n" +
                                      "\n" +
                                      "usr.type=watched\n" +
                                      "usr.watchDirectory=" + WATCHED_ITEM_WATCH_DIRECTORY + "\n" +
                                      "usr.watchedInterval=" + WATCHED_ITEM_WATCHED_INTERVAL + "\n" +
                                      "\n" +
                                      "chain=ext,usr\n");
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    DMServerConfigSupport20 configSupport20 = new DMServerConfigSupport20(home);
    DMServerIntegrationData integrationData = new DMServerIntegrationData(INSTALLATION_HOME);
    configSupport20.readFromServer(integrationData);

    assertEquals(INSTALLATION_HOME, integrationData.getInstallationHome());
    assertEquals(DMSHELL_ENABLED, integrationData.isShellEnabled());
    assertEquals(DMSHELL_PORT, integrationData.getShellPort());
    assertEquals(DEPLOYMENT_TIMEOUT, integrationData.getDeploymentTimeoutSecs());
    assertEquals(PICKUP_FOLDER, integrationData.getPickupFolder());
    assertEquals(DUMP_FOLDER, integrationData.getDumpsFolder());
    assertEquals(WRAP_SYS_OUT, integrationData.isWrapSystemOut());
    assertEquals(WRAP_SYS_ERR, integrationData.isWrapSystemErr());
    List<DMServerRepositoryItem> repositoryItems = integrationData.getRepositoryItems();
    assertEquals(2, repositoryItems.size());
    DMServerRepositoryExternalItem externalItem = assertInstanceOf(repositoryItems.get(0), DMServerRepositoryExternalItem.class);
    assertEquals("ext", externalItem.getName());
    assertEquals(EXTERNAL_ITEM_SEARCH_PATTERN, externalItem.getPath());
    DMServerRepositoryWatchedItem watchedItem = assertInstanceOf(repositoryItems.get(1), DMServerRepositoryWatchedItem.class);
    assertEquals("usr", watchedItem.getName());
    assertEquals(WATCHED_ITEM_WATCH_DIRECTORY, watchedItem.getPath());
    assertEquals(WATCHED_ITEM_WATCHED_INTERVAL, watchedItem.getWatchedInterval());
  }

  public void testWriteConfig20() throws IOException {
    VirtualFile home = getTempDir().createVirtualDir();

    VirtualFile configDir = createChildDirectory(home, "config");
    VirtualFile kernelConfigFile = createChildData(configDir, "com.springsource.kernel.properties");
    VirtualFile medicConfigFile = createChildData(configDir, "com.springsource.osgi.medic.properties");
    VirtualFile repositoryConfigFile = createChildData(configDir, "com.springsource.repository.properties");
    setFileText(kernelConfigFile, """
      ########################
      # Deployer Configuration
      ########################
      # Note: use 0 to disable deployment timeouts
      deployer.timeout=\t\t\t8765
      deployer.pickupDirectory=\tPickup/Folder/To/Overwrite

      ##########################
      # OSGi Shell Configuration
      ##########################
      shell.enabled=\t\t\t\ttrue
      shell.port=\t\t\t\t\t4321
      """);
    setFileText(medicConfigFile, """
      dump.root.directory=Dump/Folder/To/Overwrite
      log.wrapSysOut=true
      log.wrapSysErr=true
      log.dump.level=DEBUG
      log.dump.bufferSize=10000
      log.dump.pattern=[%d{yyyy-MM-dd HH:mm:ss.SSS}] %-28.28thread %-64.64logger{64} %X{medic.eventCode} %msg %ex%n""");
    setFileText(repositoryConfigFile, """
      to-overwrite.type=external
      to-overwrite.searchPattern=to/{overwrite}
      chain=to-overwrite
      """);
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    final DMServerIntegrationData integrationData = new DMServerIntegrationData(INSTALLATION_HOME);
    integrationData.setInstallationHome(INSTALLATION_HOME);
    integrationData.setShellEnabled(DMSHELL_ENABLED);
    integrationData.setShellPort(DMSHELL_PORT);
    integrationData.setDeploymentTimeoutSecs(DEPLOYMENT_TIMEOUT);
    integrationData.setPickupFolder(PICKUP_FOLDER);
    integrationData.setDumpsFolder(DUMP_FOLDER);
    integrationData.setWrapSystemErr(WRAP_SYS_ERR);
    integrationData.setWrapSystemOut(WRAP_SYS_OUT);
    List<DMServerRepositoryItem> repositoryItems = integrationData.getRepositoryItems();
    repositoryItems.clear();
    DMServerRepositoryExternalItem externalItem = new DMServerRepositoryExternalItem();
    externalItem.setName("ext");
    externalItem.setPath(EXTERNAL_ITEM_SEARCH_PATTERN);
    repositoryItems.add(externalItem);
    DMServerRepositoryWatchedItem watchedItem = new DMServerRepositoryWatchedItem();
    watchedItem.setName("usr");
    watchedItem.setPath(WATCHED_ITEM_WATCH_DIRECTORY);
    watchedItem.setWatchedInterval(WATCHED_ITEM_WATCHED_INTERVAL);
    repositoryItems.add(watchedItem);

    final DMServerConfigSupport20 configSupport20 = new DMServerConfigSupport20(home);
    ApplicationManager.getApplication().runWriteAction(() -> configSupport20.writeToServer(integrationData));

    assertNormalizedFileText(kernelConfigFile,
      "#\n" +
      "shell.port=" + DMSHELL_PORT + "\n" +
      "deployer.timeout=" + DEPLOYMENT_TIMEOUT + "\n" +
      "shell.enabled=" + DMSHELL_ENABLED + "\n" +
      "deployer.pickupDirectory=" + FileUtil.toSystemDependentName(PICKUP_FOLDER).replace("\\", "\\\\")
    );

    assertNormalizedFileText(medicConfigFile, //
      "#\n" +
      "log.dump.pattern=[%d{yyyy-MM-dd HH\\:mm\\:ss.SSS}] %-28.28thread %-64.64logger{64} %X{medic.eventCode} %msg %ex%n\n" +
      "log.wrapSysOut=" + WRAP_SYS_OUT + "\n" +
      "log.dump.bufferSize=10000\n" +
      "dump.root.directory=" + FileUtil.toSystemDependentName(DUMP_FOLDER).replace("\\", "\\\\") + "\n" +
      "log.wrapSysErr=" + WRAP_SYS_ERR + "\n" +
      "log.dump.level=DEBUG"
      );

    assertNormalizedFileText(repositoryConfigFile,
      "#\n" +
      "usr.type=watched\n" +
      "usr.watchedInterval=" + WATCHED_ITEM_WATCHED_INTERVAL + "\n" +
      "chain=ext,usr\n" +
      "usr.watchDirectory=" + WATCHED_ITEM_WATCH_DIRECTORY + "\n" +
      "ext.searchPattern=" + EXTERNAL_ITEM_SEARCH_PATTERN + "\n" +
      "ext.type=external"
    );
  }

  private static void assertNormalizedFileText(VirtualFile file, String expectedText) throws IOException {
    String contents = VfsUtilCore.loadText(file);
    String normalizedContents = normalizeTextForComparison(contents);
    String normalizedExpected = normalizeTextForComparison(expectedText);
    assertEquals("Contents before normalization: \n" + contents, normalizedExpected, normalizedContents);
  }

  private static String normalizeTextForComparison(String text) {
    String[] parts = StringUtil.convertLineSeparators(text).split("\n");

    // require first text is comment, and ignore its contents
    assertEquals("first line `" + parts[0] + "`", '#', parts[0].charAt(0));
    parts[0] = "#";

    StringBuilder normalized = new StringBuilder();
    normalized.append("#").append("\n");
    Stream.of(parts)
      .skip(1)
      .filter(line -> line.length() > 0)
      .sorted()
      .forEach(line -> normalized.append(line).append("\n"));
    return normalized.toString();
  }
}
