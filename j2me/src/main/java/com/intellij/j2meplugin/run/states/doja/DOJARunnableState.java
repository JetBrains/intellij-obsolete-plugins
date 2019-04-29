/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.run.states.doja;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.DebuggingRunnerData;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.j2meplugin.emulator.Emulator;
import com.intellij.j2meplugin.emulator.EmulatorType;
import com.intellij.j2meplugin.run.J2MERunConfiguration;
import com.intellij.j2meplugin.run.J2MERunnableState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;

import java.io.File;

public class DOJARunnableState extends J2MERunnableState {
  public DOJARunnableState(RunnerSettings runnerSettings, J2MERunConfiguration configuration, Project project, Sdk projectJdk) {
    super(runnerSettings, configuration, project, projectJdk);
  }

  @Override
  protected ProcessHandler getExecutionProcess(String availablePort) throws ExecutionException {
    final Emulator emulator = (Emulator)myProjectJdk.getSdkAdditionalData();
    final EmulatorType emulatorType = emulator.getEmulatorType();
    LOG.assertTrue(emulatorType != null);
    GeneralCommandLine generalCommandLine = new GeneralCommandLine();
    generalCommandLine.setExePath(emulatorType.getPathToEmulator(myProjectJdk.getHomePath()));
    //final DeviceSpecificOption descriptor = emulator.getEmulatorType().getDeviceSpecificOptions().get(EmulatorType.DESCRIPTOR);



    if (myRunnerSettings instanceof DebuggingRunnerData) {
      generalCommandLine.addParameter("-debugger");
      generalCommandLine.addParameter("-suspend"); //todo -nosuspend
      generalCommandLine.addParameter("-port");
      generalCommandLine.addParameter(findFreePort());
      generalCommandLine.addParameter("-debugport");
      generalCommandLine.addParameter(availablePort);
      generalCommandLine.addParameter("-jdkpath");
      generalCommandLine.addParameter(emulator.getJavaSdk().getHomePath());
    }


    generalCommandLine.addParameter(emulatorType.getDescriptorOption());
    if (!myConfiguration.IS_CLASSES) {
      generalCommandLine.addParameter(myConfiguration.JAD_NAME);
    }
    else {
      final Module module = myConfiguration.getModule();
      final File tempJam = findFilesToDelete(module);
      generalCommandLine.addParameter(tempJam.getPath());
    }
    if (myConfiguration.TARGET_DEVICE_NAME != null && myConfiguration.TARGET_DEVICE_NAME.length() != 0) {
      generalCommandLine.addParameter(emulatorType.getDeviceOption());
      generalCommandLine.addParameter(myConfiguration.TARGET_DEVICE_NAME);
    }

    if (myConfiguration.COMMAND_LINE_PARAMETERS != null) {
      String[] params = myConfiguration.COMMAND_LINE_PARAMETERS.split(" ");
      for (int i = 0; params != null && i < params.length; i++) {
        generalCommandLine.addParameter(params[i]);
      }
    }
    return new OSProcessHandler(generalCommandLine);

  }

  private String findFreePort(){
    try {
      return DebuggerUtils.getInstance().findAvailableDebugAddress(true);
    }
    catch (ExecutionException e) {
      LOG.error(e);
    }
    return null;
  }

}
