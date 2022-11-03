package com.intellij.dmserver.install.impl;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.install.DMServerInstallationManager;
import com.intellij.dmserver.integration.DMServerIntegration;
import com.intellij.dmserver.integration.DMServerIntegrationData;
import com.intellij.dmserver.osmorc.DMServerFrameworkIntegrator;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.javaee.appServers.appServerIntegrations.AppServerIntegration;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServer;
import com.intellij.javaee.appServers.integration.impl.ApplicationServersManagerAdapter;
import com.intellij.javaee.appServers.serverInstances.ApplicationServersManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.text.UniqueNameGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osmorc.frameworkintegration.FrameworkInstanceDefinition;
import org.osmorc.settings.ApplicationSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DMServerInstallationManagerImpl extends DMServerInstallationManager {
  private final Map<String, DMServerInstallation> myInstallations = new HashMap<>();

  private final ApplicationSettings myApplicationSettings;

  public DMServerInstallationManagerImpl() {
    myApplicationSettings = ApplicationSettings.getInstance();

    ApplicationServersManager.getInstance().addServersListener(new ApplicationServersManagerAdapter() {
      @Override
      public void serverAdded(@NotNull ApplicationServer server) {
        AppServerIntegration sourceIntegration = server.getSourceIntegration();
        if ((sourceIntegration instanceof DMServerIntegration)) {
          findFramework(((DMServerIntegration)sourceIntegration).getServerInstallation(server));
        }
      }
    });
  }

  @NotNull
  @Override
  public List<? extends DMServerInstallation> getValidInstallations() {
    final List<ApplicationServer> servers =
      ApplicationServersManager.getInstance().getApplicationServers(DMServerIntegration.getInstance());
    List<DMServerInstallation> result = new ArrayList<>();
    for (ApplicationServer next : servers) {
      final DMServerInstallation nextInstall = ((DMServerIntegrationData)next.getPersistentData()).getInstallation();
      if (nextInstall != null && nextInstall.isValid()) {
        result.add(nextInstall);
      }
    }
    return result;
  }

  @Override
  @NotNull
  public DMServerInstallation findInstallation(VirtualFile home) {
    String homePath = FileUtil.toSystemIndependentName(home.getPath());
    DMServerInstallation result = myInstallations.get(homePath);
    if (result == null) {
      result = new DMServerInstallationImpl(home);
      myInstallations.put(homePath, result);
    }
    return result;
  }

  @Override
  public FrameworkInstanceDefinition findFramework(DMServerInstallation installation) {
    return findFramework(installation, true);
  }

  @Override
  public FrameworkInstanceDefinition findFramework(DMServerInstallation installation, boolean create) {
    if (installation == null || !installation.isValid()) {
      return null;
    }

    for (FrameworkInstanceDefinition framework : myApplicationSettings.getFrameworkInstanceDefinitions()) {
      if (DMServerFrameworkIntegrator.FRAMEWORK_NAME.equals(framework.getFrameworkIntegratorName())) {
        VirtualFile frameworkHome =
          LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(framework.getBaseFolder()));
        if (frameworkHome != null && frameworkHome.getPath().equals(installation.getHome().getPath())) {
          return framework;
        }
      }
    }

    if (!create) {
      return null;
    }

    FrameworkInstanceDefinition newFramework = new FrameworkInstanceDefinition();

    UniqueNameGenerator generator =
      new UniqueNameGenerator(myApplicationSettings.getFrameworkInstanceDefinitions(),
                              o -> createFrameworkName(o.getName()));

    newFramework.setName(generator.generateUniqueName(createFrameworkName(installation.getOrCreateApplicationServer().getName())));
    newFramework.setFrameworkIntegratorName(DMServerFrameworkIntegrator.FRAMEWORK_NAME);
    newFramework.setBaseFolder(FileUtil.toSystemDependentName(installation.getHome().getPath()));


    myApplicationSettings.getFrameworkInstanceDefinitions().add(newFramework);

    return newFramework;
  }

  private static String createFrameworkName(String basicName) {
    return DmServerBundle.message("DMServerInstallationManagerImpl.framework.name", basicName);
  }

  @Override
  @Nullable
  public DMServerInstallation findInstallation(@NotNull String homePath) {
    VirtualFile home = LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(homePath));
    return home == null ? null : findInstallation(home);
  }

  @Override
  @Nullable
  public DMServerInstallation findInstallation(FrameworkInstanceDefinition framework) {
    return findInstallation(framework.getBaseFolder());
  }
}
