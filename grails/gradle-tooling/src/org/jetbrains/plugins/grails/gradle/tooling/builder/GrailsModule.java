// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.gradle.tooling.builder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

public interface GrailsModule extends Serializable {

  @NotNull
  String getGrailsVersion();

  @NotNull
  String getGrailsPluginId();

  @Nullable
  List<String> getShellUrls();
}
