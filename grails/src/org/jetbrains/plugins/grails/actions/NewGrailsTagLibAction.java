// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.actions;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.artefact.api.GrailsArtefactHandler;
import org.jetbrains.plugins.grails.artefact.impl.TaglibArtefactHandler;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.grails.util.version.Version;

import java.util.List;

public class NewGrailsTagLibAction extends NewGrailsXXXAction {

  public NewGrailsTagLibAction() {
    super("action.Grails.NewTagLib.text");
  }

  @Override
  protected @NotNull String getCommand(@NotNull GrailsApplication application) {
    if (application.getGrailsVersion().isAtLeast(Version.GRAILS_3_0)) {
      return "create-taglib";
    }
    else {
      return "create-tag-lib";
    }
  }

  @Override
  protected @Nullable VirtualFile getTargetDirectory(@NotNull GrailsApplication application) {
    return GrailsArtifact.TAGLIB.findDirectory(application);
  }

  @Override
  protected void doAction(@NotNull GrailsApplication application, @NotNull String name) {
    name = StringUtil.trimEnd(name, GrailsArtifact.TAGLIB.suffix);
    super.doAction(application, name);
  }

  @Override
  protected void fillGeneratedNamesList(@NotNull String name, @NotNull List<String> names) {
    name = StringUtil.trimEnd(name, GrailsArtifact.TAGLIB.suffix);
    names.add("grails-app/taglib/" + canonicalize(name) + "TagLib.groovy");
    names.add("test/unit/" + canonicalize(name) + "TagLibSpec.groovy");
    names.add(GrailsUtils.GRAILS_INTEGRATION_TESTS + canonicalize(name) + "TagLibTests.groovy");
  }

  @Override
  protected @Nullable GrailsArtefactHandler getArtefactHandler() {
    return TaglibArtefactHandler.INSTANCE;
  }
}
