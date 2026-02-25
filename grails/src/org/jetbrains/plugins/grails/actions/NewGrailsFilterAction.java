// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.List;

public class NewGrailsFilterAction extends NewGrailsXXXAction {

  public NewGrailsFilterAction() {
    super("action.Grails.NewFilter.text");
  }

  @Override
  protected boolean isEnabled(@NotNull GrailsApplication application) {
    // todo substitute following with application instanceof Grails3Application
    return application.getGrailsVersion().isLessThan("3.0");
  }

  @Override
  protected @NotNull String getCommand(@NotNull GrailsApplication application) {
    return "create-filters";
  }

  @Override
  protected @Nullable VirtualFile getTargetDirectory(@NotNull GrailsApplication application) {
    return GrailsUtils.findConfDirectory(application);
  }

  @Override
  protected void fillGeneratedNamesList(@NotNull String name, @NotNull List<String> names) {
    names.add("grails-app/conf/" + canonicalize(name) + "Filters.groovy");
    names.add(GrailsUtils.GRAILS_UNIT_TESTS + canonicalize(name) + "FiltersSpec.groovy");
  }
}
