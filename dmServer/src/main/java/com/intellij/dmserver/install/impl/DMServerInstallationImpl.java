package com.intellij.dmserver.install.impl;

import com.intellij.dmserver.install.DMServerConfigSupport;
import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.install.ServerVersionHandler;
import com.intellij.dmserver.integration.*;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.facet.ui.ValidationResult;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServer;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServerInfo;
import com.intellij.javaee.oss.util.Version;
import com.intellij.javaee.appServers.serverInstances.ApplicationServersManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.text.UniqueNameGenerator;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class DMServerInstallationImpl implements DMServerInstallation {

  private static final Logger LOG = Logger.getInstance(DMServerInstallationImpl.class);

  private final VirtualFile myHome;
  private final VirtualFile myStartup;
  private final VirtualFile myShutdown;
  private final VirtualFile myTempFolder;
  private final ServerVersionHandler myServerVersion;
  private final DMServerConfigSupport myConfigSupport;

  @NonNls
  private static final String SPRINGSOURCE_JAVAX_PREFIX = "com.springsource.javax.";
  @NonNls
  private static final String DM_VERSION_PROPERTY_NAME = "dm.server.version";
  @NonNls
  private static final String VIRGO_VERSION_PROPERTY_NAME = "virgo.server.version";

  private boolean myVersionValid;
  private String myVersionText;

  public DMServerInstallationImpl(VirtualFile home) {
    myHome = home;
    myStartup = home.findFileByRelativePath("bin/startup." + getScriptExtension());
    myShutdown = home.findFileByRelativePath("bin/shutdown." + getScriptExtension());
    myTempFolder = home.findFileByRelativePath("work/temp");

    VirtualFile versionFile = home.findFileByRelativePath("lib/.version");
    myServerVersion = detectVersion(versionFile);
    myConfigSupport = myServerVersion.createConfigSupport(home);
  }

  @Override
  public DMServerConfigSupport getConfigSupport() {
    return myConfigSupport;
  }

  @Override
  public VirtualFile getHome() {
    return myHome;
  }

  @Override
  public VirtualFile getTempFolder() {
    return myTempFolder;
  }

  @Override
  public VirtualFile[] getSharedLibraries() {
    if (!isValid()) {
      return VirtualFile.EMPTY_ARRAY;
    }

    ApplicationServer existingServer = getExistingApplicationServer();
    List<RepositoryPattern> repositoryPatterns
      = doCollectRepositoryPatterns(existingServer == null ? createServerData()
                                                           : (DMServerIntegrationData)existingServer.getPersistentData());

    List<VirtualFile> jars = new ArrayList<>();
    DMServerLibraryFinder sourceBundlesFilter = new DMServerLibraryFinder();
    for (RepositoryPattern repositoryPattern : repositoryPatterns) {
      for (VirtualFile repositoryFile : repositoryPattern.collectFiles()) {
        if ("jar".equals(repositoryFile.getExtension())
            && (!sourceBundlesFilter.isSourcesBundle(repositoryFile)
                && repositoryFile.getName().startsWith(SPRINGSOURCE_JAVAX_PREFIX))) {
          jars.add(repositoryFile);
        }
      }
    }
    return VfsUtilCore.toVirtualFileArray(jars);
  }

  @Override
  @Nullable
  public VirtualFile getSystemLibraryFolder() {
    //isn't version dependent
    return myHome.findFileByRelativePath("lib");
  }

  @Override
  public boolean isValid() {
    return isValidFile(myStartup) && isValidFile(myShutdown) && myVersionValid && myConfigSupport.isValid();
  }

  @Nullable
  private ApplicationServer getExistingApplicationServer() {
    if (!isValid()) {
      return null;
    }

    final List<ApplicationServer> servers = DMServerIntegration.getInstance().getDMServers();
    File sdkHomeFile = new File(FileUtil.toSystemDependentName(myHome.getPath()));
    for (ApplicationServer next : servers) {
      final String nextPath = ((DMServerIntegrationData)next.getPersistentData()).getInstallationHome();
      if (sdkHomeFile.equals(new File(FileUtil.toSystemDependentName(nextPath)))) {
        return next;
      }
    }

    return null;
  }

  private DMServerIntegrationData createServerData() {
    DMServerIntegrationData result = new DMServerIntegrationData(myHome.getPath());
    getConfigSupport().readFromServer(result);
    return result;
  }

  @Override
  public ApplicationServer getOrCreateApplicationServer() {
    ApplicationServer existingServer = getExistingApplicationServer();
    if (existingServer != null) {
      return existingServer;
    }
    final ApplicationServersManager serversManager = ApplicationServersManager.getInstance();
    final ApplicationServersManager.ApplicationServersManagerModifiableModel model = serversManager.createModifiableModel();
    final DMServerIntegrationData serverData = createServerData();
    final ApplicationServerInfo serverInfo;
    serverInfo = DMServerHelper.createApplicationServerInfo(this);
    UniqueNameGenerator generator = new UniqueNameGenerator(serversManager.getApplicationServers(),
                                                            o -> o.getName());
    String nameBase = serverInfo.getDefaultName();
    final String name = generator.generateUniqueName(nameBase);
    return WriteAction.computeAndWait(() -> {
      ApplicationServer server = model.createNewApplicationServer(name, serverInfo.getDefaultLibraries(), serverData);
      server.setIntegration(DMServerIntegration.getInstance());
      model.commit();
      return server;
    });
  }

  public static boolean isValidFile(VirtualFile file) {
    return file != null && file.isValid() && file.exists() && !file.isDirectory();
  }

  @NonNls
  private static String getScriptExtension() {
    return SystemInfo.isWindows ? "bat" : "sh";
  }

  public ValidationResult validate() {
    if (!myHome.isDirectory()) {
      return new ValidationResult(DmServerBundle.message("DMServerInstallationImpl.error.not.a.folder", myHome.getPath()));
    }
    if (!isValidFile(myStartup)) {
      return new ValidationResult(DmServerBundle.message("DMServerInstallationImpl.error.no.startup.script", myHome.getPath()));
    }
    if (!isValidFile(myShutdown)) {
      return new ValidationResult(DmServerBundle.message("DMServerInstallationImpl.error.no.shutdown.script", myHome.getPath()));
    }
    if (!myVersionValid) {
      return new ValidationResult(DmServerBundle.message("DMServerInstallationImpl.error.detect.version.script", myHome.getPath()));
    }
    if (!myConfigSupport.isValid()) {
      return new ValidationResult(DmServerBundle.message("DMServerInstallationImpl.error.no.config.file", myHome.getPath()));
    }
    return ValidationResult.OK;
  }

  @Override
  public ServerVersionHandler getServerVersion() {
    return myServerVersion;
  }

  private interface VersionHandlerFactory {

    String getPropertyName();

    ServerVersionHandler createHandler();
  }

  private ServerVersionHandler detectVersion(VirtualFile version) {
    myVersionValid = true;

    if (version == null || !version.exists()) {
      //version 1.0 does not have a .version file, that's a differentiator
      return new ServerVersion10();
    }

    VersionHandlerFactory defaultHandlerFactory = new VersionHandlerFactory() {

      @Override
      public String getPropertyName() {
        throw new UnsupportedOperationException();
      }

      @Override
      public ServerVersionHandler createHandler() {
        myVersionValid = false;
        return new ServerVersionVirgo35();
      }
    };

    VersionHandlerFactory[] detectableFactories = new VersionHandlerFactory[]{
      new VersionHandlerFactory() {

        @Override
        public String getPropertyName() {
          return DM_VERSION_PROPERTY_NAME;
        }

        @Override
        public ServerVersionHandler createHandler() {
          return new ServerVersion20();
        }
      },
      new VersionHandlerFactory() {

        @Override
        public String getPropertyName() {
          return VIRGO_VERSION_PROPERTY_NAME;
        }

        @Override
        public ServerVersionHandler createHandler() {
          Version parsedVersion = new Version(myVersionText);
          if (parsedVersion.compare(3, 7, 0) >= 0) {
            return new ServerVersionVirgo37();
          }
          else if (parsedVersion.compare(3, 6, 0) >= 0) {
            return new ServerVersionVirgo36();
          }
          else if (parsedVersion.compare(3, 5, 0) >= 0) {
            return new ServerVersionVirgo35();
          }
          else if (parsedVersion.compare(3, 0, 0) >= 0) {
            return new ServerVersionVirgo3();
          }
          else {
            return new ServerVersion21Virgo();
          }
        }
      }
    };

    Properties versionProperties;
    try {
      versionProperties = PropertiesUtil.loadProperties(version);
    }
    catch (IOException e) {
      LOG.debug("Error reading version properties file", e);
      return defaultHandlerFactory.createHandler();
    }

    for (VersionHandlerFactory factory : detectableFactories) {
      String propertyName = factory.getPropertyName();
      if (versionProperties.containsKey(propertyName)) {
        myVersionText = versionProperties.getProperty(propertyName);
        return factory.createHandler();
      }
    }

    return defaultHandlerFactory.createHandler();
  }

  @Override
  public String getVersionName() {
    return myVersionText != null ? myVersionText : myServerVersion.getFamilyName();
  }

  @Override
  public List<RepositoryPattern> collectRepositoryPatterns() {
    if (!isValid()) {
      return Collections.emptyList();
    }
    return doCollectRepositoryPatterns((DMServerIntegrationData)getOrCreateApplicationServer().getPersistentData());
  }

  private List<RepositoryPattern> doCollectRepositoryPatterns(final DMServerIntegrationData serverData) {
    PathResolver pathResolver = new PathResolver() {

      @Override
      protected VirtualFile getBaseDir() {
        return myHome;
      }
    };

    List<RepositoryPattern> result = new ArrayList<>();
    for (DMServerRepositoryItem repositoryItem : serverData.getRepositoryItems()) {
      result.add(repositoryItem.createPattern(pathResolver));
    }
    return result;
  }
}
