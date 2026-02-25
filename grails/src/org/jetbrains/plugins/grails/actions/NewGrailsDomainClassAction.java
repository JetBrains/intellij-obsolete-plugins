// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.artefact.api.GrailsArtefactHandler;
import org.jetbrains.plugins.grails.artefact.impl.DomainArtefactHandler;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.List;

public class NewGrailsDomainClassAction extends NewGrailsXXXAction {

  public NewGrailsDomainClassAction() {
    super("action.Grails.NewDomainClass.text");
  }

  @Override
  protected @NotNull String getCommand(@NotNull GrailsApplication application) {
    return "create-domain-class";
  }

  @Override
  protected @Nullable VirtualFile getTargetDirectory(@NotNull GrailsApplication application) {
    return GrailsArtifact.DOMAIN.findDirectory(application);
  }

  @Override
  protected void fillGeneratedNamesList(@NotNull String name, @NotNull List<String> names) {
    names.add("grails-app/domain/" + canonicalize(name) + ".groovy");
    names.add(GrailsUtils.GRAILS_UNIT_TESTS + canonicalize(name) + "Spec.groovy");
    names.add(GrailsUtils.GRAILS_INTEGRATION_TESTS + canonicalize(name) + "Tests.groovy");
  }

  @Override
  protected @Nullable GrailsArtefactHandler getArtefactHandler() {
    return DomainArtefactHandler.INSTANCE;
  }
}
