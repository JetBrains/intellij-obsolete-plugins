package com.intellij.tcserver.sdk;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileFilters;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tcserver.util.TcServerBundle;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public final class TcServerUtil {

  private static final Logger MY_LOG = Logger.getInstance(TcServerUtil.class);
  @NonNls private static final Pattern MY_SERVER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\.][a-zA-Z0-9\\-_\\.]*$");
  @NonNls public static final String TC_RUNTIME_INSTANCE_SCRIPT = "tcruntime-instance";
  @NonNls public static final String TC_RUNTIME_CTL_SCRIPT = "tcruntime-ctl";
  @NonNls private static final String TOMCAT_DIRECTORY_NAME_BEGINNING = "tomcat-";
  @NonNls private static final String JAVA_HOME_VAR_NAME = "JAVA_HOME";
  @NonNls private static final String JAVA_HOME_PROPERTY_NAME = "java.home";


  private TcServerUtil() {
  }

  @NonNls
  public static String getScriptExtension() {
    return SystemInfo.isWindows ? ".bat" : ".sh";
  }

  public static String getEscapeQuote() {
    return SystemInfo.isWindows ? "\"" : "";
  }

  public static @NotNull String validatePath(String path) throws RuntimeConfigurationException {
    if (isWhiteSpacesNotAllowed(path)) {
      throw new RuntimeConfigurationError(TcServerBundle.message("tcServerUtil.validation.pathWithWhitespaces"));
    }

    //in Windows can`t stand \ in the end of path
    path = removeTrailingSeparators(path);

    return path;
  }

  public static @NotNull String validateSdkPath(String sdkPath) throws RuntimeConfigurationException {
    if (StringUtil.isEmpty(sdkPath)) {
      throw new RuntimeConfigurationError(TcServerBundle.message("tcServerUtil.validation.emptyPath"));
    }

    sdkPath = validatePath(sdkPath);

    File tcruntimeCtlFile = new File(sdkPath + File.separator + TC_RUNTIME_CTL_SCRIPT + getScriptExtension());
    if (!tcruntimeCtlFile.exists()) {
      throw new RuntimeConfigurationError(TcServerBundle.message("tcServerUtil.validation.noExecutable", tcruntimeCtlFile.getPath()));
    }

    File tcruntimeInstanceFile = new File(sdkPath + File.separator + TC_RUNTIME_INSTANCE_SCRIPT + getScriptExtension());
    if (!tcruntimeInstanceFile.exists()) {
      throw new RuntimeConfigurationError(TcServerBundle.message("tcServerUtil.validation.noExecutable", tcruntimeInstanceFile.getPath()));
    }

    validateJavaHome();

    return sdkPath;
  }

  public static void validateServerName(String serverName) throws RuntimeConfigurationException {
    if (!MY_SERVER_NAME_PATTERN.matcher(serverName).matches()) {
      throw new RuntimeConfigurationError(TcServerBundle.message("tcServerUtil.validation.invalidCharsInServerInstanceName"));
    }
  }

  public static String runCommandWithSolidResult(String execCommand, String sdkPath, @Nls String errorMessage) throws ExecutionException {
    return StringUtil.join(runCommandWithByLineResult(execCommand, sdkPath, errorMessage), "\n");
  }

  public static List<String> runCommandWithByLineResult(String execCommand, String sdkPath, @Nls String errorMessage)
    throws ExecutionException {

    BufferedReader input = null;
    try {
      Runtime rt = Runtime.getRuntime();

      /*
       *  The script calls java, adding his own -d and -n. They do not override ours,
       *  but they must not contain spaces outside Windows("Invalid parameter" is written otherwise).
       *  -d comes with absolute path of the script which is already checked,
       *  and  -n comes with 'pwd', which is %IDEA_HOME%/bin for default for running plugin
      *  and %PROJECT_HOME%  for running tests. So we override it with sdkPath,
      *  that surely contains no spaces.
      */
      String[] environmentVariables = validateJavaHome();
      MY_LOG.debug("execCommand: " + execCommand + ", sdk: " + sdkPath);
      Process pr = rt.exec(execCommand, environmentVariables, new File(sdkPath));

      input = new BufferedReader(new InputStreamReader(pr.getInputStream(), StandardCharsets.UTF_8));

      String line;
      List<String> result = new ArrayList<>();
      while ((line = input.readLine()) != null) {
        result.add(line);
      }

      if (ContainerUtil.isEmpty(result)) {
        MY_LOG.debug("script log is empty, getting error stream...");
        String errorContent = FileUtil.loadTextAndClose(new InputStreamReader(pr.getErrorStream(), StandardCharsets.UTF_8));
        result.add(errorContent);
      }
      return result;
    }
    catch (IOException | RuntimeConfigurationException e) {
      MY_LOG.warn(e);
      throw new ExecutionException(errorMessage + e.getMessage());
    }
    finally {
      if (input != null) {
        try {
          input.close();
        }
        catch (IOException e) {
          //ignore
        }
      }
    }
  }

  @NonNls
  public static String getTemplatesPath(String sdkPath) {
    sdkPath = removeTrailingSeparators(sdkPath);
    return sdkPath + File.separator + File.separator + File.separator + File.separator + "templates";
  }

  @NonNls
  public static String[] getStartServerCommandline(String sdkPath, String instanceName, boolean batch) {
    return getServerServiceCommandline(sdkPath, instanceName, batch ? "batch" : "start");
  }

  @NonNls
  public static String[] getStopServerServiceCommandline(String sdkPath, String instanceName) {
    return getServerServiceCommandline(sdkPath, instanceName, "stop");
  }

  public static String @Nullable [] validateJavaHome() throws RuntimeConfigurationException {
    String javaHomeEnvVar = System.getenv(JAVA_HOME_VAR_NAME);
    MY_LOG.debug(JAVA_HOME_VAR_NAME + "=" + javaHomeEnvVar);
    if (javaHomeEnvVar != null) {
      return null;
    }

    String javaHomeProperty = System.getProperty(JAVA_HOME_PROPERTY_NAME);
    MY_LOG.debug(JAVA_HOME_PROPERTY_NAME + "=" + javaHomeProperty);
    if (isWhiteSpacesNotAllowed(javaHomeProperty)) {
      throw new RuntimeConfigurationException(TcServerBundle.message("tcServerUtil.javaHomeValidation", javaHomeProperty));
    }
    return new String[]{JAVA_HOME_VAR_NAME + "=" + javaHomeProperty};
  }

  private static boolean isWhiteSpacesNotAllowed(String path) {
    return !SystemInfo.isWindows && StringUtil.containsWhitespaces(path);
  }

  @NonNls
  private static String[] getServerServiceCommandline(String sdkPath, String instanceName, @NonNls String command) {
    sdkPath = removeTrailingSeparators(sdkPath);
    return new String[]{sdkPath + File.separator + TC_RUNTIME_CTL_SCRIPT + getScriptExtension(), instanceName, command};
  }

  @Contract(pure = true)
  public static @NotNull String removeTrailingSeparators(String path) {
    return path.replaceFirst("[/\\\\]*$", "");
  }

  public static File[] getLibraries(String sdkPath, String instanceName) throws RuntimeConfigurationException {
    validateSdkPath(sdkPath);

    String catalinaHomePath = getCatalinaHomePath(sdkPath, instanceName);
    @NonNls String scriptPath = catalinaHomePath + File.separator + "bin" + File.separator + "tcruntime-ctl" + getScriptExtension();
    if (!new File(scriptPath).exists()) {
      throw new RuntimeConfigurationError(
        TcServerBundle.message("tcRuntimeCtlBat.incorrectCatalinaHomeVariable", catalinaHomePath));
    }

    @NonNls String libPath = catalinaHomePath + File.separator + "lib";
    File lib = new File(libPath);
    if (lib.exists() && lib.isDirectory()) {
      return lib.listFiles(FileFilters.withExtension("jar"));
    }
    else {
      return new File[0];
    }
  }
  //instanceName is needed to check whether extra settings for path exist

  private static String getCatalinaHomePath(String sdkPath, String instanceName) throws RuntimeConfigurationException {
    validateSdkPath(sdkPath);

    sdkPath = removeTrailingSeparators(sdkPath);
    String catalinaBase = sdkPath + File.separator + instanceName;
    String tomcatVersion = null;
    @NonNls String pathToVersion = catalinaBase + File.separator + "conf" + File.separator + "tomcat.version";
    if (new File(catalinaBase).exists()) {
      tomcatVersion = readFromFile(pathToVersion, "TOMCAT_VER2");
    }

    if (tomcatVersion != null) {
      String catalinaHome = sdkPath + File.separator + TOMCAT_DIRECTORY_NAME_BEGINNING + tomcatVersion;
      if (new File(catalinaHome).exists()) {
        return catalinaHome;
      }
      if (new File(tomcatVersion).exists()) {
        return tomcatVersion;
      }
    }
    else {
      File realPath = new File(sdkPath);
      File[] tomcats = realPath.listFiles(
        pathName -> pathName.isDirectory() && pathName.getName().startsWith(TOMCAT_DIRECTORY_NAME_BEGINNING));
      if (tomcats.length != 0) {
        File tomcatInstallation = Collections.min(Arrays.asList(tomcats), Comparator.comparing(File::getName));
        return tomcatInstallation.getPath();
      }
    }

    throw new RuntimeConfigurationError(
      TcServerBundle.message("tcRuntimeCtlBat.incorrectCatalinaHomeVariable", sdkPath + File.separator + TOMCAT_DIRECTORY_NAME_BEGINNING));
  }

  @Nullable
  private static String readFromFile(String path, @NonNls String variable) {
    try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
      StringBuilder sb = new StringBuilder();
      while (in.ready()) {
        sb.append(in.readLine());
      }
      return sb.toString();
    }
    catch (IOException e) {
      MY_LOG.debug("Failed to read variable " + variable + " from file " + path, e);
      //silently, because often there really should not be any file by default,
      // and the case when variable is not reset is provided by algorithm
    }
    return null;
  }


  @NonNls public static final String JMX_PORT_KEY = "jmx";
  public static final String HTTP_PORT_KEY = "http";

  @NonNls
  public static String getCatalinaPropertiesPath(String sdkPath, String instanceName) {
    return removeTrailingSeparators(sdkPath) +
           File.separator +
           instanceName +
           File.separator +
           "conf" +
           File.separator +
           "catalina.properties";
  }
}