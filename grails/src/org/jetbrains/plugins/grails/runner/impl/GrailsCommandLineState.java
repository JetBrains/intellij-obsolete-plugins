// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner.impl;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.runner.GrailsCommandLineExecutor;
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration;
import org.jetbrains.plugins.grails.runner.GrailsRunConfigurationExtension;
import org.jetbrains.plugins.grails.runner.SetupKt;
import org.jetbrains.plugins.grails.structure.Grails3Application;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;
import org.jetbrains.plugins.groovy.springloaded.SpringLoadedPositionManagerFactory;

import java.util.HashMap;

import static org.jetbrains.plugins.grails.runner.util.GrailsExecutionUtils.GROOVY_PAGE_ADD_LINE_NUMBERS;

public class GrailsCommandLineState extends BaseGrailsCommandLineState {

  private final @NotNull GrailsApplication myApplication;
  private final @NotNull MvcCommand myCommand;
  private final @NotNull GrailsCommandLineExecutor myExecutor;

  public GrailsCommandLineState(@NotNull ExecutionEnvironment environment,
                                @NotNull GrailsRunConfiguration configuration,
                                @NotNull GrailsCommandLineExecutor executor) throws ExecutionException {
    super(environment, configuration);
    myApplication = configuration.getGrailsApplication();
    myCommand = configuration.getGrailsCommand();
    myExecutor = executor;
  }

  public @NotNull GrailsApplication getApplication() {
    return myApplication;
  }

  public @NotNull MvcCommand getCommand() {
    return myCommand;
  }

  public @NotNull GrailsCommandLineExecutor getExecutor() {
    return myExecutor;
  }

  @Override
  protected @NotNull JavaParameters createJavaParameters() throws ExecutionException {
    MvcCommand command = getCommand();
    final JavaParameters parameters = doCreateJavaParameters(command);

    parameters.setWorkingDirectory(VfsUtilCore.virtualToIoFile(getApplication().getRoot()));

    if (parameters.getJdk() == null) {
      final Sdk sdk = ProjectRootManager.getInstance(getEnvironment().getProject()).getProjectSdk();
      parameters.setJdk(sdk);
    }

    parameters.setEnv(new HashMap<>(command.getEnvVariables()));
    parameters.setPassParentEnvs(command.isPassParentEnvs());

    if (DefaultDebugExecutor.getDebugExecutorInstance().equals(getEnvironment().getExecutor())) {
      final Version version = getApplication().getGrailsVersion();
      if (version.isAtLeast(Version.GRAILS_1_3_4) && version.isLessThan(Version.GRAILS_3_0)) {
        if (!parameters.getEnv().containsKey(GROOVY_PAGE_ADD_LINE_NUMBERS)) {
          parameters.getEnv().put(GROOVY_PAGE_ADD_LINE_NUMBERS, "true");
        }
      }

      if (version.compareTo(Version.GRAILS_2_0) >= 0) {
        if (!parameters.getVMParametersList().hasProperty("grails.full.stacktrace")) {
          parameters.getVMParametersList().addProperty("grails.full.stacktrace", "true");
        }
      }
    }

    SetupKt.setupJavaParameters(getConfiguration(), this, parameters);
    return parameters;
  }

  protected @NotNull JavaParameters doCreateJavaParameters(@NotNull MvcCommand command) throws ExecutionException {
    GrailsCommandLineExecutor executor = getExecutor();
    if (executor instanceof GrailsRunConfigurationExtension runConfigurationExtensionExecutor) {
      final Key<?> key = runConfigurationExtensionExecutor.getKey();
      //noinspection unchecked
      return runConfigurationExtensionExecutor.createJavaParameters(
        getApplication(),
        command,
        getConfiguration().getUserData(key)
      );
    }
    else {
      return executor.createJavaParameters(
        getApplication(),
        command
      );
    }
  }

  @Override
  protected @NotNull OSProcessHandler startProcess() throws ExecutionException {
    OSProcessHandler handler = super.startProcess();
    if (getApplication() instanceof Grails3Application) {
      handler.putUserData(SpringLoadedPositionManagerFactory.FORCE_SPRINGLOADED, true);
    }
    return handler;
  }
}
