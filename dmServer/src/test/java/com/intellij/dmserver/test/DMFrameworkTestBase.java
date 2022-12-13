package com.intellij.dmserver.test;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.install.DMServerInstallationManager;
import com.intellij.dmserver.integration.DMServerIntegrationData;
import com.intellij.dmserver.osmorc.FrameworkUtils;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.io.Compressor;
import org.osmorc.frameworkintegration.FrameworkInstanceDefinition;
import org.osmorc.settings.ApplicationSettings;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.jar.JarFile;

public abstract class DMFrameworkTestBase extends DMTestBase {
  @Override
  protected void tearDown() throws Exception {
    try {
      JavaeeFrameworkSupportProviderTestCase.deleteApplicationServers();
      ApplicationSettings.getInstance().setFrameworkInstanceDefinitions(new ArrayList<>());
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  protected final DMServerInstallation createAndSetActiveInstallation() {
    VirtualFile homeDir = getTempDir().createVirtualDir();
    VirtualFile binDir = createChildDirectory(homeDir, "bin");
    String scriptExt = SystemInfo.isWindows ? "bat" : "sh";
    VirtualFile startupFile = createChildData(binDir, "startup." + scriptExt);
    VirtualFile shutdownFile = createChildData(binDir, "shutdown." + scriptExt);
    VirtualFile repositoryDir = createChildDirectory(homeDir, "repository");
    VirtualFile usrDir = createChildDirectory(repositoryDir, "usr");
    VirtualFile extDir = createChildDirectory(repositoryDir, "ext");
    VirtualFile lib = createChildDirectory(homeDir, "lib");
    VirtualFile versionFile = createChildData(lib, ".version");
    setFileText(versionFile, "dm.kernel.version=2.0.0.RC1\n" +
                             "dm.server.version=2.0.0.RC1");
    VirtualFile configDir = createChildDirectory(homeDir, "config");
    VirtualFile kernelConfigFile = createChildData(configDir, "com.springsource.kernel.properties");
    setFileText(kernelConfigFile, """
      deployer.timeout=300
      deployer.pickupDirectory=pickup
      shell.enabled=true
      shell.port=2401
      """);
    VirtualFile medicConfigFile = createChildData(configDir, "com.springsource.osgi.medic.properties");
    setFileText(medicConfigFile, """
      dump.root.directory=serviceability/dump
      log.wrapSysOut=true
      log.wrapSysErr=true
      log.dump.level=DEBUG
      log.dump.bufferSize=10000
      log.dump.pattern=[%d{yyyy-MM-dd HH:mm:ss.SSS}] %-28.28thread %-64.64logger{64} %X{medic.eventCode} %msg %ex%n
      """);
    VirtualFile repositoryConfigFile = createChildData(configDir, "com.springsource.repository.properties");
    setFileText(repositoryConfigFile, """
      ext.type=external
      ext.searchPattern=repository/ext/{artifact}

      usr.type=watched
      usr.watchDirectory=repository/usr

      chain=ext,usr
      """);
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    DMServerInstallation installation = DMServerInstallationManager.getInstance().findInstallation(homeDir);

    DMServerIntegrationData data = (DMServerIntegrationData)installation.getOrCreateApplicationServer().getPersistentData();

    installation.getConfigSupport().readFromServer(data);


    FrameworkInstanceDefinition framework = DMServerInstallationManager.getInstance().findFramework(installation);
    FrameworkUtils.getInstance().setActiveFrameworkInstance(getProject(), framework);

    return installation;
  }

  protected final void createMockBundle(String path, String name, String version, String symbolicName) throws IOException {
    createMockBundle(path, name, version, symbolicName, "");
  }

  protected final void createMockBundle(String path, String name, String version, String symbolicName, String additionalHeaders) throws IOException {
    String manifest = "Manifest-Version: 1.0\n" +
                      "Bundle-Name: " + name + "\n" +
                      "Bundle-Version: " + version + "\n" +
                      "Bundle-ManifestVersion: 2\n" +
                      "Bundle-SymbolicName: " + symbolicName + "\n" +
                      additionalHeaders;
    try (Compressor zip = new Compressor.Zip(new File(path))) {
      zip.addFile(JarFile.MANIFEST_NAME, manifest.getBytes(StandardCharsets.UTF_8));
    }
  }

  protected final void createMockLibrary(VirtualFile dir, String fileName, String name, String version, String symbolicName) {
    VirtualFile libraryFile = createChildData(dir, fileName);
    setFileText(libraryFile, "Library-SymbolicName: " + symbolicName + "\n" +
                             "Library-Version: " + version + "\n" +
                             "Library-Name: " + name + "\n" +
                             "Import-Bundle: nothing;version=\"[1.0.0, 1.0.0]\"\n");
  }
}
