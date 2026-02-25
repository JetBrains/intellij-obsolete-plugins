// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner.impl;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.JDOMExternalizer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.JdomKt;
import com.intellij.util.PathUtil;
import com.intellij.util.PathsList;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.groovy.grails.rt.GrailsRtMarker;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.config.GrailsConstants;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.runner.GrailsCommandLineExecutor;
import org.jetbrains.plugins.grails.runner.GrailsInstallationExecutor;
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration;
import org.jetbrains.plugins.grails.runner.GrailsRunConfigurationExtension;
import org.jetbrains.plugins.grails.runner.ui.GrailsRunConfigurationEditor;
import org.jetbrains.plugins.grails.runner.ui.GrailsRunConfigurationEditorWithListener;
import org.jetbrains.plugins.grails.runner.util.GrailsExecutionUtils;
import org.jetbrains.plugins.grails.sdk.GrailsSDK;
import org.jetbrains.plugins.grails.sdk.GrailsSDKManager;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.OldGrailsApplication;
import org.jetbrains.plugins.grails.structure.impl.Grails2Application;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;
import org.jetbrains.plugins.groovy.util.GroovyUtils;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import java.io.File;
import java.util.Collection;
import java.util.List;

public final class GrailsInstallationCommandExecutor
  extends GrailsCommandLineExecutor
  implements GrailsRunConfigurationExtension<Boolean>, GrailsInstallationExecutor {

  private static final Key<Boolean> DATA_KEY = Key.create(GrailsInstallationCommandExecutor.class.getName() + " data key");
  private static final String DEPS_CLASSPATH = "depsClasspath";

  @Override
  public boolean isApplicable(@NotNull GrailsApplication grailsApplication) {
    return grailsApplication instanceof OldGrailsApplication && GrailsSDKManager.getGrailsSdk(grailsApplication) != null;
  }

  @Override
  public boolean isApplicable(@NotNull GrailsSDK grailsSdk) {
    return grailsSdk.getVersion().isLessThan(Version.GRAILS_3_0);
  }

  @Override
  public @NotNull JavaParameters createJavaParameters(@NotNull GrailsApplication grailsApplication, @NotNull MvcCommand command)
    throws ExecutionException {
    return createJavaParameters((OldGrailsApplication)grailsApplication, command, false);
  }

  @Override
  public void addListener(@NotNull JavaParameters params, @NotNull String listener) {
    super.addListener(params, listener);

    String listenerJar = PathUtil.getJarPathForClass(GrailsRtMarker.class);

    List<String> programParams = params.getProgramParametersList().getList();
    int cpIndex = programParams.indexOf("--classpath");

    if (cpIndex != -1 && cpIndex < programParams.size() - 1) {
      PathsList pathsList = new PathsList();
      pathsList.add(programParams.get(cpIndex + 1));
      pathsList.add(listenerJar);

      params.getProgramParametersList().replaceOrAppend(programParams.get(cpIndex + 1), pathsList.getPathsString());
    }
    else {
      params.getProgramParametersList().addAt(0, "--classpath");
      params.getProgramParametersList().addAt(1, listenerJar);
    }
  }

  @Override
  public @NotNull Key<Boolean> getKey() {
    return DATA_KEY;
  }

  @Override
  public @NotNull JavaParameters createJavaParameters(@NotNull GrailsApplication grailsApplication,
                                                      @NotNull MvcCommand command,
                                                      @Nullable Boolean depsClassPath) throws ExecutionException {
    if (grailsApplication instanceof OldGrailsApplication) {
      return createJavaParameters(((OldGrailsApplication)grailsApplication), command, depsClassPath != null && depsClassPath);
    }
    throw new IllegalStateException("Should not get here");
  }

  @Override
  public @Nullable SettingsEditor<GrailsRunConfiguration> createExtensionEditor() {
    return new GrailsRunConfigurationEditorWithListener() {

      private final JCheckBox myDepsClassPath = new JCheckBox();

      @Override
      protected void resetEditorFrom(@NotNull GrailsRunConfiguration s) {
        final Boolean depsClassPath = s.getUserData(getKey());
        myDepsClassPath.setSelected(depsClassPath != null && depsClassPath);
      }

      @Override
      protected void applyEditorTo(@NotNull GrailsRunConfiguration s) {
        s.putUserData(getKey(), myDepsClassPath.isEnabled() && myDepsClassPath.isSelected());
      }

      @Override
      protected @NotNull JComponent createEditor() {
        return myDepsClassPath;
      }

      @Override
      public void applicationChanged(GrailsApplication application) {
        if (application instanceof Grails2Application) {
          final Module module = ((Grails2Application)application).getModule();
          final String depsClasspath = GrailsFramework.getInstance().getApplicationClassPath(module).getPathsString();
          final boolean hasClasspath = StringUtil.isNotEmpty(depsClasspath);

          String presentable;
          if (hasClasspath) {
            String classpath = depsClasspath.length() > 70 ? depsClasspath.substring(0, 70) + "..." : depsClasspath;
            presentable = GrailsBundle.message("runner.configuration.label.text.add.classpath", classpath) ;
          } else {
            presentable = GrailsBundle.message("runner.configuration.label.text.add.empty.classpath");
          }
          myDepsClassPath.setText(presentable);
          myDepsClassPath.setToolTipText(GrailsBundle.message("checkbox.tooltip.text.html.nbsp.0.html",
                                                              StringUtil.replace(depsClasspath, File.pathSeparator, "<br>&nbsp;")));
          GrailsRunConfigurationEditor.setCBEnabled(hasClasspath, myDepsClassPath);
          myDepsClassPath.setVisible(true);
        }
        else {
          GrailsRunConfigurationEditor.setCBEnabled(false, myDepsClassPath);
          myDepsClassPath.setVisible(false);
        }
      }
    };
  }

  @Override
  public @Nullable Boolean readAdditionalConfiguration(@NotNull Element element) {
    return Boolean.parseBoolean(JDOMExternalizer.readString(element, DEPS_CLASSPATH));
  }

  @Override
  public void writeAdditionalConfiguration(@NotNull Boolean depsClassPath, @NotNull Element element) {
    JdomKt.addOptionTag(element, DEPS_CLASSPATH, Boolean.toString(depsClassPath), "setting");
  }

  public JavaParameters createJavaParameters(@NotNull OldGrailsApplication grailsApplication,
                                             @NotNull MvcCommand command,
                                             boolean classPathFromDependencies) throws ExecutionException {
    final GrailsSDK grailsSdk = GrailsSDKManager.getGrailsSdk(grailsApplication);
    if (grailsSdk == null) throw new ExecutionException(GrailsBundle.message("dialog.message.grails.sdk.not.defined"));
    Module module = grailsApplication.getModule();
    final Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
    if (sdk == null) throw new ExecutionException(ExecutionBundle.message("no.jdk.for.module.error.message", module.getName()));

    final JavaParameters params = createJavaParameters(sdk, grailsSdk, command);
    final VirtualFile rootFile = grailsApplication.getRoot();
    final String workDir = VfsUtilCore.virtualToIoFile(rootFile).getAbsolutePath();
    params.getVMParametersList().addProperty("base.dir", workDir);
    params.setWorkingDirectory(workDir);
    params.setDefaultCharset(grailsApplication.getProject());
    if (classPathFromDependencies) {
      addToClassPath(params, GrailsFramework.getInstance().getApplicationClassPath(module).getVirtualFiles());
    }
    return params;
  }

  @Override
  public @NotNull JavaParameters createJavaParameters(@NotNull Sdk sdk,
                                                      @NotNull GrailsSDK grailsSdk,
                                                      @NotNull MvcCommand command) {
    final JavaParameters params = new JavaParameters();
    params.setUseClasspathJar(true);

    final String grailsSdkHomePath = grailsSdk.getPath();

    params.setJdk(sdk);
    params.setupEnvs(command.getEnvVariables(), command.isPassParentEnvs());
    params.addEnv(GrailsConstants.GRAILS_HOME, FileUtil.toSystemDependentName(grailsSdkHomePath));
    GrailsFramework.addJavaHome(sdk, params);

    final File groovyJar = findGroovyJar(grailsSdkHomePath);
    if (groovyJar != null) params.getClassPath().add(groovyJar.getAbsolutePath());

    final File bootStrapJar = findBootStrapJar(grailsSdkHomePath);
    if (bootStrapJar != null) params.getClassPath().add(bootStrapJar.getAbsolutePath());

    /////////////////////////////////////////////////////////////

    params.setMainClass("org.codehaus.groovy.grails.cli.support.GrailsStarter");
    params.getVMParametersList().addProperty("grails.home", grailsSdkHomePath);

    final SdkTypeId sdkType = sdk.getSdkType();
    if (sdkType instanceof JavaSdkType) {
      params.getVMParametersList().addProperty("tools.jar", ((JavaSdkType)sdkType).getToolsPath(sdk));
    }

    final String confPath = grailsSdkHomePath + GrailsFramework.GROOVY_STARTER_CONF;
    params.getVMParametersList().addProperty("groovy.starter.conf", confPath);

    params.getProgramParametersList().add("--main");
    params.getProgramParametersList().add("org.codehaus.groovy.grails.cli.GrailsScriptRunner");
    params.getProgramParametersList().add("--conf");
    params.getProgramParametersList().add(confPath);

    final Version version = grailsSdk.getVersion();

    final PathsList cp = new PathsList();
    if (version.compareTo(Version.GRAILS_1_1) < 0) {
      cp.add("."); // needed for some Grails internals (e.g. 1.0.3 uses it to find test classes)
    }

    final String pathStr = cp.getPathsString();
    if (!pathStr.isEmpty()) {
      params.getProgramParametersList().add("--classpath");
      params.getProgramParametersList().add(pathStr);
    }

    final String grailsOpts = System.getenv("GRAILS_OPTS");
    if (!StringUtil.isEmptyOrSpaces(grailsOpts)) {
      params.getVMParametersList().addParametersString(grailsOpts);
    }

    params.getVMParametersList().addParametersString(command.getVmOptions());

    final String parametersString = params.getVMParametersList().getParametersString();
    if (!JavaSdk.getInstance().isOfVersionOrHigher(sdk, JavaSdkVersion.JDK_1_8)) {
      if (version.compareTo(Version.GRAILS_2_0) >= 0) {
        if (addMemoryConstraintIfNotExists(parametersString, params, "-Xmx", "768M")) {
          addMemoryConstraintIfNotExists(parametersString, params, "-Xms", "768M");
        }
        if (addMemoryConstraintIfNotExists(parametersString, params, "-XX:MaxPermSize=", "256m")) {
          addMemoryConstraintIfNotExists(parametersString, params, "-XX:PermSize=", "256m");
        }
      }
      else {
        addMemoryConstraintIfNotExists(parametersString, params, "-Xmx", "512M");
        addMemoryConstraintIfNotExists(parametersString, params, "-XX:MaxPermSize=", "192m");
      }
    }

    GrailsExecutionUtils.addCommonJvmOptions(params);


    final ParametersList grailsCommand = new ParametersList();
    command.addToParametersList(grailsCommand);
    if (!grailsCommand.hasParameter("-plain-output") && version.compareTo(Version.GRAILS_2_0) >= 0) {
      grailsCommand.add("-plain-output");
    }

    params.getProgramParametersList().add(grailsCommand.getParametersString());

    if (enableReloader(grailsCommand)) {
      final File springLoadedJar = findSpringLoadedJar(grailsSdkHomePath);
      if (springLoadedJar != null) {
        params.getVMParametersList().add("-javaagent:" + springLoadedJar.getAbsolutePath());
        params.getVMParametersList().add("-noverify");
        if (!params.getVMParametersList().hasProperty("springloaded")) {
          params.getVMParametersList().addProperty("springloaded", "profile=grails;cacheDir=.");
        }
      }
    }

    return params;
  }

  private static boolean addMemoryConstraintIfNotExists(String existedParams,
                                                        JavaParameters javaParameters,
                                                        String paramName,
                                                        String value) {
    if (!existedParams.contains(paramName)) {
      javaParameters.getVMParametersList().add(paramName + value);
      return true;
    }

    return false;
  }

  private static boolean enableReloader(@NotNull ParametersList grailsCommand) {
    // See $GRAILS_HOME/bin/startGrails
    return grailsCommand.hasParameter("-reloading") || grailsCommand.hasParameter("run-app") && !grailsCommand.hasParameter("-noreloading");
  }

  private static @Nullable File findGroovyJar(@NotNull String grailsSdkHomePath) {
    File groovyJar = GrailsUtils.findLatestJarInIvyRepository(grailsSdkHomePath + "/lib/org.codehaus.groovy/groovy-all", "groovy-all-");
    if (groovyJar != null) return groovyJar;

    final File[] files = GroovyUtils.getFilesInDirectoryByPattern(grailsSdkHomePath + "/lib", "groovy-all-\\d[^-]*\\.jar");
    if (files.length > 0) {
      groovyJar = files[0];
    }
    return groovyJar;
  }

  private static @Nullable File findBootStrapJar(@NotNull String grailsSdkHomePath) {
    final File[] bootstrapFiles = GroovyUtils.getFilesInDirectoryByPattern(
      grailsSdkHomePath + "/dist/", "grails-bootstrap-\\d[^-]*(?:-SNAPSHOT)?\\.jar"
    );
    if (bootstrapFiles.length > 0) {
      return bootstrapFiles[0];
    }

    final File[] cliFiles = GroovyUtils.getFilesInDirectoryByPattern(
      grailsSdkHomePath + "/dist/", "grails-cli-\\d[^-]*(?:-SNAPSHOT)?\\.jar"
    );
    if (cliFiles.length > 0) {
      return cliFiles[0];
    }

    return null;
  }

  private static @Nullable File findSpringLoadedJar(@NotNull String grailsSdkHomePath) {
    File springLoadedJar = GrailsUtils.findLatestJarInIvyRepository(
      grailsSdkHomePath + "/lib/org.springframework/springloaded", "springloaded-"
    );

    if (springLoadedJar == null) {
      springLoadedJar = GrailsUtils.findLatestJarInIvyRepository(
        grailsSdkHomePath + "/lib/org.springsource.springloaded/springloaded-core", "springloaded-core-"
      );
    }

    if (springLoadedJar == null) {
      springLoadedJar = GrailsUtils.findLatestJarInIvyRepository(
        grailsSdkHomePath + "/lib/com.springsource.springloaded/springloaded-core", "springloaded-core-"
      );
    }
    return springLoadedJar;
  }

  private static void addToClassPath(@NotNull JavaParameters params, Collection<VirtualFile> files) {
    List<String> programParams = params.getProgramParametersList().getList();
    int cpIndex = programParams.indexOf("--classpath");

    if (cpIndex != -1 && cpIndex < programParams.size() - 1) {
      PathsList pathsList = new PathsList();
      pathsList.add(programParams.get(cpIndex + 1));
      pathsList.addVirtualFiles(files);

      params.getProgramParametersList().replaceOrAppend(programParams.get(cpIndex + 1), pathsList.getPathsString());
    }
    else {
      PathsList pathsList = new PathsList();
      pathsList.addVirtualFiles(files);
      params.getProgramParametersList().addAt(0, "--classpath");
      params.getProgramParametersList().addAt(1, pathsList.getPathsString());
    }
  }
}