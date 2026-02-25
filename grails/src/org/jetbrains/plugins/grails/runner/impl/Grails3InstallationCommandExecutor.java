// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner.impl;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.groovy.grails.rt.GrailsRtMarker;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.runner.GrailsCommandLineExecutor;
import org.jetbrains.plugins.grails.runner.GrailsInstallationExecutor;
import org.jetbrains.plugins.grails.sdk.GrailsSDK;
import org.jetbrains.plugins.grails.sdk.GrailsSDKManager;
import org.jetbrains.plugins.grails.structure.Grails3Application;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.plugins.grails.runner.SetupKt.addPlainOutput;
import static org.jetbrains.plugins.grails.runner.util.GrailsExecutionUtils.addCommonJvmOptions;

public final class Grails3InstallationCommandExecutor extends GrailsCommandLineExecutor implements GrailsInstallationExecutor {

  @Override
  public boolean isApplicable(@NotNull GrailsApplication grailsApplication) {
    return grailsApplication instanceof Grails3Application && GrailsSDKManager.getGrailsSdk(grailsApplication) != null;
  }

  @Override
  public boolean isApplicable(@NotNull GrailsSDK grailsSdk) {
    return grailsSdk.getVersion().isAtLeast(Version.GRAILS_3_0);
  }

  @Override
  public @NotNull JavaParameters createJavaParameters(@NotNull GrailsApplication grailsApplication,
                                                      @NotNull MvcCommand command) throws ExecutionException {
    final GrailsSDK grailsSdk = GrailsSDKManager.getGrailsSdk(grailsApplication);
    if (grailsSdk == null) throw new ExecutionException(GrailsBundle.message("dialog.message.grails.sdk.not.defined"));

    final Sdk sdk = ProjectRootManager.getInstance(grailsApplication.getProject()).getProjectSdk();
    if (sdk == null) throw new ExecutionException(GrailsBundle.message("dialog.message.grails.sdk.not.defined"));

    final JavaParameters params = createJavaParameters(sdk, grailsSdk, command);
    params.setWorkingDirectory(VfsUtilCore.virtualToIoFile(grailsApplication.getRoot()));
    params.setDefaultCharset(grailsApplication.getProject());
    return params;
  }

  @Override
  public @NotNull JavaParameters createJavaParameters(@NotNull Sdk sdk, @NotNull GrailsSDK grailsSdk, @NotNull MvcCommand command)
    throws ExecutionException {
    final JavaParameters params = new JavaParameters();
    params.setJdk(sdk);
    params.getVMParametersList().addAll("-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1", "-XX:CICompilerCount=3");
    params.getVMParametersList().addParametersString(command.getVmOptions());
    addCommonJvmOptions(params);
    params.getClassPath().addAllFiles(runClassPath(new File(grailsSdk.getPath())));
    params.getClassPath().add(PathUtil.getJarPathForClass(GrailsRtMarker.class)); // we add rt.jar to enable execution of idea scripts
    params.setMainClass("org.grails.cli.GrailsCli");
    command.addToParametersList(params.getProgramParametersList());
    if (!grailsSdk.getVersion().isAtLeast(Version.GRAILS_4_0)) {
      addPlainOutput(params.getProgramParametersList());
    }
    params.setUseClasspathJar(true);
    return params;
  }

  private static List<File> runClassPath(File grailsSDKHome) {
    final File grailsSDKDist = new File(grailsSDKHome.getAbsolutePath() + "/dist");
    final File grailsSDKLib = new File(grailsSDKHome.getAbsolutePath() + "/lib");
    if (!grailsSDKDist.exists() || !grailsSDKLib.exists()) return Collections.emptyList();
    final List<File> result = new ArrayList<>();
    FileUtil.processFilesRecursively(grailsSDKHome, file -> {
      final String fileName = file.getName();
      if (fileName.endsWith(".jar") &&
          !fileName.endsWith("-sources.jar") &&
          !fileName.endsWith("-javadoc.jar") &&
          !fileName.matches("javaee-(web-)?api(-\\d(\\.\\d)*)?.jar")) {
        result.add(file);
      }
      return true;
    });
    return result;
  }
}
