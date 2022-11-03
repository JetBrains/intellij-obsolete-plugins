package com.intellij.dmserver.run;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.install.ServerVersionHandler;
import com.intellij.dmserver.integration.DMServerIntegrationData;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.javaee.appServers.appServerIntegrations.ApplicationServer;
import com.intellij.javaee.appServers.run.localRun.*;
import com.intellij.javaee.appServers.run.configuration.CommonModel;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public class DMServerStartupPolicy implements ExecutableObjectStartupPolicy {

  private static final Logger LOG = Logger.getInstance(DMServerStartupPolicy.class);

  @NonNls
  public static final String STARTUP_SCRIPT = "startup";

  @NonNls
  private static final String SHUTDOWN_SCRIPT = "shutdown";

  @NonNls
  public static final String DMK_SCRIPT = "dmk";

  @NonNls
  private static final String JMX_OPTS_ENV_VAR = "JMX_OPTS";

  private static final String SH_LINE_CONCAT = "\\";

  private static final String SH_QUOTE = "\"";

  @NonNls
  private static final String BAT_SET = "set";

  private static final boolean isWindows = SystemInfo.isWindows;

  @NonNls
  private static final String DEBUG_ENV_VAR = "DEBUG_OPTS";

  @NonNls
  private static String getEnvVarRef(String envVar) {
    return isWindows ? "%" + envVar + "%" : "$" + envVar;
  }

  @Override
  @Deprecated
  @Nullable
  public ScriptsHelper getStartupHelper() {
    return null;
  }

  @Override
  @Deprecated
  @Nullable
  public ScriptsHelper getShutdownHelper() {
    return null;
  }

  private static DMServerIntegrationData getPersistentData(CommonModel commonModel) {
    ApplicationServer applicationServer = commonModel.getApplicationServer();
    if (applicationServer == null) {
      return null;
    }
    return (DMServerIntegrationData)applicationServer.getPersistentData();
  }

  private static ServerVersionHandler getVersionHandler(CommonModel commonModel) {
    DMServerIntegrationData persistentData = getPersistentData(commonModel);
    if (persistentData == null) {
      return null;
    }
    DMServerInstallation installation = persistentData.getInstallation();
    if (installation == null || !installation.isValid()) {
      return null;
    }
    return installation.getServerVersion();
  }

  @Nullable
  @Override
  public ScriptHelper createStartupScriptHelper(ProgramRunner runner) {
    return new ScriptHelperImpl(STARTUP_SCRIPT) {

      @Override
      public void checkRunnerSettings(RunConfigurationBase runConfiguration, RunnerSettings runnerSettings) throws RuntimeConfigurationException {
        if (runConfiguration instanceof CommonModel) {
          CommonModel commonModel = (CommonModel)runConfiguration;
          ServerVersionHandler versionHandler = getVersionHandler(commonModel);
          if (versionHandler == null) {
            return;
          }
          String scriptName = versionHandler.getJmxScriptName();
          final File script = getScript(commonModel, scriptName);
          if (script == null) {
            return;
          }
          checkJmxScript(script, versionHandler.getJmxPortEnvVar());
        }
      }
    };
  }

  @Nullable
  @Override
  public ScriptHelper createShutdownScriptHelper(ProgramRunner runner) {
    return new ScriptHelperImpl(SHUTDOWN_SCRIPT);
  }

  private static void checkJmxScript(final File script, String jmxPortEnvVar) throws RuntimeConfigurationError {
    try {
      String scriptContent = FileUtil.loadFile(script);
      final List<String> scriptLines = StringUtil.split(scriptContent, "\n", true, false);

      final List<String> fixedLines = new ArrayList<>();

      boolean hasWrongArgs = false;

      Set<String> requiredArgs = new LinkedHashSet<>();
      requiredArgs.add("-Dcom.sun.management.jmxremote=");
      requiredArgs.add("-Dcom.sun.management.jmxremote.port=" + getEnvVarRef(jmxPortEnvVar));
      requiredArgs.add("-Dcom.sun.management.jmxremote.ssl=false");
      requiredArgs.add("-Dcom.sun.management.jmxremote.authenticate=false");

      final String comment = isWindows ? "rem" : "#";

      final String jmxOptsRef = getEnvVarRef(JMX_OPTS_ENV_VAR);
      final String jmxOptsSet = JMX_OPTS_ENV_VAR + "=";

      Iterator<String> itLine = scriptLines.iterator();

      int insertLineIndex = 0;
      int lineIndex = 0;

      while (itLine.hasNext()) {
        lineIndex++;
        String line = itLine.next();
        String trimmedLine = line.trim();

        boolean setCandidate = true;
        if (isWindows) {
          if (StringUtil.startsWithIgnoreCase(trimmedLine, BAT_SET)) {
            trimmedLine = trimmedLine.substring(BAT_SET.length()).trim();
          }
          else {
            setCandidate = false;
          }
        }

        if (setCandidate && trimmedLine.startsWith(jmxOptsSet)) {
          List<String> partLines = new ArrayList<>();
          partLines.add(line);

          trimmedLine = StringUtil.trimStart(trimmedLine, jmxOptsSet).trim();

          String part;
          if (isWindows) {
            part = trimmedLine;
          }
          else {
            StringBuilder concatenatedLine = new StringBuilder();
            while (trimmedLine.endsWith(SH_LINE_CONCAT)) {
              concatenatedLine.append(StringUtil.trimEnd(trimmedLine, SH_LINE_CONCAT));
              lineIndex++;
              line = itLine.next();
              trimmedLine = line.trim();
              partLines.add(line);
            }
            concatenatedLine.append(trimmedLine);
            part = concatenatedLine.toString();

            if (part.startsWith(SH_QUOTE)) {
              part = StringUtil.trimEnd(StringUtil.trimStart(part, SH_QUOTE), SH_QUOTE).trim();
            }
          }

          if (insertLineIndex == 0) {
            insertLineIndex = lineIndex;
          }

          part = StringUtil.trimStart(part, jmxOptsRef).trim();
          String[] args = part.split("\\s+");
          List<String> partRequiredArgs = new ArrayList<>();
          boolean partHasWrongArgs = false;
          for (String arg : args) {
            if (requiredArgs.contains(arg)) {
              partRequiredArgs.add(arg);
            }
            else {
              partHasWrongArgs = true;
              hasWrongArgs = true;
            }
          }

          for (String partLine : partLines) {
            if (partHasWrongArgs) {
              fixedLines.add(comment + " " + partLine);
            }
            else {
              fixedLines.add(partLine);
            }
          }

          if (!partHasWrongArgs) {
            requiredArgs.removeAll(partRequiredArgs);
          }
        }
        else {
          fixedLines.add(line);
        }
      }

      if (!requiredArgs.isEmpty()) {
        for (String requiredArg : requiredArgs) {
          String value = jmxOptsRef + " " + requiredArg;
          if (!isWindows) {
            value = SH_QUOTE + value + SH_QUOTE;
          }
          String line = jmxOptsSet + value;
          if (isWindows) {
            line = BAT_SET + " " + line;
          }
          fixedLines.add(insertLineIndex, line);
        }
      }

      if (hasWrongArgs || !requiredArgs.isEmpty()) {
        throw new RuntimeConfigurationError(DmServerBundle.message("DMServerStartupPolicy.jmx.arguments.are.incompatible"), () -> {
          try {
            final String fixedScriptContent = StringUtil.join(ArrayUtilRt.toStringArray(fixedLines), "\n");
            FileUtil.writeToFile(script, fixedScriptContent.getBytes(Charset.defaultCharset()));
          }
          catch (IOException e) {
            LOG.warn(e);
          }
        });
      }
    }
    catch (IOException e) {
      LOG.debug(e);
    }
  }

  @Nullable
  @Override
  public EnvironmentHelper getEnvironmentHelper() {
    return new EnvironmentHelper() {

      @Override
      public String getDefaultJavaVmEnvVariableName(CommonModel model) {
        return DEBUG_ENV_VAR;
      }

      @Override
      public List<EnvironmentVariable> getAdditionalEnvironmentVariables(CommonModel model) {
        List<EnvironmentVariable> result = new ArrayList<>();
        ServerVersionHandler versionHandler = getVersionHandler(model);
        if (versionHandler != null) {
          DMServerModelBase dmServerModel = (DMServerModelBase)model.getServerModel();
          result.add(new EnvironmentVariable(versionHandler.getJmxPortEnvVar(), String.valueOf(dmServerModel.getMBeanServerPort()), true));
        }
        return result;
      }
    };
  }

  @Nullable
  private static File getScript(CommonModel commonModel, String scriptName) {
    DMServerIntegrationData data = getPersistentData(commonModel);
    if (data == null) {
      return null;
    }
    File scriptDir = new File(data.getInstallationHome(), "bin");
    return new File(scriptDir, scriptName + (isWindows ? ".bat" : ".sh"));
  }

  private static class ScriptHelperImpl extends ScriptHelper {

    private final String myScriptName;

    ScriptHelperImpl(String scriptName) {
      myScriptName = scriptName;
    }

    @Nullable
    @Override
    public ExecutableObject getDefaultScript(CommonModel commonModel) {
      File script = getScript(commonModel, myScriptName);
      if (script == null) {
        return null;
      }
      return new ColoredCommandLineExecutableObject(new String[]{script.getAbsolutePath()}, null);
    }
  }
}
