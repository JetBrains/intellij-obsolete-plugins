// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.commands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.OldGrailsApplication;
import org.jetbrains.plugins.groovy.mvc.util.MvcTargetDialogCompletionUtils;

import java.util.Collection;
import java.util.Collections;

final class OldGrailsCommandProvider implements GrailsCommandProvider {
  @Override
  public @NotNull Collection<String> addCommands(@NotNull GrailsApplication application) {
    if (!(application instanceof OldGrailsApplication)) {
      return Collections.emptyList();
    }
    return MvcTargetDialogCompletionUtils.getAllTargetNames(((OldGrailsApplication)application).getModule());
  }
}
