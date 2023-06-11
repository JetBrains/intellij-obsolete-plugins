package com.intellij.tcserver.server.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.DebuggingRunnerData;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.javaee.appServers.run.configuration.CommonStrategy;
import com.intellij.javaee.appServers.run.localRun.*;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.tcserver.sdk.TcServerUtil;
import com.intellij.tcserver.server.instance.TcServerModelBase;
import com.intellij.tcserver.server.integration.TcServerData;
import com.intellij.tcserver.server.run.conf.TcServerWrapperConfig;
import org.jetbrains.annotations.NonNls;

import java.util.Map;

public class TcServerExecutableObjectStartupPolicy implements ExecutableObjectStartupPolicy {
  public static final String BATCH_DEBUG_ENV_VAR = "CATALINA_OPTS";
  public static final String SERVICE_DEBUG_ENV_VAR = "JVM_OPTS";

  @Override
  @SuppressWarnings("deprecation")
  public ScriptsHelper getStartupHelper() {
    return null;
  }

  @Override
  @SuppressWarnings("deprecation")
  public ScriptsHelper getShutdownHelper() {
    return null;
  }

  @Override
  public ScriptHelper createStartupScriptHelper(ProgramRunner runner) {
    final boolean isDebug = isDebug(runner);
    final boolean isWindows = SystemInfo.isWindows;
    return new ScriptHelper() {
      @Override
      public void checkRunnerSettings(RunConfigurationBase runConfiguration, RunnerSettings runnerSettings) throws RuntimeConfigurationException {
        CommonModel model = (CommonModel)runConfiguration;
        TcServerModelBase.verifyCommonModel(model);
        if (isWindows && isDebug && runnerSettings instanceof DebuggingRunnerData && model.isLocal()) {
          TcServerWrapperConfig.getInstance().checkWrapperConfigSettings(getPersistentData(model));
        }
      }

      @Override
      @SuppressWarnings("RawUseOfParameterizedType")
      public void initRunnerSettings(RunConfigurationBase runConfiguration, RunnerSettings settings) {
        CommonModel model = (CommonModel)runConfiguration;
        try {
          TcServerModelBase.verifyCommonModel(model);
          if (isWindows && isDebug && settings instanceof DebuggingRunnerData) {
            String port = TcServerWrapperConfig.getInstance().getProvidedDebugPort(getPersistentData(model));
            if (port != null) {
              ((DebuggingRunnerData)settings).setDebugPort(port);
            }
          }
        }
        catch (RuntimeConfigurationException ignore) {
        }
      }

      @Override
      public ExecutableObject getDefaultScript(CommonModel commonModel) {
        return createExecutable(commonModel, true);
      }
    };
  }

  @Override
  public ScriptHelper createShutdownScriptHelper(ProgramRunner runner) {
    return new ScriptHelper() {
      @Override
      public ExecutableObject getDefaultScript(CommonModel commonModel) {
        return isBatchMode(commonModel) ? null : createExecutable(commonModel, false);
      }
    };
  }

  @Override
  public EnvironmentHelper getEnvironmentHelper() {
    return new EnvironmentHelper() {
      @NonNls
      @Override
      public String getDefaultJavaVmEnvVariableName(CommonModel model) {
        return isBatchMode(model) || !SystemInfo.isWindows ? BATCH_DEBUG_ENV_VAR : SERVICE_DEBUG_ENV_VAR;
      }
    };
  }

  private static ExecutableObject createExecutable(CommonModel commonModel, final boolean isStarting) {
    try {
      TcServerModelBase.verifyCommonModel(commonModel);

      TcServerData data = getPersistentData(commonModel);

      boolean isBatch = isBatchMode(commonModel);
      final String sdkPath = data.getSdkPath();
      final String instanceName = data.getServerName();
      String[] commandlineParameters = isStarting
                                       ? TcServerUtil.getStartServerCommandline(sdkPath, instanceName, isBatch)
                                       : TcServerUtil.getStopServerServiceCommandline(sdkPath, instanceName);

      return new ColoredCommandLineExecutableObject(commandlineParameters, "") {
        @Override
        public OSProcessHandler createProcessHandler(String workingDirectory, Map<String, String> envVariables)
          throws ExecutionException {
          if (isStarting && SystemInfo.isWindows && !isBatch) {
            TcServerWrapperConfig.getInstance()
              .writeJvmOptsAndEnvVars(sdkPath, instanceName, ((CommonStrategy)commonModel).getSettingsBean().COMMON_VM_ARGUMENTS,
                                      envVariables);
          }
          return super.createProcessHandler(workingDirectory, envVariables);
        }
      };
    }
    catch (RuntimeConfigurationException e) {
      return null;
    }
  }

  private static boolean isDebug(ProgramRunner runner) {
    return DebuggingRunnerData.DEBUGGER_RUNNER_ID.equals(runner.getRunnerId());
  }

  private static boolean isBatchMode(CommonModel commonModel) {
    return ((TcServerModelBase)commonModel.getServerModel()).isBatchMode();
  }

  private static TcServerData getPersistentData(CommonModel commonModel) {
    return (TcServerData)commonModel.getApplicationServer().getPersistentData();
  }
}