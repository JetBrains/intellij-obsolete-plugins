// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.sdk.GrailsSDK;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

public interface GrailsInstallationExecutor {

  boolean isApplicable(@NotNull GrailsSDK grailsSdk);

  @NotNull
  JavaParameters createJavaParameters(@NotNull Sdk sdk, @NotNull GrailsSDK grailsSdk, @NotNull MvcCommand command) throws ExecutionException;
}

