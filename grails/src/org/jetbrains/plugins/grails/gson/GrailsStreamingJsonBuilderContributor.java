// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.gson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.builder.StreamingJsonBuilderDelegateContributor;

public final class GrailsStreamingJsonBuilderContributor extends StreamingJsonBuilderDelegateContributor {

  @Override
  protected @NotNull String getDelegateClassName() {
    return "grails.plugin.json.builder.StreamingJsonBuilder.StreamingJsonDelegate";
  }
}
