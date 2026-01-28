// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.providers.view;

import com.intellij.helidon.HelidonIcons;
import com.intellij.helidon.providers.HelidonRequestMethods;
import com.intellij.helidon.utils.HelidonBundle;
import com.intellij.helidon.utils.HelidonCommonUtils;
import com.intellij.helidon.utils.HelidonUrlTargetInfo;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.microservices.endpoints.*;
import com.intellij.microservices.endpoints.presentation.HttpMethodPresentation;
import com.intellij.microservices.jvm.cache.SourceTestLibSearcher;
import com.intellij.microservices.url.UrlPath;
import com.intellij.microservices.url.UrlTargetInfo;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.uast.UastModificationTracker;
import com.intellij.util.CommonProcessors.CollectProcessor;
import kotlin.text.StringsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UastContextKt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.intellij.helidon.utils.HelidonCommonUtils.hasHelidonLibrary;
import static com.intellij.microservices.endpoints.EndpointTypes.HTTP_SERVER_TYPE;
import static com.intellij.microservices.jvm.url.UastUrlAttributeUtils.isUastDeclarationDeprecated;
import static com.intellij.util.containers.ContainerUtil.emptyList;

final class HelidonUrlFramework implements EndpointsUrlTargetProvider<HelidonUrlTargetInfo, HelidonUrlTargetInfo> {
  private final FrameworkPresentation myPresentation =
    new FrameworkPresentation(HelidonBundle.HELIDON_LIBRARY, HelidonBundle.HELIDON_LIBRARY, HelidonIcons.Helidon);

  private final SourceTestLibSearcher<HelidonUrlTargetInfo> groupsSearcher =
    new SourceTestLibSearcher<>("HELIDON_GROUPS", HelidonUrlFramework::findEndpointGroups);

  @Override
  public @NotNull EndpointType getEndpointType() {
    return HTTP_SERVER_TYPE;
  }

  @Override
  public @NotNull FrameworkPresentation getPresentation() {
    return myPresentation;
  }

  @Override
  public @NotNull Status getStatus(@NotNull Project project) {
    if (hasHelidonLibrary(project)) return Status.HAS_ENDPOINTS;
    return Status.UNAVAILABLE;
  }

  @Override
  public @NotNull Iterable<HelidonUrlTargetInfo> getEndpointGroups(@NotNull Project project, @NotNull EndpointsFilter filter) {
    if (!(filter instanceof ModuleEndpointsFilter moduleFilter)) return emptyList();

    Module module = moduleFilter.getModule();
    if (!hasHelidonLibrary(module)) return emptyList();

    return groupsSearcher.iterable(moduleFilter.getModule(), moduleFilter.getFromTests(), moduleFilter.getFromLibraries());
  }

  private static @NotNull Collection<HelidonUrlTargetInfo> findEndpointGroups(Module module, GlobalSearchScope filterScope) {
    CollectProcessor<HelidonUrlTargetInfo> collectProcessor = new CollectProcessor<>();
    GlobalSearchScope classReferencesScope = HelidonCommonUtils.getRoutingClassReferencesScope(module)
      .intersectWith(filterScope);

    HelidonCommonUtils.processBuilderRegisterMethodsWithProgress(collectProcessor, classReferencesScope, module);
    HelidonCommonUtils.processBuilderHttpMethods(collectProcessor, classReferencesScope, module);

    return collectProcessor.getResults();
  }

  @Override
  public @NotNull Iterable<HelidonUrlTargetInfo> getEndpoints(@NotNull HelidonUrlTargetInfo registerEndpoint) {
    return getRegisteredEndpoints(registerEndpoint);
  }

  @Override
  public @NotNull ModificationTracker getModificationTracker(@NotNull Project project) {
    return UastModificationTracker.getInstance(project);
  }

  private static @NotNull Iterable<HelidonUrlTargetInfo> getRegisteredEndpoints(@NotNull HelidonUrlTargetInfo groupEndpoint) {
    PsiElement registerPoint = groupEndpoint.resolveToPsiElement();
    if (registerPoint == null) return Collections.emptyList();
    if (groupEndpoint.getType() != HelidonRequestMethods.REGISTER) return Collections.singletonList(groupEndpoint);
    UCallExpression invocationPoint = UastContextKt.getUastParentOfType(registerPoint, UCallExpression.class);
    if (invocationPoint != null) {
      PsiType serviceType = HelidonCommonUtils.getRegisteredServiceType(invocationPoint);
      if (serviceType instanceof PsiClassType) {
        PsiClass resolve = ((PsiClassType)serviceType).resolve();
        if (resolve != null) {
          CollectProcessor<HelidonUrlTargetInfo> collectProcessor = new CollectProcessor<>() {
            @Override
            protected boolean accept(HelidonUrlTargetInfo info) {
              String parentUrl = info.getParentUrl();
              return parentUrl != null && groupEndpoint.getPath()
                .equals(UrlPath.Companion.fromExactString(StringsKt.removePrefix(info.getParentUrl(), "/")));
            }
          };
          HelidonCommonUtils.processRulesHttpMethods(collectProcessor, new LocalSearchScope(resolve),
                                                     ModuleUtilCore.findModuleForPsiElement(registerPoint));
          return collectProcessor.getResults();
        }
        return emptyList();
      }
    }
    return emptyList();
  }


  private static @NotNull PresentationData getPresentation(HelidonUrlTargetInfo info, String url) {
    HelidonRequestMethods infoType = info.getType();
    String methodType = infoType == HelidonRequestMethods.REGISTER || infoType == HelidonRequestMethods.UNKNOWN ? "" : infoType.name();
    return new HttpMethodPresentation(url.startsWith("/") ? url : "/" + url, methodType, getEndpointContainerName(info),
                                      HelidonIcons.Helidon,
                                      isUastDeclarationDeprecated(info.resolveToPsiElement()) ? CodeInsightColors.DEPRECATED_ATTRIBUTES : null);
  }

  @Override
  public @NotNull ItemPresentation getEndpointPresentation(@NotNull HelidonUrlTargetInfo group, @NotNull HelidonUrlTargetInfo endpoint) {
    return getPresentation(endpoint, joinSegments(endpoint.getPath()));
  }

  private static String joinSegments(@NotNull UrlPath path) {
    return path.getPresentation(UrlPath.FULL_PATH_VARIABLE_PRESENTATION);
  }

  private static @NotNull String getEndpointContainerName(@NotNull HelidonUrlTargetInfo endpoint) {
    PsiMethod method = PsiTreeUtil.getParentOfType(endpoint.resolveToPsiElement(), PsiMethod.class);
    if (method == null) return "";
    PsiClass aClass = method.getContainingClass();
    if (aClass != null && aClass.getName() != null) {
      return aClass.getName();
    }
    return method.getName();
  }

  @Override
  public @Nullable PsiElement getDocumentationElement(@NotNull HelidonUrlTargetInfo group, @NotNull HelidonUrlTargetInfo endpoint) {
    return endpoint.resolveToPsiElement();
  }

  @Override
  public @NotNull Iterable<UrlTargetInfo> getUrlTargetInfo(@NotNull HelidonUrlTargetInfo group, @NotNull HelidonUrlTargetInfo endpoint) {
    return List.of(endpoint);
  }

  @Override
  public boolean isValidEndpoint(@NotNull HelidonUrlTargetInfo group, @NotNull HelidonUrlTargetInfo endpoint) {
    PsiElement psiElement = endpoint.resolveToPsiElement();
    return psiElement != null && psiElement.isValid();
  }
}
