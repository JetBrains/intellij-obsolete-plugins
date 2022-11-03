package com.intellij.dmserver.facet;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.run.DMServerRunConfigurationType;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.execution.RunManager;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.ide.util.frameworkSupport.FrameworkVersion;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServer;
import com.intellij.javaee.appServers.run.configuration.CommonStrategy;
import com.intellij.javaee.web.artifact.WebArtifactUtil;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osmorc.facet.OsmorcFacetType;

public class DMBundleSupportProvider extends DMFacetSupportProviderBase<DMBundleFacet, DMBundleFacetConfiguration> {

  private final DMWebFacetFrameworkSupportProvider myWebSupportProvider;

  public DMBundleSupportProvider() {
    super(DMBundleFacet.ID);
    myWebSupportProvider = new DMWebFacetFrameworkSupportProvider();
  }

  public DMWebFacetFrameworkSupportProvider getWebSupportProvider() {
    return myWebSupportProvider;
  }

  @Override
  protected void setupConfiguration(DMBundleFacet dmBundleFacet,
                                    ModifiableRootModel modifiableRootModel,
                                    FrameworkVersion frameworkVersion) {
    //TODO: we don't have any dmSpecific artifacts here
  }

  @Override
  public String getUnderlyingFrameworkId() {
    return null;
  }

  @Override
  public void addDMSupport(@NotNull Module module,
                           @NotNull ModifiableRootModel rootModel) {
    DMBundleFacet.addRequiredFacet(module, OsmorcFacetType.getInstance(), null);
    super.addDMSupport(module, rootModel);
  }

  @Override
  protected void doFinishAddDMSupport(Module module,
                                      ModifiableRootModel rootModel,
                                      DMServerInstallation installation,
                                      DMBundleFacet facet) {
    if (installation == null || !installation.isValid()) {
      return;
    }

    Project project = module.getProject();
    Artifact artifact = facet.getMainArtifact();

    ApplicationServer appServer = installation.getOrCreateApplicationServer();
    ConfigurationFactory type = DMServerRunConfigurationType.getInstance().getLocalFactory();
    RunManager runManager = RunManager.getInstance(project);
    RunnerAndConfigurationSettingsImpl runSettings = (RunnerAndConfigurationSettingsImpl)runManager.createConfiguration(
      type.getName(), type);

    CommonStrategy configuration = (CommonStrategy)runSettings.getConfiguration();
    configuration.setApplicationServer(appServer);

    if (artifact != null) {
      BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun(project, configuration, artifact);
    }

    runManager.addConfiguration(runSettings);
    runManager.setSelectedConfiguration(runSettings);

    Library apiJar = addRequiredJars(module, DmServerBundle.message("DMBundleSupportProvider.server.library.name"), installation);
    rootModel.addLibraryEntry(apiJar);
    if (artifact != null) {
      WebArtifactUtil.getInstance().addLibrary(apiJar, artifact, project);
    }
  }

  private static Library addRequiredJars(final Module module, final String libraryName, @Nullable final DMServerInstallation installation) {
    return WriteAction.compute(() -> {
      final LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(module.getProject());
      Library library = libraryTable.getLibraryByName(libraryName);
      if (library == null) {
        library = libraryTable.createLibrary(libraryName);
        final Library.ModifiableModel model = library.getModifiableModel();
        if (installation != null) {
          for (VirtualFile nextJar : installation.getSharedLibraries()) {
            model.addJarDirectory(nextJar, false);
          }
        }
        model.commit();
      }
      return library;
    });
  }
}
