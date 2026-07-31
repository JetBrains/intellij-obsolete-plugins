package com.intellij.gwt.gradle;

import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetConfiguration;
import com.intellij.gwt.run.GwtRunConfiguration;
import com.intellij.gwt.run.GwtRunConfiguration.GwtRunConfigurationState;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.task.TaskData;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.task.ExecuteRunConfigurationTask;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JpsJavaSdkType;
import org.jetbrains.plugins.gradle.execution.build.CachedModuleDataFinder;
import org.jetbrains.plugins.gradle.execution.build.GradleExecutionEnvironmentProvider;
import org.jetbrains.plugins.gradle.util.GradleModuleData;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intellij.execution.executors.DefaultRunExecutor.EXECUTOR_ID;
import static com.intellij.openapi.util.text.StringUtil.escapeStringCharacters;
import static com.intellij.openapi.util.text.StringUtil.join;
import static com.intellij.openapi.util.text.StringUtil.substringAfter;
import static com.intellij.openapi.util.text.StringUtil.trimEnd;
import static org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.getGradleIdentityPathOrNull;
import static org.jetbrains.plugins.gradle.service.task.GradleTaskManager.INIT_SCRIPT_KEY;
import static org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID;

public final class GwtGradleExecutionEnvironmentProvider implements GradleExecutionEnvironmentProvider {

  @Override
  public boolean isApplicable(ExecuteRunConfigurationTask task) {
    return task.getRunProfile() instanceof GwtRunConfiguration;
  }

  @Override
  public ExecutionEnvironment createExecutionEnvironment(Project project, ExecuteRunConfigurationTask task, Executor executor) {
    if (!isApplicable(task)) return null;

    GwtRunConfiguration gwtRunConfiguration = (GwtRunConfiguration)task.getRunProfile();
    Module module = gwtRunConfiguration.getModule();
    if (module == null) return null;

    GwtFacet gwtFacet = GwtFacet.getInstance(module);
    if (gwtFacet == null) return null;

    String gradleProjectPath = ExternalSystemApiUtil.getExternalRootProjectPath(module);
    if (gradleProjectPath == null) return null;
    String gradlePath = getGradleIdentityPathOrNull(module);
    if (gradlePath == null) return null;

    GradleModuleData gradleModuleData = CachedModuleDataFinder.getGradleModuleData(module);
    if (gradleModuleData == null) return null;

    Collection<TaskData> tasks = gradleModuleData.findAll(ProjectKeys.TASK);
    if (tasks.isEmpty()) return null;

    String gradleIdentityPath = gradleModuleData.getGradleIdentityPathOrNull();
    if (gradleIdentityPath == null) return null;
    String taskPathPrefix = trimEnd(gradleIdentityPath, ":") + ":";

    Set<String> names = tasks.stream().map(data -> substringAfter(data.getName(), taskPathPrefix)).collect(Collectors.toSet());
    if (!names.contains("gwtDev") || !names.contains("gwtSuperDev") || !names.contains("warTemplate")) {
      return null;
    }

    ExternalSystemTaskExecutionSettings taskSettings = new ExternalSystemTaskExecutionSettings();
    taskSettings.setExternalSystemIdString(SYSTEM_ID.getId());
    taskSettings.setExternalProjectPath(gradleModuleData.getDirectoryToRunTask());

    GwtRunConfigurationState gwtRunConfigurationState = gwtRunConfiguration.getGwtState();
    String taskPath = gradleModuleData.getTaskPathOfSimpleTaskName(gwtRunConfigurationState.USE_SUPER_DEV_MODE ? "gwtDev" : "gwtSuperDev");
    taskSettings.setTaskNames(Collections.singletonList(taskPath));

    String executorId = executor == null ? EXECUTOR_ID : executor.getId();
    ExecutionEnvironment environment = ExternalSystemUtil.createExecutionEnvironment(project, SYSTEM_ID, taskSettings, executorId);
    if (environment == null) return null;

    RunnerAndConfigurationSettings runnerAndConfigurationSettings = environment.getRunnerAndConfigurationSettings();
    if (runnerAndConfigurationSettings == null) return null;

    ExternalSystemRunConfiguration runnerSettings = (ExternalSystemRunConfiguration)runnerAndConfigurationSettings.getConfiguration();
    Map<@NonNls String, @NonNls String> gwtParameters         = new HashMap<>();
    Map<@NonNls String, @NonNls String> gwtCompilerParameters = new HashMap<>();
    Map<@NonNls String, @NonNls String> gwtDevParameters      = new HashMap<>();
    Map<@NonNls String, @NonNls String> gwtSuperDevParameters = new HashMap<>();
    Map<@NonNls String, @NonNls String> warTemplateParameters = new HashMap<>();

    List<String> modules = gwtRunConfigurationState.getGwtModules();
    if (modules != null) {
      StringBuilder result = new StringBuilder("'");
      join(modules, "', '", result);
      result.append("'");

      gwtParameters.put("modules", result.toString());
    }

    GwtFacetConfiguration gwtFacetConfiguration = gwtFacet.getConfiguration();
    gwtParameters.put("maxHeapSize", escapeString(gwtFacetConfiguration.getCompilerMaxHeapSize() + "M"));
    gwtParameters.put("minHeapSize", null);

    gwtCompilerParameters.put("style", escapeString(gwtFacetConfiguration.getOutputStyle().getId()));
    gwtParameters.put("sourceLevel", escapeString(JpsJavaSdkType.complianceOption(gwtFacetConfiguration.getClientLanguageLevel().toJavaVersion())));

    String gwtRunConfigurationProgramParameters = gwtRunConfiguration.getProgramParameters();
    if (gwtRunConfigurationProgramParameters != null) {
      Iterator<String> iterator = ParametersListUtil.parse(gwtRunConfigurationProgramParameters).iterator();
      while (iterator.hasNext()) {
        @NonNls String arg = iterator.next();
        switch (arg) {
          case "-war" -> {
            String destinationDir = nextValue(iterator);
            if (destinationDir != null) {
              String value = "file(" + escapeString(destinationDir) + ")";
              warTemplateParameters.put("destinationDir", value);
              gwtParameters.put("devWar", value);
            }
          }
          case "-workDir" -> {
            String workDir = nextValue(iterator);
            if (workDir != null) {
              gwtParameters.put("workDir", "file(" + escapeString(workDir) + ")");
            }
          }
          case "-gen" -> {
            String genDir = nextValue(iterator);
            if (genDir != null) {
              gwtParameters.put("genDir", "file(" + escapeString(genDir) + ")");
            }
          }
          case "-extra" -> {
            String extraDir = nextValue(iterator);
            if (extraDir != null) {
              gwtParameters.put("extraDir", "file(" + escapeString(extraDir) + ")");
            }
          }
          case "-incremental" -> gwtParameters.put("incremental", "true");
          case "-XjsInteropMode" -> {
            String jsInteropMode = nextValue(iterator);
            if (jsInteropMode != null) {
              gwtParameters.put("jsInteropMode", escapeString(jsInteropMode));
            }
          }
          case "-logLevel" -> {
            String logLevel = nextValue(iterator);
            if (logLevel != null) {
              gwtParameters.put("logLevel", escapeString(logLevel));
            }
          }
          case "-sourceLevel" -> {
            String sourceLevel = nextValue(iterator);
            if (sourceLevel != null) {
              gwtParameters.put("sourceLevel", escapeString(sourceLevel));
            }
          }
          case "-noserver", "-nostartServer" -> gwtDevParameters.put("noserver", "true");
        }
      }
    }

    gwtDevParameters.put("superDevMode", Boolean.toString(gwtRunConfigurationState.USE_SUPER_DEV_MODE));

    String gwtParametersString         = mapToString(gwtParameters);
    String gwtCompilerParametersString = mapToString(gwtCompilerParameters);
    String warTemplateParametersString = mapToString(warTemplateParameters);
    String gwtDevParametersString      = mapToString(gwtDevParameters);
    String gwtSuperDevParametersString = mapToString(gwtSuperDevParameters);

    @NonNls String initScript =
      "allprojects {\n" +
      "  afterEvaluate { project ->\n" +
      "    if (project.path == '" + gradlePath + "' && project.extensions.findByName('gwt') != null) {\n" +
      "      project.gwt {\n" + gwtParametersString + "}\n" +
      "      if (project.hasProperty('compileGwt')) {\n" +
      "        project.compileGwt {\n" + gwtCompilerParametersString + "}\n" +
      "      }\n" +
      "      if (project.hasProperty('gwtDev')) {\n" +
      "        project.gwtDev {\n" + gwtDevParametersString + "}\n" +
      "      }\n" +
      "      if (project.hasProperty('gwtSuperDev')) {\n" +
      "        project.gwtSuperDev {\n" + gwtSuperDevParametersString + "}\n" +
      "      }\n" +
      "      if (project.hasProperty('warTemplate')) {\n" +
      "        project.warTemplate {\n" + warTemplateParametersString + "}\n" +
      "      }\n" +
      "    }\n" +
      "  }\n" +
      "}\n";
    runnerSettings.putUserData(INIT_SCRIPT_KEY, initScript);

    return environment;
  }

  private static @NotNull String mapToString(Map<String, String> gwtCompilerParameters) {
    StringBuilder gwtCompilerParametersString = new StringBuilder();
    for (Map.Entry<String, String> entry : gwtCompilerParameters.entrySet()) {
      gwtCompilerParametersString.append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n');
    }
    return gwtCompilerParametersString.toString();
  }

  private static @Nullable String nextValue(@NotNull Iterator<String> iterator) {
    return iterator.hasNext() ? iterator.next() : null;
  }

  private static @NotNull String escapeString(@NotNull String string) {
    return "'" + escapeStringCharacters(string) + "'";
  }
}
