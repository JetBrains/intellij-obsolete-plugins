package com.intellij.tcserver.server.integration;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileFilters;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tcserver.sdk.TcServerUtil;
import com.intellij.tcserver.util.TcServerBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum TcServerVersion {
  BEFORE_2_1 {
    @NonNls private final Pattern JMX_PORT_DEFINITION = Pattern.compile("^jmx\\.port=(\\d+)$");
    @NonNls private final Pattern HTTP_PORT_DEFINITION = Pattern.compile("^http\\.port=(\\d+)$");
    @NonNls private final Pattern MY_SUCCESSFUL_RUNTIME_INSTANCE_SCRIPT_PATTERN_BEFORE_2_1 = Pattern.compile(".*Done.\\s*$");

    @Override
    public List<String> getServers(String sdkPath) throws ExecutionException, RuntimeConfigurationException {
      sdkPath = TcServerUtil.validateSdkPath(sdkPath);
      @NonNls StringBuilder command = new StringBuilder();
      command.append(TcServerUtil.getEscapeQuote()).append(sdkPath).append(File.separator).append(TcServerUtil.TC_RUNTIME_INSTANCE_SCRIPT)
        .append(
          TcServerUtil.getScriptExtension())
        .append(TcServerUtil.getEscapeQuote());
      command.append(" -l");
      // -n and -d options to set proper path for the script
      command.append(" -n ").append(TcServerUtil.getEscapeQuote()).append(sdkPath).append(TcServerUtil.getEscapeQuote());
      command.append(" -d ").append(TcServerUtil.getEscapeQuote()).append(sdkPath).append(TcServerUtil.getEscapeQuote());


      String execCommand = command.toString();
      MY_LOG.debug("getServers command " + execCommand);

      @NonNls final String beginning = " Instance: ";
      List<String> result = new LinkedList<>();
      List<String> scriptLog = TcServerUtil.runCommandWithByLineResult(execCommand, sdkPath,
                                                                       TcServerBundle.message("tcServerUtil.failedToGetAvailableServers"));
      for (String line : scriptLog) {
        if (line.startsWith(beginning)) {
          result.add(line.substring(beginning.length()).trim());
        }
      }
      String solidScriptLog = StringUtil.join(scriptLog, "\n");
      if (!MY_SUCCESSFUL_RUNTIME_INSTANCE_SCRIPT_PATTERN_BEFORE_2_1.matcher(solidScriptLog).find()) {
        MY_LOG.warn(execCommand + "\n" + scriptLog);
        throw new ExecutionException(solidScriptLog);
      }
      return result;
    }

    @Override
    public Pattern getJmxPortPattern() {
      return JMX_PORT_DEFINITION;
    }

    @Override
    public Pattern getHttpPortPattern() {
      return HTTP_PORT_DEFINITION;
    }

    @Override
    public void createServer(String sdkPath, String serverName, String pathToTemplate, boolean forceCreation)
      throws ExecutionException, RuntimeConfigurationException {
      sdkPath = TcServerUtil.validateSdkPath(sdkPath);
      TcServerUtil.validateServerName(serverName);

      @NonNls StringBuilder command = new StringBuilder();

      command.append(TcServerUtil.getEscapeQuote()).append(sdkPath).append(File.separator).append(TcServerUtil.TC_RUNTIME_INSTANCE_SCRIPT)
        .append(
          TcServerUtil.getScriptExtension())
        .append(TcServerUtil.getEscapeQuote());
      command.append(" -c -s ").append(serverName);
      if (!StringUtil.isEmpty(pathToTemplate)) {
        pathToTemplate = TcServerUtil.validatePath(pathToTemplate);
        command.append(" -t ").append(TcServerUtil.getEscapeQuote()).append(pathToTemplate).append(TcServerUtil.getEscapeQuote());
      }

      if (forceCreation) {
        command.append(" -f");
      }

      // -n and -d options to set proper path for the script
      command.append(" -n ").append(TcServerUtil.getEscapeQuote()).append(sdkPath).append(TcServerUtil.getEscapeQuote());
      command.append(" -d ").append(TcServerUtil.getEscapeQuote()).append(sdkPath).append(TcServerUtil.getEscapeQuote());

      String execCommand = command.toString();
      MY_LOG.debug("createServer command " + execCommand);


      @NlsSafe final String scriptLog = TcServerUtil.runCommandWithSolidResult(execCommand, sdkPath,
                                                                               TcServerBundle.message("tcServerUtil.failedToCreateServer"));
      if (!MY_SUCCESSFUL_RUNTIME_INSTANCE_SCRIPT_PATTERN_BEFORE_2_1.matcher(scriptLog).find()) {
        MY_LOG.warn(execCommand + "\n" + scriptLog);
        throw new ExecutionException(scriptLog);
      }
      else {
        MY_LOG.debug(scriptLog);
        if (SystemInfo.isWindows) {
          installWindowsServerService(sdkPath, serverName);
        }
      }
    }

    @Override
    public List<String> listTemplates(String sdkPath) throws ExecutionException {
      throw new UnsupportedOperationException();
    }
  }, EQUAL_OR_AFTER_2_1 {
    @NonNls private final Pattern JMX_PORT_DEFINITION = Pattern.compile("^.*\\.jmx\\.port=(\\d+)$");
    @NonNls private final Pattern HTTP_PORT_DEFINITION = Pattern.compile("^.*\\.http\\.port=(\\d+)$");
    @NonNls private final Pattern MY_SUCCESSFUL_RUNTIME_INSTANCE_SCRIPT_PATTERN_AFTER_2_1 = Pattern.compile("Instance created");

    @Override
    public List<String> getServers(String sdkPath) throws ExecutionException, RuntimeConfigurationException {
      sdkPath = TcServerUtil.validateSdkPath(sdkPath);

      @NonNls StringBuilder command = new StringBuilder();
      command.append(TcServerUtil.getEscapeQuote()).append(sdkPath).append(File.separator).append(TcServerUtil.TC_RUNTIME_INSTANCE_SCRIPT)
        .append(
          TcServerUtil.getScriptExtension())
        .append(TcServerUtil.getEscapeQuote());
      command.append(" list");
      // -i option to set proper directory for examining
      command.append(" -i ").append(TcServerUtil.getEscapeQuote()).append(sdkPath).append(TcServerUtil.getEscapeQuote());

      String execCommand = command.toString();
      MY_LOG.debug("getServers command " + execCommand);

      @NonNls final String end21 = " Status:";
      @NonNls final String end70 = " Info:";
      List<String> result = new LinkedList<>();
      List<String> scriptLog = TcServerUtil.runCommandWithByLineResult(execCommand, sdkPath,
                                                                       TcServerBundle.message("tcServerUtil.failedToGetAvailableServers"));
      for (String line : scriptLog) {
        MY_LOG.debug("script output line   " + line);
        if (line.endsWith(end21)) {
          result.add(line.substring(0, line.length() - end21.length()).trim());
        }
        else if (line.endsWith(end70)) {
          result.add(line.substring(0, line.length() - end70.length()).trim());
        }
      }

      return result;
    }

    @Override
    public Pattern getJmxPortPattern() {
      return JMX_PORT_DEFINITION;
    }

    @Override
    public Pattern getHttpPortPattern() {
      return HTTP_PORT_DEFINITION;
    }

    @Override
    public void createServer(String sdkPath, String serverName, String pathToTemplate, boolean forceCreation)
      throws ExecutionException, RuntimeConfigurationException {
      sdkPath = TcServerUtil.validateSdkPath(sdkPath);
      TcServerUtil.validateServerName(serverName);

      @NonNls StringBuilder command = new StringBuilder();

      command.append(TcServerUtil.getEscapeQuote()).append(sdkPath).append(File.separator).append(TcServerUtil.TC_RUNTIME_INSTANCE_SCRIPT)
        .append(
          TcServerUtil.getScriptExtension())
        .append(TcServerUtil.getEscapeQuote());
      command.append(" create ").append(serverName);
      if (!StringUtil.isEmpty(pathToTemplate)) {
        pathToTemplate = TcServerUtil.validatePath(pathToTemplate);
        command.append(" -t ").append(TcServerUtil.getEscapeQuote()).append(pathToTemplate).append(TcServerUtil.getEscapeQuote());
      }

      if (forceCreation) {
        command.append(" --force");
      }

      // -i is directory for instance
      command.append(" -i ").append(TcServerUtil.getEscapeQuote()).append(sdkPath).append(TcServerUtil.getEscapeQuote());

      String execCommand = command.toString();
      MY_LOG.debug("createServer command " + execCommand);

      @NlsSafe final String scriptLog = TcServerUtil.runCommandWithSolidResult(execCommand, sdkPath,
                                                                               TcServerBundle.message("tcServerUtil.failedToCreateServer"));
      if (!MY_SUCCESSFUL_RUNTIME_INSTANCE_SCRIPT_PATTERN_AFTER_2_1.matcher(scriptLog).find()) {
        MY_LOG.warn(execCommand + "\n" + scriptLog);
        throw new ExecutionException(scriptLog);
      }
      else {
        MY_LOG.debug(scriptLog);
        if (SystemInfo.isWindows) {
          installWindowsServerService(sdkPath, serverName);
        }
      }
    }

    @Override
    public List<String> listTemplates(String sdkPath) throws ExecutionException {
      @NonNls File templatesDir = new File(sdkPath + File.separator + "templates");

      if (!templatesDir.exists() || !templatesDir.isDirectory()) {
        throw new ExecutionException(
          TcServerBundle.message("tcServerUtil.0.directory.not.found", FileUtil.toSystemIndependentName(templatesDir.getPath())));
      }


      File[] childDirs = templatesDir.listFiles(FileFilters.DIRECTORIES);
      List<String> result = new ArrayList<>(childDirs.length + 1);
      result.add("");
      for (File childDir : childDirs) {
        result.add(childDir.getName());
      }
      return result;
    }
  };


  private static final Logger MY_LOG = Logger.getInstance(TcServerVersion.class);
  @NonNls private static final Pattern MY_SUCCESSFUL_INSTALLATION_SCRIPT_PATTERN = Pattern.compile(".*installed.\\s*$");
  @NonNls private static final Pattern MY_ALREADY_INSTALLED_SCRIPT_PATTERN =
    Pattern.compile("wrapper  | CreateService failed - The specified service already exists. (0x431)\\s*$");

  public abstract List<String> getServers(String sdkPath) throws ExecutionException, RuntimeConfigurationException;

  protected abstract Pattern getJmxPortPattern();

  protected abstract Pattern getHttpPortPattern();

  public abstract void createServer(String sdkPath, String serverName, String pathToTemplate, boolean forceCreation)
    throws ExecutionException, RuntimeConfigurationException;

  public abstract List<String> listTemplates(String sdkPath) throws ExecutionException;

  private static void installWindowsServerService(String sdkPath, String serverName)
    throws RuntimeConfigurationException, ExecutionException {
    sdkPath = TcServerUtil.validateSdkPath(sdkPath);
    TcServerUtil.validateServerName(serverName);

    @NonNls StringBuilder command = new StringBuilder();

    command.append(TcServerUtil.getEscapeQuote()).append(sdkPath).append(File.separator).append(TcServerUtil.TC_RUNTIME_CTL_SCRIPT).
      append(TcServerUtil.getScriptExtension()).append(TcServerUtil.getEscapeQuote());
    command.append(" ").append(serverName);
    command.append(" install");

    String execCommand = command.toString();
    MY_LOG.debug("installServer command " + execCommand);

    @NlsSafe final String scriptLog = TcServerUtil.runCommandWithSolidResult(execCommand, sdkPath,
                                                                             TcServerBundle.message("tcServerUtil.failedToInstallService"));
    if (!MY_SUCCESSFUL_INSTALLATION_SCRIPT_PATTERN.matcher(scriptLog).find() &&
        MY_ALREADY_INSTALLED_SCRIPT_PATTERN.matcher(scriptLog).find()) {
      MY_LOG.warn(execCommand + "\n" + scriptLog);
      throw new ExecutionException(scriptLog);
    }
    else {
      MY_LOG.debug(scriptLog);
    }
  }

  public static TcServerVersion getVersion(String sdkPath) throws ExecutionException, RuntimeConfigurationException {
    sdkPath = TcServerUtil.validateSdkPath(sdkPath);

    @NlsSafe
    String execCommand = TcServerUtil.getEscapeQuote() + sdkPath + File.separator +
                         TcServerUtil.TC_RUNTIME_INSTANCE_SCRIPT + TcServerUtil.getScriptExtension() +
                         TcServerUtil.getEscapeQuote() + " " + "list";

    String scriptLog = TcServerUtil.runCommandWithSolidResult(execCommand, sdkPath,
                                                              TcServerBundle.message("tcServerUtil.failedToDetermineVersion"));
    MY_LOG.debug("script output    " + scriptLog);
    if (scriptLog.startsWith("Listing instances ")) {
      return EQUAL_OR_AFTER_2_1;
    }
    else if (scriptLog.startsWith("Invalid option")) {
      return BEFORE_2_1;
    }
    else {
      throw new ExecutionException(
        TcServerBundle.message("tcServerUtil.failedToDetermineVersionUnexpectedMessageBeginningOfInputStream.0", scriptLog));
    }
  }

  public static Map<String, Integer> getPorts(String sdkPath, String instanceName, @NotNull TcServerVersion version)
    throws RuntimeConfigurationException {

    @NonNls String catalinaPropertiesPath = TcServerUtil.getCatalinaPropertiesPath(sdkPath, instanceName);
    File catalinaProperties = new File(catalinaPropertiesPath);

    Scanner scanner;
    try {
      scanner = new Scanner(catalinaProperties);
    }
    catch (FileNotFoundException e) {
      throw new RuntimeConfigurationError(
        TcServerBundle.message("serverInstanceCreatorDialog.fileNotFound", catalinaPropertiesPath));
    }

    Map<String, Integer> result = new HashMap<>();
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      Matcher matcher = version.getJmxPortPattern().matcher(line);
      if (matcher.matches()) {
        result.put(TcServerUtil.JMX_PORT_KEY, Integer.valueOf(matcher.group(1)));
      }
      else {
        matcher = version.getHttpPortPattern().matcher(line);
        if (matcher.matches()) {
          result.put(TcServerUtil.HTTP_PORT_KEY, Integer.valueOf(matcher.group(1)));
        }
      }
    }
    scanner.close();

    return result;
  }
}
