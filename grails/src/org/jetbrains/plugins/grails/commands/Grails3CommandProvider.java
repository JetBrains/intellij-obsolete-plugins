// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.commands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.structure.Grails3Application;
import org.jetbrains.plugins.grails.structure.GrailsApplication;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

final class Grails3CommandProvider implements GrailsCommandProvider {
  private static final List<String> GRAILS3_COMMANDS = List.of(
    "bug-report",
    "clean",
    "compile",
    "console",
    "create-controller",
    "create-domain-class",
    "create-functional-test",
    "create-integration-test",
    "create-interceptor",
    "create-scaffold-controller",
    "create-script",
    "create-service",
    "create-taglib",
    "create-unit-test",
    "dependency-report",
    "generate-all",
    "generate-async-controller",
    "generate-controller",
    "gradle",
    "help",
    "install",
    "install-templates",
    "list-plugins",
    "open",
    "package",
    "plugin-info",
    "run-app",
    "schema-export",
    "shell",
    "stats",
    "stop-app",
    "test-app",
    "url-mappings-report",
    "war"
  );

  @Override
  public @NotNull Collection<String> addCommands(@NotNull GrailsApplication application) {
    return application instanceof Grails3Application ? GRAILS3_COMMANDS : Collections.emptyList();
  }
}
