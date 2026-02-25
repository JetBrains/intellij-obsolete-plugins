// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.gradle;

import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.gradle.util.GradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.groovy.grails.rt.GrailsRtMarker;
import org.jetbrains.plugins.gradle.service.task.GradleTaskManagerExtension;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;

import java.util.Arrays;
import java.util.stream.Collectors;

public class GrailsTaskManagerExtension implements GradleTaskManagerExtension {

  private static final String GRAILS_TASK_PREFIX = "grails-";
  private static final String GRAILS_TEST_LOGGER_SCRIPT_NAME = "ijGrailsTestLoggerInit";
  private static final String GRAILS_JVM_OPTIONS_SCRIPT_NAME = "ijGrailsJvmOptionsInit";

  @Override
  public void configureTasks(@NotNull String projectPath,
                             @NotNull ExternalSystemTaskId id,
                             @NotNull GradleExecutionSettings settings,
                             @Nullable GradleVersion gradleVersion) {
    final String grailsTask = ContainerUtil.find(settings.getTasks(),
                                                 taskName -> taskName != null && StringUtil.startsWith(taskName, GRAILS_TASK_PREFIX));

    if (grailsTask == null) return;

    configureTestLogger(settings);
    configureJvmOptions(settings);
  }

  private static void configureTestLogger(@NotNull GradleExecutionSettings settings) {
    if (settings.getTasks().contains("grails-test-app")) {
      String grailsRtPath = FileUtil.toCanonicalPath(PathUtil.getJarPathForClass(GrailsRtMarker.class));
      final String[] lines = {
        //"allprojects { project ->",
        //"    def grailsPlugin = project.plugins.findPlugin('grails')",
        //"    if (grailsPlugin != null) {",
        //"       dependencies {",
        //"           runtime files(\"" + grailsRtPath + "\")",
        //"       }" +
        //"    }" +
        //"}",
        "gradle.taskGraph.whenReady { taskGraph ->",
        "  taskGraph.allTasks.each { Task task ->",
        "    if (task.hasProperty('group') && 'grails'.equals(task?.group)) {",
        "       if (task.hasProperty('command') && task?.command.equals('test-app')) {",
        "           task.runtimeClasspath += files(\"" + grailsRtPath + "\")",
        "           task.jvmOptions.jvmArgs('-Dgrails.build.listeners=org.jetbrains.groovy.grails.rt.GrailsIdeaTestListener')",
        "       }" +
        "    }" +
        "  }" +
        "}"
      };
      final String script = StringUtil.join(lines, System.lineSeparator());
      settings.addInitScript(GRAILS_TEST_LOGGER_SCRIPT_NAME, script);
    }
  }

  private static void configureJvmOptions(@NotNull GradleExecutionSettings settings) {
    String jvmParametersSetup = settings.getJvmParameters();
    if (!StringUtil.isEmpty(jvmParametersSetup)) {
      final String jvmArgs = Arrays.stream(ParametersListUtil.parseToArray(jvmParametersSetup))
        .map(s -> '\'' + s.trim().replace("\\", "\\\\") + '\'').collect(Collectors.joining(" , "));
      final String[] lines = {
        "gradle.taskGraph.whenReady { taskGraph ->",
        "  taskGraph.allTasks.each { Task task ->",
        "    if (task.hasProperty('group') && 'grails'.equals(task?.group)) {",
        "        if (task.hasProperty('jvmOptions') && task?.jvmOptions instanceof JavaForkOptions) {",
        "            task.jvmOptions.jvmArgs = [" + jvmArgs + " , *task.jvmOptions.jvmArgs]",
        "       }" +
        "    }" +
        "  }" +
        "}"
      };
      final String script = StringUtil.join(lines, System.lineSeparator());
      settings.addInitScript(GRAILS_JVM_OPTIONS_SCRIPT_NAME, script);
    }
  }
}
