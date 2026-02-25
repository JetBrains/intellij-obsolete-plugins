// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.groovy.grails.maven;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts.TabTitle;
import com.intellij.openapi.util.Pair;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenExternalParameters;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType;
import org.jetbrains.idea.maven.execution.MavenRunner;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;
import org.jetbrains.idea.maven.importing.MavenPomPathModuleService;
import org.jetbrains.idea.maven.model.MavenExplicitProfiles;
import org.jetbrains.idea.maven.project.MavenGeneralSettings;
import org.jetbrains.idea.maven.project.MavenGeneralSettingsEditor;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.runner.GrailsCommandLineExecutor;
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration;
import org.jetbrains.plugins.grails.runner.GrailsRunConfigurationExtension;
import org.jetbrains.plugins.grails.runner.util.GrailsExecutionUtils;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

import javax.swing.JComponent;
import java.util.Map;

import static org.jetbrains.plugins.grails.runner.util.GrailsExecutionUtils.addCommonJvmOptions;

final class MavenCommandExecutor extends GrailsCommandLineExecutor implements GrailsRunConfigurationExtension<MavenGeneralSettings> {

  public static final Key<MavenGeneralSettings> DATA_KEY = Key.create(MavenCommandExecutor.class.getName() + " data key");

  private static final Map<String, String> ourCommandGoals = GrailsUtils.createMap(
    "run-app", "grails:run-app",
    "test-app", "grails:test-app",
    "war", "grails:war");

  @Override
  public boolean isApplicable(@NotNull GrailsApplication grailsApplication) {
    return grailsApplication instanceof GrailsMavenApplication;
  }

  @Override
  public @NotNull JavaParameters createJavaParameters(@NotNull GrailsApplication grailsApplication,
                                                      @NotNull MvcCommand command,
                                                      @Nullable MavenGeneralSettings generalSettings) throws ExecutionException {
    if (!(grailsApplication instanceof GrailsMavenApplication)) {
      throw new ExecutionException(GrailsBundle.message("dialog.message.maven.cannot.execute", grailsApplication.getClass()));
    }
    final Project project = grailsApplication.getProject();
    final MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(project);

    final MavenRunnerSettings runnerSettings = MavenRunner.getInstance(project).getSettings().clone();

    runnerSettings.setVmOptions(command.getVmOptions());

    String parameterStr = null;
    if (!command.getArgs().isEmpty()) {
      parameterStr = ParametersList.join(command.getArgs());
    }

    String goal = null;

    String mappedGoal = ourCommandGoals.get(command.getCommand());

    if ("test-app".equals(command.getCommand())) {
      final Version grailsVersion = grailsApplication.getGrailsVersion();

      if (grailsVersion.compareToString("2.1.0") < 0) {
        mappedGoal = null; // We should run Grails tests using 'mvn -Dcommand=test-app -Dargs=<TestClassName> grails:exec' instead of
        // "mvn grails:test-app -Dgrails.cli.args=<TestClassName>" for Grails < 2.1.0
        // see https://youtrack.jetbrains.com/issue/IDEA-105206
      }
    }

    if (mappedGoal != null) {
      goal = mappedGoal;

      if (parameterStr != null) {
        ParametersList vmOptionList = new ParametersList();
        vmOptionList.addParametersString(runnerSettings.getVmOptions());
        vmOptionList.addProperty("grails.cli.args", parameterStr);

        runnerSettings.setVmOptions(vmOptionList.getParametersString());
      }
    }
    else if (command.getCommand() != null) {
      goal = "grails:exec";

      runnerSettings.getMavenProperties().put("command", command.getCommand());

      if (parameterStr != null) {
        runnerSettings.getMavenProperties().put("args", parameterStr);
      }
    }

    if (!command.getProperties().isEmpty() || command.getEnv() != null) {
      ParametersList vmOptionList = new ParametersList();

      vmOptionList.addParametersString(runnerSettings.getVmOptions());
      vmOptionList.addAll(command.getProperties());

      if (command.getEnv() != null) {
        vmOptionList.add("-Dgrails.env=" + command.getEnv());
      }

      runnerSettings.setVmOptions(vmOptionList.getParametersString());
    }

    final MavenExplicitProfiles explicitProfiles = projectsManager.getExplicitProfiles();

    Module module = ((GrailsMavenApplication)grailsApplication).getModule();
    String pomFileUrl = MavenPomPathModuleService.getInstance(module).getPomFileUrl();

    final MavenRunnerParameters runnerParameters = new MavenRunnerParameters(
      true,
      grailsApplication.getRoot().getPath(),
      pomFileUrl == null ? null : PathUtil.getFileName(pomFileUrl),
      ContainerUtil.createMaybeSingletonList(goal),
      explicitProfiles.getEnabledProfiles(),
      explicitProfiles.getDisabledProfiles()
    );

    final JavaParameters res = MavenExternalParameters.createJavaParameters(
      project, runnerParameters, generalSettings, runnerSettings, null
    );
    addCommonJvmOptions(res);
    return res;
  }

  @Override
  public @NotNull JavaParameters createJavaParameters(@NotNull GrailsApplication grailsApplication,
                                                      @NotNull MvcCommand command) throws ExecutionException {
    return createJavaParameters(grailsApplication, command, null);
  }

  @Override
  public void addListener(@NotNull JavaParameters params, @NotNull String listener) {
    super.addListener(params, listener);
    GrailsExecutionUtils.addAgentJar(params);
  }

  @Override
  public @NotNull Key<MavenGeneralSettings> getKey() {
    return DATA_KEY;
  }

  @Override
  public @NotNull Pair<@TabTitle String, SettingsEditor<GrailsRunConfiguration>> createSettingsEditor(@NotNull Project project) {
    return new Pair<>(GrailsBundle.message("tab.title.maven.settings"), new GrailsMavenSettingsEditor(project));
  }

  @Override
  public @Nullable MavenGeneralSettings readAdditionalConfiguration(@NotNull Element element) {
    Element e = element.getChild(MavenGeneralSettings.class.getSimpleName());
    return e != null ? XmlSerializer.deserialize(e, MavenGeneralSettings.class) : null;
  }

  @Override
  public void writeAdditionalConfiguration(@NotNull MavenGeneralSettings cfg, @NotNull Element element) {
    element.addContent(XmlSerializer.serialize(cfg));
  }

  public static final class GrailsMavenSettingsEditor extends SettingsEditor<GrailsRunConfiguration> {

    private final MavenGeneralSettingsEditor myDelegate;

    private GrailsMavenSettingsEditor(Project project) {
      myDelegate = new MavenGeneralSettingsEditor(project);
      Disposer.register(this, myDelegate);
    }

    @Override
    protected void resetEditorFrom(@NotNull GrailsRunConfiguration s) {
      myDelegate.resetFrom(getConfiguration(s));
    }

    @Override
    protected void applyEditorTo(@NotNull GrailsRunConfiguration s) throws ConfigurationException {
      final MavenRunConfiguration mavenRunConfiguration = getConfiguration(s);
      myDelegate.applyTo(mavenRunConfiguration);
      s.putUserData(DATA_KEY, mavenRunConfiguration.getGeneralSettings());
    }

    @Override
    protected @NotNull JComponent createEditor() {
      return myDelegate.getComponent();
    }

    private static @NotNull MavenRunConfiguration getConfiguration(GrailsRunConfiguration s) {
      ConfigurationFactory factory = MavenRunConfigurationType.getInstance().getConfigurationFactories()[0];
      MavenRunConfiguration mavenRunConfiguration = (MavenRunConfiguration)factory.createTemplateConfiguration(s.getProject());
      mavenRunConfiguration.setGeneralSettings(s.getUserData(DATA_KEY));
      return mavenRunConfiguration;
    }
  }
}
