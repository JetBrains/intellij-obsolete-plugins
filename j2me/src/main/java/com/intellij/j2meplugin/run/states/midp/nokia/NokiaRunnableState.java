/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.run.states.midp.nokia;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.DebuggingRunnerData;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.j2meplugin.emulator.Emulator;
import com.intellij.j2meplugin.emulator.midp.nokia.ConfigurationUtil;
import com.intellij.j2meplugin.emulator.midp.nokia.NokiaEmulatorType;
import com.intellij.j2meplugin.run.J2MERunConfiguration;
import com.intellij.j2meplugin.run.J2MERunnableState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NonNls;

import java.io.File;

public class NokiaRunnableState extends J2MERunnableState {
  public NokiaRunnableState(RunnerSettings runnerSettings, J2MERunConfiguration configuration, Project project, Sdk projectJdk) {
    super(runnerSettings, configuration, project, projectJdk);
  }

  @Override
  protected ProcessHandler getExecutionProcess(String availablePort) throws ExecutionException {
    final Emulator emulator = (Emulator)myProjectJdk.getSdkAdditionalData();
    JavaParameters javaParameters = new JavaParameters();
    javaParameters.setJdk(emulator.getJavaSdk());
    @NonNls final String key = "kvem.main";
    javaParameters.setMainClass(ConfigurationUtil.getProperties(myProjectJdk.getHomePath()).getProperty(key));
    javaParameters.getClassPath().add(NokiaEmulatorType.getKvemPath(myProjectJdk.getHomeDirectory().getPath()));
    final String[] urls = myProjectJdk.getRootProvider().getUrls(OrderRootType.CLASSES);
    for (int k = 0; urls != null && k < urls.length; k++) {
      javaParameters.getClassPath().add(PathUtil.toPresentableUrl(urls[k]));
    }
    javaParameters.getVMParametersList().add("-Demulator.home=" + myProjectJdk.getHomeDirectory().getPath());
    ParametersList params = javaParameters.getProgramParametersList();
    if (myRunnerSettings instanceof DebuggingRunnerData) {
      params.add("-debugger");
      params.add("-dbg_port");
      params.add(availablePort);
    }
    if (!myConfiguration.IS_CLASSES) {
      params.add(myConfiguration.JAD_NAME);
    }
    else {
      final File tempJad = findFilesToDelete(myConfiguration.getModule());
      params.add(tempJad.getPath());
    }
    if (myConfiguration.COMMAND_LINE_PARAMETERS != null && myConfiguration.COMMAND_LINE_PARAMETERS.length() > 0) {
      params.add(myConfiguration.COMMAND_LINE_PARAMETERS);
    }
    return new OSProcessHandler(javaParameters.toCommandLine());
  }
}
