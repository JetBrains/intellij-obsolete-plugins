package com.intellij.tcserver.server.run.conf;

import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tcserver.sdk.TcServerUtil;
import com.intellij.tcserver.server.integration.TcServerData;
import com.intellij.tcserver.server.run.TcServerExecutableObjectStartupPolicy;
import com.intellij.tcserver.util.TcServerBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TcServerWrapperConfig {
  @NonNls private final static String JVM_OPTION_STATEMENT = "wrapper.java.additional.";
  @NonNls private final static String SET_VARIABLE_STATEMENT = "set.";
  @NonNls private final static Pattern JVM_OPTION_PATTERN = Pattern.compile("^wrapper\\.java\\.additional\\.(\\d+)=(.*)$");
  @NonNls private final static Pattern FULL_PORT_OPTION_PATTERN =
    Pattern.compile("^wrapper\\.java\\.additional\\.(\\d+)=-Xrunjdwp:.*address=(\\d+).*$");

  private static final TcServerWrapperConfig INSTANCE = new TcServerWrapperConfig();

  public static TcServerWrapperConfig getInstance() {
    return INSTANCE;
  }

  public void checkWrapperConfigSettings(final String settingsConfigPath) throws RuntimeConfigurationException {
    /*
   0. parse Wrapper.config for #include statement
   1. if no include, throw warning with quick fix
      */

    final File config = new File(settingsConfigPath);
    Scanner configScanner;
    try {
      configScanner = new Scanner(config);
    }
    catch (FileNotFoundException e) {
      throw new RuntimeConfigurationError(TcServerBundle.message("debugConfig.configFileNoFound", config.getPath()));
    }

    String includeOption = getIncludeIdeaConfigStatement(config);
    while (configScanner.hasNextLine()) {
      String line = configScanner.nextLine();
      if (includeOption.equals(line)) {
        configScanner.close();
        return;
      }
    }
    configScanner.close();

    //no #include statement found
    String message = TcServerBundle.message("debugConfig.noIncludeStatement", getIdeaConfigFile(config).getPath());
    RuntimeConfigurationWarning warning = new RuntimeConfigurationWarning(message);
    warning.setQuickFix(() -> fixInclude(settingsConfigPath));
    throw warning;
  }

  public void checkWrapperConfigSettings(TcServerData data) throws RuntimeConfigurationException {
    String configPath = getServerConfigPath(data.getSdkPath(), data.getServerName());
    checkWrapperConfigSettings(configPath);
  }

  private void fixInclude(String configPath) {
    try {
      File config = new File(configPath);
      FileUtil.appendToFile(config, TcServerBundle.message("debugConfig.addedByIdea") + "\n" + getIncludeIdeaConfigStatement(config));
    }
    catch (IOException ignore) {
    }
  }

  @Nullable
  public String getProvidedDebugPort(TcServerData data) throws RuntimeConfigurationException {
    String configPath = getServerConfigPath(data.getSdkPath(), data.getServerName());
    File config = new File(configPath);
    File ideaConfig = getIdeaConfigFile(config);
    if (ideaConfig.exists()) {
      return findPort(ideaConfig);
    }
    return null;
  }

  @Nullable
  protected String findPort(File config) throws RuntimeConfigurationError {
    Scanner configScanner;
    try {
      configScanner = new Scanner(config);
    }
    catch (FileNotFoundException e) {
      throw new RuntimeConfigurationError(TcServerBundle.message("debugConfig.configFileNoFound", config.getPath()));
    }

    while (configScanner.hasNextLine()) {
      String line = configScanner.nextLine();
      Matcher matcher = FULL_PORT_OPTION_PATTERN.matcher(line);
      String port = matcher.matches() ? matcher.group(2) : null;
      if (port != null) {
        configScanner.close();
        return port;
      }
    }

    configScanner.close();
    return null;
  }

  @NonNls
  protected String getServerConfigPath(String sdkPath, String instanceName) {
    sdkPath = TcServerUtil.removeTrailingSeparators(sdkPath);
    String catalinaBase = sdkPath + File.separator + instanceName;
    @NonNls String wrapperConfig = catalinaBase + File.separator + "conf" + File.separator + "wrapper.conf";
    return wrapperConfig;
  }

  @NonNls
  protected File getIdeaConfigFile(File configFile) {
    return new File(configFile.getParent() + File.separator + "idea.conf");
  }

  @NonNls
  protected String getIncludeIdeaConfigStatement(File configFile) {
    return "#include " + getIdeaConfigFile(configFile).getPath();
  }

  public void writeJvmOptsAndEnvVars(String sdkPath, String instanceName, String jvmOptions, Map<String, String> envVariables) {
    BufferedWriter writer = null;

    try {
      String myConfigPath = getServerConfigPath(sdkPath, instanceName);
      File configFile = new File(myConfigPath);
      File ideaConfig = getIdeaConfigFile(configFile);

      int nextNumber = getNextJVMOptionNumber(myConfigPath);

      //noinspection IOResourceOpenedButNotSafelyClosed
      writer = new BufferedWriter(new FileWriter(ideaConfig));
      writer.write(TcServerBundle.message("executableObjectStartupPolicy.ideaConfigHeader"));
      for (Map.Entry<String, String> variableEntry : envVariables.entrySet()) {
        String key = variableEntry.getKey();

        if (!StringUtil.isEmpty(key) && !TcServerExecutableObjectStartupPolicy.SERVICE_DEBUG_ENV_VAR.equals(key)) {
          writer.write(SET_VARIABLE_STATEMENT);
          writer.write(key);
          writer.write("=");
          writer.write(variableEntry.getValue());
          writer.write("\n");
        }
        else if (TcServerExecutableObjectStartupPolicy.SERVICE_DEBUG_ENV_VAR.equals(variableEntry.getKey())) {
          writer.write(JVM_OPTION_STATEMENT);
          writer.write(String.valueOf(nextNumber));
          writer.write("=");
          writer.write(variableEntry.getValue());
          writer.write("\n");
        }
      }

      //already included into fixed variable
      /* if (!StringUtil.isEmpty(jvmOptions)) {
        writer.write(JVM_OPTION_STATEMENT);
        writer.write(String.valueOf(nextNumber));
        writer.write("=");
        writer.write(jvmOptions);
        writer.write("\n");
      }*/
    }
    catch (RuntimeConfigurationException | IOException ignore) {
    }
    finally {
      if (writer != null) {
        try {
          writer.close();
        }
        catch (IOException ignore) {
        }
      }
    }
  }

  private static int getNextJVMOptionNumber(String wrapperConfigPath) throws RuntimeConfigurationError {
    final File config = new File(wrapperConfigPath);
    Scanner configScanner;
    try {
      configScanner = new Scanner(config);
    }
    catch (FileNotFoundException e) {
      throw new RuntimeConfigurationError(TcServerBundle.message("debugConfig.configFileNoFound", config.getPath()));
    }

    List<Integer> usedJvmOptionNumbers = new LinkedList<>();
    while (configScanner.hasNextLine()) {
      String line = configScanner.nextLine();
      Matcher matcher = JVM_OPTION_PATTERN.matcher(line);
      if (matcher.matches()) {
        usedJvmOptionNumbers.add(Integer.valueOf(matcher.group(1)));
      }
    }
    configScanner.close();

    Collections.sort(usedJvmOptionNumbers);
    int i = 1;     //option numeration starts with 1
    for (Integer number : usedJvmOptionNumbers) {
      if (i < number) {
        usedJvmOptionNumbers.add(i);
        return i;
      }
      i++;
    }
    usedJvmOptionNumbers.add(i);
    return i;
  }
}
