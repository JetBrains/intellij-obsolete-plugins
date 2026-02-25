// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.gradle.tooling.builder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class GrailsModuleImpl implements GrailsModule {

  private static final long serialVersionUID = 1L;

  private final @NotNull String myGrailsVersion;

  private final @NotNull String myGrailsPluginId;

  private final @Nullable List<String> myShellUrls;

  public GrailsModuleImpl(@NotNull String grailsVersion, @NotNull String grailsPluginId) {
    this(grailsVersion, grailsPluginId, null);
  }

  public GrailsModuleImpl(@NotNull String grailsVersion, @NotNull String grailsPluginId, @Nullable List<String> urls) {
    myGrailsVersion = grailsVersion;
    myGrailsPluginId = grailsPluginId;
    myShellUrls = urls;
  }

  @Override
  public @NotNull String getGrailsVersion() {
    return myGrailsVersion;
  }

  @Override
  public @NotNull String getGrailsPluginId() {
    return myGrailsPluginId;
  }

  @Override
  public @Nullable List<String> getShellUrls() {
    return myShellUrls;
  }
}
