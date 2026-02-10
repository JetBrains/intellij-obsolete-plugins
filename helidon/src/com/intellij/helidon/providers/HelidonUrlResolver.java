// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.providers;

import com.intellij.helidon.utils.HelidonBundle;
import com.intellij.helidon.utils.HelidonCommonUtils;
import com.intellij.helidon.utils.HelidonUrlTargetInfo;
import com.intellij.ide.presentation.Presentation;
import com.intellij.microservices.url.HttpUrlResolver;
import com.intellij.microservices.url.UrlResolveRequest;
import com.intellij.microservices.url.UrlTargetInfo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.CommonProcessors.CollectProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;

@Presentation(typeName = HelidonBundle.HELIDON_LIBRARY, icon = "com.intellij.helidon.HelidonIcons.Helidon")
public final class HelidonUrlResolver extends HttpUrlResolver {
  private final Project myProject;

  public HelidonUrlResolver(Project project) { myProject = project; }

  @Override
  public @NotNull Iterable<UrlTargetInfo> resolve(@NotNull UrlResolveRequest request) {
    // todo implement resolve
    return Collections.emptyList();
  }

  @Override
  public @NotNull Iterable<UrlTargetInfo> getVariants() {
    CollectProcessor<HelidonUrlTargetInfo> collectProcessor = new CollectProcessor<>();
    for (Module module : ModuleManager.getInstance(myProject).getModules()) {
      if (!HelidonCommonUtils.hasHelidonLibrary(module)) continue;

      GlobalSearchScope scope = HelidonCommonUtils.getRoutingClassReferencesScope(module);
      if (!HelidonCommonUtils.processBuilderRegisterMethods(collectProcessor, scope, module)) break;
      if (!HelidonCommonUtils.processRulesHttpMethods(collectProcessor, scope, module)) break;
    }
    return new ArrayList<>(collectProcessor.getResults());
  }
}
