// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.GrailsArtifact;

import java.util.List;

public class NewGrailsInterceptorAction extends NewGrailsXXXAction {

  public NewGrailsInterceptorAction() {
    super("action.Grails.NewInterceptor.text");
  }

  @Override
  protected boolean isEnabled(@NotNull GrailsApplication application) {
    return application.getGrailsVersion().isAtLeast("3.0");
  }

  @Override
  protected @NotNull String getCommand(@NotNull GrailsApplication application) {
    return "create-interceptor";
  }

  @Override
  protected @Nullable VirtualFile getTargetDirectory(@NotNull GrailsApplication application) {
    return GrailsArtifact.INTERCEPTOR.findDirectory(application);
  }

  @Override
  protected void fillGeneratedNamesList(@NotNull String name, @NotNull List<String> names) {
    // todo
  }
}
