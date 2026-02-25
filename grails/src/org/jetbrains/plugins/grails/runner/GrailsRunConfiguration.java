// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizer;
import com.intellij.openapi.util.NlsContexts.TabTitle;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.util.JdomKt;
import com.intellij.util.containers.ContainerUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.runner.ui.GrailsRunConfigurationEditor;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public final class GrailsRunConfiguration
  extends LocatableRunConfigurationWithCommonParameters
  implements RunProfileWithCompileBeforeLaunchOption {

  private static final String ROOT_ELEMENT_NAME = "grailsApplicationRoot";
  private static final String LAUNCH_BROWSER = "launchBrowser";
  private static final String LAUNCH_BROWSER_URL = "launchBrowserUrl";

  private @Nullable String myGrailsApplicationRootPath;
  private boolean myLaunchBrowser;
  private @Nullable String myLaunchBrowserUrl;

  public GrailsRunConfiguration(@NotNull Project project,
                                @NotNull ConfigurationFactory factory,
                                String name) {
    super(project, factory, name);
    setProgramParameters("run-app");
    myLaunchBrowser = true;
  }

  public boolean isLaunchBrowser() {
    return myLaunchBrowser;
  }

  public void setLaunchBrowser(boolean launchBrowser) {
    myLaunchBrowser = launchBrowser;
  }

  public @Nullable String getLaunchBrowserUrl() {
    return StringUtil.nullize(myLaunchBrowserUrl, true);
  }

  public void setLaunchBrowserUrl(@Nullable String launchBrowserUrl) {
    myLaunchBrowserUrl = StringUtil.nullize(launchBrowserUrl, true);
  }

  @Override
  public @NotNull SettingsEditor<GrailsRunConfiguration> getConfigurationEditor() {
    final SettingsEditorGroup<GrailsRunConfiguration> group = new SettingsEditorGroup<>();

    final GrailsRunConfigurationEditor runConfigurationEditor = new GrailsRunConfigurationEditor(getProject());
    group.addEditor(GrailsBundle.message("library.name"), runConfigurationEditor);

    final List<GrailsRunConfigurationExtension> configurationExtensions = ContainerUtil.findAll(
      GrailsCommandExecutor.EP_NAME.getExtensions(), GrailsRunConfigurationExtension.class
    );
    for (GrailsRunConfigurationExtension<?> extension : configurationExtensions) {
      final SettingsEditor<GrailsRunConfiguration> extensionEditor = extension.createExtensionEditor();
      if (extensionEditor != null) {
        runConfigurationEditor.addExtension(extensionEditor);
      }

      final Pair<@TabTitle String, SettingsEditor<GrailsRunConfiguration>> pair = extension.createSettingsEditor(getProject());
      if (pair != null) {
        group.addEditor(pair.first, pair.second);
      }
    }

    JavaRunConfigurationExtensionManager.getInstance().appendEditors(this, group);

    return group;
  }

  @Override
  public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
    final GrailsCommandExecutor grailsExecutor = getGrailsExecutor();
    return grailsExecutor.getState(this, executor, environment);
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    final GrailsApplication grailsApplication = getGrailsApplicationNullable();
    if (grailsApplication == null) {
      throw new RuntimeConfigurationException(GrailsBundle.message("run.configuration.error.application.not.selected"));
    }
    final MvcCommand grailsCommand = getGrailsCommandNullable();
    if (grailsCommand == null) throw new RuntimeConfigurationException(GrailsBundle.message("run.configuration.error.command.not.specified"));

    final GrailsCommandExecutor grailsExecutor = GrailsCommandExecutor.getGrailsExecutor(grailsApplication);
    if (grailsExecutor == null) throw new RuntimeConfigurationException(
      GrailsBundle.message("run.configuration.error.cannot.execute.command", grailsApplication.getRoot().getPath()));

    if (isLaunchBrowser()) {
      String browserUrl = getLaunchBrowserUrl();
      if (browserUrl != null) {
        try {
          //noinspection ResultOfObjectAllocationIgnored
          new URL(browserUrl);
        }
        catch (MalformedURLException e) {
          throw new RuntimeConfigurationException(GrailsBundle.message("run.configuration.error.invalid.launch.url"));
        }
      }
    }
  }

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);

    myGrailsApplicationRootPath = JDOMExternalizer.readString(element, ROOT_ELEMENT_NAME);
    myLaunchBrowser = !"false".equals(JDOMExternalizer.readString(element, LAUNCH_BROWSER));
    myLaunchBrowserUrl = JDOMExternalizer.readString(element, LAUNCH_BROWSER_URL);

    final List<GrailsRunConfigurationExtension> configurationExtensions = ContainerUtil.findAll(
      GrailsCommandExecutor.EP_NAME.getExtensions(), GrailsRunConfigurationExtension.class
    );
    for (GrailsRunConfigurationExtension extension : configurationExtensions) {
      final Object additionalConfiguration = extension.readAdditionalConfiguration(element);
      //noinspection unchecked
      putUserData(extension.getKey(), additionalConfiguration);
    }

    JavaRunConfigurationExtensionManager.getInstance().readExternal(this, element);
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element);

    JDOMExternalizer.write(element, ROOT_ELEMENT_NAME, myGrailsApplicationRootPath);
    JdomKt.addOptionTag(element, LAUNCH_BROWSER, Boolean.toString(myLaunchBrowser), "setting");
    JDOMExternalizer.write(element, LAUNCH_BROWSER_URL, myLaunchBrowserUrl);

    final List<GrailsRunConfigurationExtension> configurationExtensions = ContainerUtil.findAll(
      GrailsCommandExecutor.EP_NAME.getExtensions(), GrailsRunConfigurationExtension.class
    );

    for (GrailsRunConfigurationExtension extension : configurationExtensions) {
      //noinspection unchecked
      final Object additionalConfiguration = getUserData(extension.getKey());
      if (additionalConfiguration != null) {
        //noinspection unchecked
        extension.writeAdditionalConfiguration(additionalConfiguration, element);
      }
    }

    JavaRunConfigurationExtensionManager.getInstance().writeExternal(this, element);
  }

  public @Nullable GrailsApplication getGrailsApplicationNullable() {
    final String path = myGrailsApplicationRootPath;
    if (path == null) return null;
    return GrailsApplicationManager.getInstance(getProject()).getApplicationByRoot(
      LocalFileSystem.getInstance().findFileByPath(path)
    );
  }

  public @NotNull GrailsApplication getGrailsApplication() throws ExecutionException {
    final GrailsApplication application = getGrailsApplicationNullable();
    if (application == null) throw new ExecutionException(GrailsBundle.message("dialog.message.grails.application.not.found"));
    return application;
  }

  public void setGrailsApplication(@Nullable GrailsApplication grailsApplication) {
    myGrailsApplicationRootPath = grailsApplication == null ? null : grailsApplication.getRoot().getPath();
  }

  public @NotNull GrailsCommandExecutor getGrailsExecutor() throws ExecutionException {
    final GrailsApplication grailsApplication = getGrailsApplication();
    final GrailsCommandExecutor grailsExecutor = GrailsCommandExecutor.getGrailsExecutor(grailsApplication);
    if (grailsExecutor == null) {
      throw new ExecutionException(
        GrailsBundle.message("dialog.message.cannot.execute.grails.command", grailsApplication.getRoot().getPath())
      );
    }
    return grailsExecutor;
  }

  public @Nullable MvcCommand getGrailsCommandNullable() {
    final String parameters = getProgramParameters();
    if (parameters == null) return null;
    return MvcCommand.parse(parameters).setVmOptions(getVMParameters()).setEnvVariables(getEnvs()).setPassParentEnvs(isPassParentEnvs());
  }

  public @NotNull MvcCommand getGrailsCommand() throws ExecutionException {
    final String parameters = getProgramParameters();
    if (parameters == null) throw new ExecutionException(GrailsBundle.message("dialog.message.command.parameters.are.empty"));

    return MvcCommand.parse(parameters).setVmOptions(getVMParameters()).setEnvVariables(getEnvs()).setPassParentEnvs(isPassParentEnvs());
  }
}
