// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.providers;

import com.intellij.helidon.utils.HelidonCommonUtils;
import com.intellij.microservices.url.UrlResolver;
import com.intellij.microservices.url.UrlResolverFactory;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class HelidonResolverFactory implements UrlResolverFactory {
  @Override
  public UrlResolver forProject(@NotNull Project project) {
    return HelidonCommonUtils.hasHelidonLibrary(project) ? new HelidonUrlResolver(project) : null;
  }
}
