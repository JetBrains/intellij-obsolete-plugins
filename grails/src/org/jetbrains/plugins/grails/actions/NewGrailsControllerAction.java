// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.artefact.api.GrailsArtefactHandler;
import org.jetbrains.plugins.grails.artefact.impl.ControllerArtefactHandler;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.List;

public class NewGrailsControllerAction extends NewGrailsXXXAction {

  public NewGrailsControllerAction() {
    super("action.Grails.NewController.text");
  }

  @Override
  protected @NotNull String getCommand(@NotNull GrailsApplication application) {
    return "create-controller";
  }

  @Override
  protected @Nullable VirtualFile getTargetDirectory(@NotNull GrailsApplication application) {
    return GrailsArtifact.CONTROLLER.findDirectory(application);
  }

  @Override
  protected void doAction(@NotNull GrailsApplication application, @NotNull String name) {
    name = StringUtil.trimEnd(name, GrailsArtifact.CONTROLLER.suffix);
    super.doAction(application, name);
  }

  @Override
  protected void fillGeneratedNamesList(@NotNull String name, @NotNull List<String> names) {
    name = StringUtil.trimEnd(name, GrailsArtifact.CONTROLLER.suffix);
    names.add("grails-app/controllers/" + canonicalize(name) + "Controller.groovy");
    names.add(GrailsUtils.GRAILS_UNIT_TESTS + canonicalize(name) + "ControllerSpec.groovy");
    names.add(GrailsUtils.GRAILS_INTEGRATION_TESTS + canonicalize(name) + "ControllerTests.groovy");
  }

  @Override
  protected @Nullable GrailsArtefactHandler getArtefactHandler() {
    return ControllerArtefactHandler.INSTANCE;
  }
}
