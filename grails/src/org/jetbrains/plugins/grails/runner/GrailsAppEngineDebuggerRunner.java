// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaCommandLine;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ReadAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsStructure;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.impl.Grails2Application;

public final class GrailsAppEngineDebuggerRunner extends GenericDebuggerRunner {

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    if (!(profile instanceof GrailsRunConfiguration runConfiguration)) return false;
    if (!executorId.equals(DefaultDebugExecutor.EXECUTOR_ID)) return false;
    GrailsApplication application = runConfiguration.getGrailsApplicationNullable();
    if (!(application instanceof Grails2Application)) return false;
    return ReadAction.compute(() -> {
      GrailsStructure structure = GrailsStructure.getInstance(((Grails2Application)application).getModule());
      return structure != null && structure.isPluginInstalled("app-engine");
    });
  }

  @Override
  public @NotNull String getRunnerId() {
    return getClass().getSimpleName();
  }

  @Override
  protected RunContentDescriptor createContentDescriptor(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    ((JavaCommandLine)state).getJavaParameters().getVMParametersList().add("-Dappengine.debug=true");
    return attachVirtualMachine(state, environment, new RemoteConnection(true, "127.0.0.1", "9999", false), true);
  }
}
