// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.GroovyMvcIcons;
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutor;
import org.jetbrains.plugins.grails.runner.GrailsConsole;
import org.jetbrains.plugins.grails.runner.GrailsInstallationExecutor;
import org.jetbrains.plugins.grails.sdk.GrailsSDK;
import org.jetbrains.plugins.grails.sdk.GrailsSDKManager;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

import javax.swing.Icon;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class GrailsModuleBuilder extends GrailsGradleAwareModuleBuilder {

  private static final Logger LOG = Logger.getInstance(GrailsModuleBuilder.class);

  private GrailsSDK myGrailsSDK;
  private @NotNull String myCreationChoice = "create-app";
  private @Nullable String myOptions;

  @Override
  public boolean isAvailable() {
    return Registry.is("grails.cli.project.wizard");
  }

  @Override
  public boolean isSuitableSdkType(SdkTypeId sdkType) {
    return sdkType instanceof JavaSdkType && !((JavaSdkType)sdkType).isDependent();
  }

  @Override
  public void setupRootModel(@NotNull ModifiableRootModel modifiableRootModel) throws ConfigurationException {
    // todo this should be moved to generic ModuleBuilder
    if (myJdk != null) {
      modifiableRootModel.setSdk(myJdk);
    }
    else {
      modifiableRootModel.inheritSdk();
    }

    if (doAddContentEntry(modifiableRootModel) == null) {
      throw new ConfigurationException(GrailsBundle.message("module.builder.cannot.setup.root.path.error"));
    }
  }

  @Override
  protected void setupModule(final @NotNull Module module) throws ConfigurationException {
    super.setupModule(module);
    final Project project = module.getProject();
    StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> {
      try {
        if (module.isDisposed()) return;
        GrailsConsole.getInstance(project);
        doSetupModule(module);
      }
      catch (Exception e) {
        LOG.debug(e);
        GrailsConsole.NOTIFICATION_GROUP
          .createNotification(GrailsBundle.message("notification.title.failed.to.create.grails.app.structure"), String.valueOf(e.getMessage()), NotificationType.WARNING)
          .notify(project);
      }
    });
  }

  @Override
  public @Nullable ModuleWizardStep getCustomOptionsStep(final WizardContext context, Disposable parentDisposable) {
    this.setCreatingNewProject(context.isCreatingNewProject());

    return new GrailsOptionsWizardStep(context, this);
  }

  @Override
  public ModuleType getModuleType() {
    return JavaModuleType.getModuleType();
  }

  @Override
  public @NonNls String getBuilderId() {
    return "Grails";
  }

  @Override
  public String getPresentableName() {
    return GrailsBundle.message("library.name");
  }

  @Override
  public String getDescription() {
    return GrailsBundle.message("module.description.grails.application.module.builder");
  }

  @Override
  public Icon getNodeIcon() {
    return GroovyMvcIcons.Grails;
  }

  @Override
  public String getParentGroup() {
    return "Groovy";
  }

  @Override
  public int getWeight() {
    return GROOVY_WEIGHT;
  }

  public void setGrailsSDKHome(GrailsSDK grailsSDK) {
    myGrailsSDK = grailsSDK;
  }

  public void setCreateChoice(@NotNull String creationChoice) {
    myCreationChoice = creationChoice;
  }

  public void setOptions(@Nullable String options) {
    myOptions = StringUtil.isEmptyOrSpaces(options) ? null : options;
  }

  private void doSetupModule(@NotNull Module module) throws ExecutionException {
    final Project project = module.getProject();

    if (isCreatingNewProject()) { // Suppress "Link Gradle project" notification
      project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, Boolean.TRUE);
      project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, Boolean.TRUE);
    }

    final VirtualFile rootModuleContent = getModuleRoot(module);
    GrailsSDKManager.getInstance(project).setGrailsSDK(rootModuleContent.getPath(), myGrailsSDK.getPath());
    final Pair<GeneralCommandLine, Path> parameters = getCreationCommandLine(module);
    final Consumer<Module> linker = myGrailsSDK.getVersion().isAtLeast(Version.GRAILS_3_0) ? this::linkModule : null;
    final Runnable onDone = () -> {
      try {
        FileUtil.copyDirContent(parameters.second.toFile(), rootModuleContent.toNioPath().toFile());
        FileUtil.delete(parameters.second);

        LocalFileSystem.getInstance().refresh(true);
      }
      catch (IOException e) {
        LOG.debug("Cannot copy content of Grails module: " + module);
        return;
      }

      LOG.debug("Grails application structure created for module: " + module);
      if (linker != null) linker.accept(module);
    };
    GrailsConsole.executeProcess(project, parameters.first, onDone, false);
  }

  private @NotNull Pair<GeneralCommandLine, Path> getCreationCommandLine(@NotNull Module module) throws ExecutionException {
    final Project project = module.getProject();
    final VirtualFile root = getModuleRoot(module);

    Path directory;
    try {
      directory = Files.createTempDirectory("grails-new-project"); // Grails CLI 5.0.0 requires empty directory
    }
    catch (IOException e) {
      throw new ExecutionException(e);
    }

    final MvcCommand command = new MvcCommand(myCreationChoice, root.getName(), "--inplace");
    if (myOptions != null) ContainerUtil.addAll(command.getArgs(), myOptions.split(" "));

    final Sdk sdk = getModuleJdk() == null ? ProjectRootManager.getInstance(project).getProjectSdk() : getModuleJdk();
    if (sdk == null) throw new ExecutionException(ExecutionBundle.message("no.jdk.for.module.error.message", module.getName()));

    final JavaParameters params = getExecutor().createJavaParameters(sdk, myGrailsSDK, command);
    params.setWorkingDirectory(directory.toFile().getAbsolutePath());
    params.setDefaultCharset(project);

    final GeneralCommandLine parameters = params.toCommandLine();
    LOG.debug(parameters.getCommandLineString());
    return new Pair<>(parameters, directory);
  }

  private @NotNull GrailsInstallationExecutor getExecutor() throws ExecutionException {
    for (GrailsCommandExecutor executor : GrailsCommandExecutor.EP_NAME.getExtensions()) {
      if (executor instanceof GrailsInstallationExecutor installationExecutor) {
        if (installationExecutor.isApplicable(myGrailsSDK)) return installationExecutor;
      }
    }
    throw new ExecutionException(GrailsBundle.message("dialog.message.cannot.create.application.with.grails.sdk", myGrailsSDK.getPath()));
  }
}
