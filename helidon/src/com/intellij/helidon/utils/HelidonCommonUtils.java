// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.helidon.utils;

import com.intellij.codeInspection.dataFlow.StringExpressionHelper;
import com.intellij.helidon.constants.HelidonConstants;
import com.intellij.helidon.providers.HelidonRequestMethods;
import com.intellij.java.library.JavaLibraryModificationTracker;
import com.intellij.java.library.JavaLibraryUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.uast.UastModificationTracker;
import com.intellij.uast.UastSmartPointer;
import com.intellij.util.Processor;
import com.intellij.util.containers.ConcurrentFactoryMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.intellij.helidon.providers.HelidonReferenceContributorKt.*;

public final class HelidonCommonUtils {
  private static final Key<CachedValue<Map<SearchScope, Set<UCallExpression>>>> METHOD_INVOCATIONS_KEY =
    Key.create("METHOD_INVOCATIONS_KEY");

  private HelidonCommonUtils() {
  }

  public static boolean hasHelidonLibrary(Project project) {
    return JavaLibraryUtil.hasLibraryClass(project, HelidonConstants.ROUTING);
  }

  public static boolean hasHelidonLibrary(@Nullable Module module) {
    return JavaLibraryUtil.hasLibraryClass(module, HelidonConstants.ROUTING);
  }

  public static boolean hasHelidonMPLibrary(@Nullable Module module) {
    return JavaLibraryUtil.hasLibraryClass(module, HelidonConstants.MP_MAIN);
  }

  public static @NotNull Set<String> getParentUrlPaths(@Nullable PsiElement host) {
    if (host == null) return Collections.emptySet();
    Set<String> paths = RecursionManager.doPreventingRecursion(host, true, () -> calculateParentUrls(host));
    return paths != null ? paths : Collections.emptySet();
  }

  private static @NotNull Set<String> calculateParentUrls(@NotNull PsiElement host) {
    Set<String> allParentPaths = new HashSet<>();
    UClass definedInUClass = UastContextKt.getUastParentOfType(host, UClass.class);
    if (definedInUClass == null) return Collections.emptySet();
    //PsiClass baseClass = JavaPsiFacade.getInstance(host.getProject()).findClass(HelidonConstants.SERVICE, definedInUxClass.getResolveScope());
    //if (baseClass == null || definedInClass.isInheritor(baseClass, false)) return Collections.emptySet();
    Module module = ModuleUtilCore.findModuleForPsiElement(host);
    if (module == null) return Collections.emptySet();
    for (PsiMethod registerMethod : getBuilderRegisterMethod(module)) {
      Map<String, UExpression> expressionMap = mapUrlsToServiceInvocations(module, registerMethod);
      for (Map.Entry<String, UExpression> entry : expressionMap.entrySet()) {
        String urlDefinition = entry.getKey();
        if (StringUtil.isNotEmpty(urlDefinition)) {
          PsiType type = entry.getValue().getExpressionType();
          if (type != null) {
            PsiClassType psiClassType = JavaPsiFacade.getElementFactory(host.getProject()).createType(definedInUClass.getJavaPsi());
            if (type.isAssignableFrom(psiClassType)) {
              Set<String> parentUrlPaths = getParentUrlPaths(entry.getValue().getSourcePsi());
              if (parentUrlPaths.isEmpty()) {
                allParentPaths.add(urlDefinition);
              }
              else {
                for (String path : parentUrlPaths) {
                  if (StringUtil.isNotEmpty(path)) {
                    allParentPaths.add(path + urlDefinition);
                  }
                }
              }
            }
          }
        }
      }
    }
    return allParentPaths;
  }

  private static @NotNull Map<String, UExpression> mapUrlsToServiceInvocations(@NotNull Module module, @NotNull PsiMethod registerMethod) {
    // todo !! cache it
    Map<String, UExpression> resultMap = new HashMap<>();
    for (UCallExpression uCallExpression : getUCallExpressions(getRoutingClassReferencesScope(module), registerMethod)) {
      List<UExpression> valueArguments = uCallExpression.getValueArguments();
      if (valueArguments.size() != 2) continue;
      String expressionText = getUExpressionText(valueArguments.get(0));
      if (expressionText != null) {
        resultMap.put(expressionText, valueArguments.get(1));
      }
    }
    return resultMap;
  }

  public static @NotNull Map<UastSmartPointer<UCallExpression>, PsiType> getServiceRegisterInvocations(@NotNull Module module) {
    Map<UastSmartPointer<UCallExpression>, PsiType> resultMap = new HashMap<>();
    GlobalSearchScope scope = getRoutingClassReferencesScope(module);
    for (PsiMethod registerMethod : getBuilderRegisterMethod(module)) {
      for (UCallExpression uCallExpression : getUCallExpressions(scope, registerMethod)) {
        PsiType serviceType = getRegisteredServiceType(uCallExpression);
        if (serviceType != null) {
          resultMap.put(new UastSmartPointer<>(uCallExpression, UCallExpression.class), serviceType);
        }
      }
    }
    return resultMap;
  }

  private static @NotNull Set<UCallExpression> getUCallExpressions(@NotNull SearchScope scope, @NotNull PsiMethod psiMethod) {
    if (!psiMethod.isValid()) return Collections.emptySet();
    Map<SearchScope, Set<UCallExpression>> value = CachedValuesManager.getManager(psiMethod.getProject())
      .getCachedValue(psiMethod, METHOD_INVOCATIONS_KEY, () -> {
        return Result.create(createMethodsInScopeMap(psiMethod),
                             UastModificationTracker.getInstance(psiMethod.getProject()),
                             JavaLibraryModificationTracker.getInstance(psiMethod.getProject()));
      }, false);
    return value.get(scope);
  }

  private static @NotNull Map<SearchScope, Set<UCallExpression>> createMethodsInScopeMap(@NotNull PsiMethod psiMethod) {
    return ConcurrentFactoryMap.createMap(forScope -> {
            Set<UCallExpression> expressions = MethodReferencesSearch.search(psiMethod, forScope, true).findAll().stream()
              .map(reference -> UastContextKt.getUastParentOfType(reference.getElement(), UCallExpression.class))
              .filter(Objects::nonNull)
              .collect(Collectors.toSet());
            return expressions;
          });
  }

  public static @Nullable PsiType getRegisteredServiceType(@NotNull UCallExpression callExpression) {
    List<UExpression> arguments = callExpression.getValueArguments();
    return arguments.size() == 2 ? arguments.get(1).getExpressionType() : null;
  }

  public static boolean processBuilderRegisterMethodsWithProgress(@NotNull Processor<? super HelidonUrlTargetInfo> processor,
                                                                  @NotNull GlobalSearchScope scope,
                                                                  @NotNull Module module) {
    return processBuilderRegisterMethods(processor, scope, module);
  }

  public static boolean processBuilderRegisterMethods(@NotNull Processor<? super HelidonUrlTargetInfo> processor,
                                                      @NotNull GlobalSearchScope scope, @NotNull Module module) {
    for (PsiMethod registerMethod : getBuilderRegisterMethod(module)) {
      if (!findAndProcessTargets(processor, scope, registerMethod, HelidonRequestMethods.REGISTER, 0)) {
        return false;
      }
    }
    return true;
  }

  public static boolean processRulesHttpMethods(@NotNull Processor<? super HelidonUrlTargetInfo> processor,
                                                @NotNull SearchScope scope,
                                                @Nullable Module module) {
    if (module == null) return true;
    for (Pair<PsiMethod, HelidonRequestMethods> rulesMethod : getRulesHttpMethods(module)) {
      if (!findAndProcessTargets(processor, scope, rulesMethod.first, rulesMethod.second, 0)) return false;
    }
    return true;
  }

  public static boolean processBuilderHttpMethods(@NotNull Processor<? super HelidonUrlTargetInfo> processor,
                                                  @NotNull SearchScope scope,
                                                  @Nullable Module module) {
    if (module == null) return true;
    for (Pair<PsiMethod, HelidonRequestMethods> rulesMethod : getBuilderHttpMethods(module)) {
      if (!findAndProcessTargets(processor, scope, rulesMethod.first, rulesMethod.second, 0)) return false;
    }
    return true;
  }

  private static boolean findAndProcessTargets(@NotNull Processor<? super HelidonUrlTargetInfo> processor,
                                               @NotNull SearchScope scope,
                                               @NotNull PsiMethod psiMethod,
                                               @NotNull HelidonRequestMethods requestMethods,
                                               int expressionNum) {
    for (UExpression expression : findExpressions(psiMethod, scope, expressionNum)) {
      if (!processExpressions(processor, requestMethods, expression)) return false;
    }
    return true;
  }

  private static boolean processExpressions(@NotNull Processor<? super HelidonUrlTargetInfo> processor,
                                            @NotNull HelidonRequestMethods requestMethods,
                                            @NotNull UExpression expression) {
    String expressionText = getUExpressionText(expression);
    if (expressionText != null) {
      if (!processTargets(processor, expression, expressionText, requestMethods, getParentUrlPaths(expression.getSourcePsi()))) {
        return false;
      }
    }
    else {
      // if UStringConcatenationsFacade failed to process)))
      PsiElement javaPsi = expression.getJavaPsi();
      if (javaPsi instanceof PsiExpression && !processJavaStringExpressions(processor, requestMethods, (PsiExpression)javaPsi)) {
        return false;
      }
    }
    return true;
  }

  private static @Nullable String getUExpressionText(UExpression expression) {
    return UastUtils.evaluateString(expression);
  }

  private static boolean processJavaStringExpressions(@NotNull Processor<? super HelidonUrlTargetInfo> processor,
                                                      @NotNull HelidonRequestMethods requestMethods,
                                                      @NotNull PsiExpression expression) {
    Pair<PsiElement, String> pair = StringExpressionHelper.evaluateExpression(expression);
    if (pair != null) {
      UElement uElement = UastContextKt.toUElement(pair.first);
      if (uElement instanceof UExpression &&
          !processTargets(processor, (UExpression)uElement, pair.second, requestMethods, getParentUrlPaths(pair.first))) {
        return false;
      }
    }
    return true;
  }

  private static @NotNull Set<UExpression> findExpressions(@NotNull PsiMethod psiMethod,
                                                           @NotNull SearchScope scope,
                                                           int expNum) {
    return getUCallExpressions(scope, psiMethod).stream().
      map(uCallExpression -> uCallExpression.getArgumentForParameter(expNum))
      .filter(Objects::nonNull).collect(Collectors.toSet());
  }

  private static boolean processTargets(@NotNull Processor<? super HelidonUrlTargetInfo> processor,
                                        @NotNull UExpression resolveTo,
                                        @NotNull String url,
                                        HelidonRequestMethods requestMethods,
                                        @NotNull Set<String> parentUrlPaths) {

    PsiElement psiElement = resolveTo.getSourcePsi();
    if (psiElement == null) return true;
    if (parentUrlPaths.isEmpty()) {
      return processor.process(HelidonUrlTargetInfo.create(url, psiElement).ofType(requestMethods));
    }
    for (String parentUrl : parentUrlPaths) {
      if (!processor.process(HelidonUrlTargetInfo.create(url, psiElement)
                               .withParentUrl(parentUrl)
                               .ofType(requestMethods))) {
        return false;
      }
    }
    return true;
  }

  private static @NotNull Collection<Pair<PsiMethod, HelidonRequestMethods>> getRulesHttpMethods(@NotNull Module module) {
    return CachedValuesManager.getManager(module.getProject())
      .getCachedValue(module, () -> Result.createSingleDependency(getHttpMethods(module, HelidonConstants.ROUTING_RULES),
                                                                  JavaLibraryModificationTracker.getInstance(module.getProject())));
  }

  private static @NotNull Collection<Pair<PsiMethod, HelidonRequestMethods>> getBuilderHttpMethods(@NotNull Module module) {
    return CachedValuesManager.getManager(module.getProject())
      .getCachedValue(module, () -> Result.createSingleDependency(getHttpMethods(module, HelidonConstants.ROUTING_BUILDER),
                                                                  JavaLibraryModificationTracker.getInstance(module.getProject())));
  }

  private static @NotNull Collection<Pair<PsiMethod, HelidonRequestMethods>> getHttpMethods(@NotNull Module module, @NotNull String containerClass) {
    PsiClass routingBuilderClass =
      JavaPsiFacade.getInstance(module.getProject()).findClass(containerClass, module.getModuleRuntimeScope(true));

    if (routingBuilderClass == null) return Collections.emptySet();

    return Arrays.stream(routingBuilderClass.getMethods()).filter(method -> {
      return getHttpMethodsPattern().accepts(method) ||
             getAnyOfMethodPattern().accepts(method);
    }).map(method -> Pair.create(method, HelidonRequestMethods.getTypeByMethodName(method.getName()))).collect(Collectors.toSet());
  }

  private static @NotNull Set<PsiMethod> getBuilderRegisterMethod(@NotNull Module module) {
    return CachedValuesManager.getManager(module.getProject())
      .getCachedValue(module, () -> Result.createSingleDependency(getRegisterMethod(module),
                                                                  JavaLibraryModificationTracker.getInstance(module.getProject())));
  }

  private static @NotNull Set<PsiMethod> getRegisterMethod(@NotNull Module module) {
    Set<PsiMethod> methods = new HashSet<>();
    String[] registerClasses = {HelidonConstants.ROUTING_RULES}; //HelidonConstants.ROUTING_BUILDER

    for (String registerClass : registerClasses) {

      PsiClass routingBuilderClass =
        JavaPsiFacade.getInstance(module.getProject()).findClass(registerClass, module.getModuleRuntimeScope(true));

      if (routingBuilderClass == null) continue;
      for (PsiMethod psiMethod : routingBuilderClass.findMethodsByName("register", false)) {
        if (getRegisterMethodPattern().accepts(psiMethod)) {
          methods.add(psiMethod);
          break;
        }
      }
    }
    return methods;
  }

  public static @NotNull GlobalSearchScope getRoutingClassReferencesScope(@NotNull Module module) {
    return CachedValuesManager.getManager(module.getProject())
      .getCachedValue(module, () -> Result.create(calculateRoutingClassReferencesScope(module),
                                                  UastModificationTracker.getInstance(module.getProject()),
                                                  JavaLibraryModificationTracker.getInstance(module.getProject())));
  }

  private static GlobalSearchScope calculateRoutingClassReferencesScope(@NotNull Module module) {
    PsiClass routingClass =
      JavaPsiFacade.getInstance(module.getProject()).findClass(HelidonConstants.ROUTING, module.getModuleRuntimeScope(true));

    if (routingClass == null) return GlobalSearchScope.EMPTY_SCOPE;

    Set<VirtualFile> virtualFiles = ReferencesSearch.search(routingClass, module.getModuleWithDependenciesScope()).findAll().stream()
      .map(reference -> reference.getElement().getContainingFile().getVirtualFile())
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    return virtualFiles.isEmpty() ? GlobalSearchScope.EMPTY_SCOPE : GlobalSearchScope.filesScope(module.getProject(), virtualFiles);
  }
}
