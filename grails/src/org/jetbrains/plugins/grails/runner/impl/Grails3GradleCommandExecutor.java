// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner.impl;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.groovy.grails.rt.GrailsRtMarker;
import org.jetbrains.plugins.grails.runner.GrailsCommandLineExecutor;
import org.jetbrains.plugins.grails.structure.Grails3Application;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

import static org.jetbrains.plugins.grails.runner.SetupKt.addPlainOutput;
import static org.jetbrains.plugins.grails.runner.util.GrailsExecutionUtils.addCommonJvmOptions;

public final class Grails3GradleCommandExecutor extends GrailsCommandLineExecutor {

  @Override
  public boolean isApplicable(@NotNull GrailsApplication grailsApplication) {
    return grailsApplication instanceof Grails3Application && !((Grails3Application)grailsApplication).getGradleData().getShellUrls().isEmpty();
  }

  @Override
  public @NotNull JavaParameters createJavaParameters(@NotNull GrailsApplication grailsApplication, @NotNull MvcCommand command)
    throws ExecutionException {
    final JavaParameters params = new JavaParameters();
    params.setJdk(ProjectRootManager.getInstance(grailsApplication.getProject()).getProjectSdk());
    params.getVMParametersList().addAll("-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1", "-XX:CICompilerCount=3");
    params.getVMParametersList().addParametersString(command.getVmOptions());
    addCommonJvmOptions(params);
    params.getClassPath().addAll(((Grails3Application)grailsApplication).getGradleData().getShellUrls());
    params.getClassPath().add(PathUtil.getJarPathForClass(GrailsRtMarker.class)); // we add rt.jar to enable execution of idea scripts
    params.setMainClass("org.grails.cli.GrailsCli");
    command.addToParametersList(params.getProgramParametersList());
    addPlainOutput(params.getProgramParametersList());
    params.setWorkingDirectory(VfsUtilCore.virtualToIoFile(grailsApplication.getRoot()));
    params.setDefaultCharset(grailsApplication.getProject());
    params.setUseClasspathJar(true);
    return params;
  }
}
