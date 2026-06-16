// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.beans.JitBindDescriptor;
import com.intellij.guice.model.jam.GuiceProvides;
import com.intellij.guice.model.renderers.GuiceBindingClassTargetPresentationRenderer;
import com.intellij.guice.model.renderers.GuiceInjectionPointTargetPresentationRenderer;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.java.ultimate.icons.JavaUltimateIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class GuiceInjectionsClassAnnotator extends RelatedItemLineMarkerProvider {

  /** Method names that represent Guice call-site injection points. */
  private static final Set<String> GUICE_CALL_NAMES = Set.of("to", "toProvider", "getProvider");

  // -----------------------------------------------------------------------
  // Entry points
  // -----------------------------------------------------------------------

  @Override
  public void collectNavigationMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                       boolean forNavigation) {
    if (elements.isEmpty()) return;
    Module module = ModuleUtilCore.findModuleForPsiElement(elements.get(0));
    if (module == null) return;
    GuiceProjectModel model = GuiceProjectModel.getInstance(module.getProject());
    if (!model.isGuiceAvailable(module)) return;
    super.collectNavigationMarkers(elements, result, forNavigation);
  }

  @Override
  protected void collectNavigationMarkers(@NotNull PsiElement psiElement,
                                          @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    PsiElement owner = resolveAnnotatableOwner(psiElement);
    if (owner == null) return;

    final Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    if (module == null) return;

    final UElement uElement = UastContextKt.toUElement(owner);
    if (uElement == null) return;

    // Obtain the index once — it's cached per-module, so this is O(1).
    GuiceLiveIndex index = GuiceProjectModel.getInstance(module.getProject()).getIndex(module);

    // Dispatch by UAST element type.
    // CRITICAL: Use UAST, not Java PSI, so that Kotlin declarations are handled correctly.
    switch (uElement) {
      case UField uField -> annotateInjectField(result, index, uField, psiElement);
      case UParameter uParameter -> annotateInjectParameter(result, index, uParameter, psiElement);
      case UMethod uMethod -> annotateMethod(result, index, uMethod, owner, psiElement);
      default -> annotateCallSiteInjectionPoint(result, index, owner, psiElement);
    }
  }

  // -----------------------------------------------------------------------
  // Owner resolution: leaf PSI element → annotatable owner
  // -----------------------------------------------------------------------

  /**
   * For a leaf PSI element (identifier), determines the "owner" element to annotate.
   *
   * <ul>
   *   <li><b>Declaration identifiers</b> (field, param, method, class name): returns
   *       the declaring {@link PsiNameIdentifierOwner}.</li>
   *   <li><b>Guice call identifiers</b> ({@code .to()}, {@code .toProvider()},
   *       {@code .getProvider()}): returns the call expression PSI element.</li>
   * </ul>
   *
   * Works across all JVM languages (Java, Kotlin) via a UAST fallback for call
   * detection.
   *
   * @return the owner element, or {@code null} if this leaf is not annotatable
   */
  private static @Nullable PsiElement resolveAnnotatableOwner(@NotNull PsiElement leafElement) {
    PsiElement parent = leafElement.getParent();
    if (parent == null) return null;

    // 1. Declaration identifiers (field, parameter, method, class name).
    if (parent instanceof PsiNameIdentifierOwner nameOwner
        && leafElement.equals(nameOwner.getNameIdentifier())) {
      return parent;
    }

    // 1b. Kotlin constructor keyword: KtPrimaryConstructor.getNameIdentifier()
    //     returns null, so the check above misses it.  Use a cheap text
    //     pre-filter to avoid UAST conversion on irrelevant tokens.
    if ("constructor".equals(leafElement.getText())) {
      UElement u = UastContextKt.toUElement(parent, UMethod.class);
      if (u instanceof UMethod um && um.isConstructor()) {
        return parent;
      }
    }

    // 2. Guice call identifiers (.to(), .toProvider(), .getProvider()).
    //    Cheap text pre-filter avoids UAST conversions for irrelevant identifiers.
    if (!GUICE_CALL_NAMES.contains(leafElement.getText())) {
      return null;
    }
    return resolveCallOwner(leafElement, parent);
  }

  /**
   * Resolves the owning call expression for a method-call identifier like {@code to} in
   * {@code bind(Foo.class).to(Bar.class)}.
   *
   * <p>Tries the Java-specific PSI path first (cheaper), then falls back to UAST for
   * Kotlin and other JVM languages.
   */
  private static @Nullable PsiElement resolveCallOwner(@NotNull PsiElement leafElement,
                                                       @NotNull PsiElement parent) {
    // Java: PsiIdentifier → PsiReferenceExpression → PsiMethodCallExpression
    if (parent instanceof PsiReferenceExpression refExpr
        && refExpr.getParent() instanceof PsiMethodCallExpression methodCall
        && leafElement.equals(refExpr.getReferenceNameElement())) {
      return methodCall;
    }

    // Kotlin/other: KtIdentifier → KtNameReferenceExpression → KtCallExpression
    PsiElement grandparent = parent.getParent();
    if (grandparent != null) {
      UElement uGP = UastContextKt.toUElement(grandparent);
      if (uGP instanceof UCallExpression) {
        return grandparent;
      }
    }

    return null;
  }

  // -----------------------------------------------------------------------
  // Per-element annotation logic
  // -----------------------------------------------------------------------

  /** {@code @Inject} field → gutter icon to matching bindings / @Provides. */
  private static void annotateInjectField(@NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                          @NotNull GuiceLiveIndex index,
                                          @NotNull UField uField,
                                          @NotNull PsiElement anchor) {
    PsiElement javaPsi = uField.getJavaPsi();
    if (javaPsi instanceof PsiField psiField
        && AnnotationUtil.isAnnotated(psiField, GuiceAnnotations.INJECTS, 0)) {
      addBindingTargetsGutterIcon(result, index, new InjectionPointDescriptor(psiField), anchor);
    }
  }

  /** {@code @Inject} method/constructor parameter → gutter icon to matching bindings / @Provides. */
  private static void annotateInjectParameter(@NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                              @NotNull GuiceLiveIndex index,
                                              @NotNull UParameter uParameter,
                                              @NotNull PsiElement anchor) {
    PsiElement javaPsi = uParameter.getJavaPsi();
    if (javaPsi instanceof PsiParameter parameter) {
      PsiElement declarationScope = parameter.getDeclarationScope();
      if (declarationScope instanceof PsiMethod method
          && (AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, 0)
              || AnnotationUtil.isAnnotated(method, GuiceAnnotations.PROVIDES_ANNOTATIONS, 0))) {
        addBindingTargetsGutterIcon(result, index, new InjectionPointDescriptor(parameter), anchor);
      }
    }
  }

  /** Method declaration: constructor injection, {@code configure()}, or {@code @Provides}. */
  private static void annotateMethod(@NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
      @NotNull GuiceLiveIndex index,
                                     @NotNull UMethod uMethod,
                                     @NotNull PsiElement owner,
                                     @NotNull PsiElement anchor) {
    PsiMethod psiMethod = uMethod.getJavaPsi();
    if (uMethod.isConstructor()) {
      if (AnnotationUtil.isAnnotated(psiMethod, GuiceAnnotations.INJECTS, 0)) {
        PsiClass psiClass = psiMethod.getContainingClass();
        if (psiClass != null) {
          BindDescriptor jitDescriptor = new JitBindDescriptor(psiMethod, psiClass);
          addInjectionPointsGutterIcon(result, index.findMatchingInjectionPoints(jitDescriptor), anchor);
        }
      }
    } else {
      if ("configure".equals(uMethod.getName())) {
        annotateBindExpressions(result, index, owner);
      }
      if (AnnotationUtil.isAnnotated(psiMethod, GuiceAnnotations.PROVIDES_ANNOTATIONS, 0)) {
        annotateProvidesMethod(result, index, psiMethod, anchor);
      }
    }
  }

  /** {@code .to()}, {@code .toProvider()}, {@code .getProvider()} → gutter icon to bindings / @Provides. */
  private static void annotateCallSiteInjectionPoint(@NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                                     @NotNull GuiceLiveIndex index,
                                                     @NotNull PsiElement owner,
                                                     @NotNull PsiElement anchor) {
    UCallExpression uCall = GuiceUtils.getCallExpression(owner);
    if (uCall != null) {
      String name = uCall.getMethodName();
      if (GUICE_CALL_NAMES.contains(name)) {
        // .to() and .toProvider() are binding declarations — the referenced class
        // is the exact binding target.  Provider-type unwrapping must be suppressed
        // (e.g., toProvider(SyncConfigBinder.class) needs a binding for SyncConfigBinder,
        // not for what SyncConfigBinder provides).
        // .getProvider() is a real injection site — Provider unwrapping is correct.
        boolean isBindingCall = "to".equals(name) || "toProvider".equals(name);
        addBindingTargetsGutterIcon(result, index, new InjectionPointDescriptor(owner, isBindingCall), anchor);
      }
    }
  }

  /** {@code bind().to()} expressions inside {@code configure()} → gutter icon to injection points. */
  private static void annotateBindExpressions(@NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                              @NotNull GuiceLiveIndex index,
                                              @NotNull PsiElement scope) {
    Set<BindDescriptor> descriptors = GuiceInjectorManager.getBindingDescriptors(scope);
    if (descriptors.isEmpty()) return;

    for (BindDescriptor descriptor : descriptors) {
      PsiElement bindExpr = descriptor.getBindExpression();
      if (bindExpr == null) continue;
      Set<InjectionPointDescriptor> matching = index.findMatchingInjectionPoints(descriptor);
      if (!matching.isEmpty()) {
        addInjectionPointsGutterIcon(result, matching, getBindingAnchor(bindExpr));
      }
    }
  }

  /** {@code @Provides} method → gutter icon to matching injection points + multibinder targets. */
  private static void annotateProvidesMethod(@NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                             @NotNull GuiceLiveIndex index,
                                             @NotNull PsiMethod psiMethod,
                                             @NotNull PsiElement anchor) {
    GuiceProvides provides = new GuiceProvides(psiMethod);

    Set<InjectionPointDescriptor> injectionPoints = index.findMatchingInjectionPoints(provides);
    List<PsiElement> targets = new ArrayList<>(ContainerUtil.mapNotNull(injectionPoints, InjectionPointDescriptor::getOwner));
    targets.addAll(index.findMultibinderTargets(psiMethod));

    if (!targets.isEmpty()) {
      NavigationGutterIconBuilder<PsiElement> builder =
        NavigationGutterIconBuilder.create(JavaUltimateIcons.Cdi.Gutter.ShowAutowiredDependencies, GuiceBundle.GUICE)
          .setTargets(targets)
          .setPopupTitle(GuiceBundle.message("gutter.choose.injected.point"))
          .setTooltipText(GuiceBundle.message("gutter.navigate.to.injection.point"))
          .setTargetRenderer(GuiceInjectionPointTargetPresentationRenderer::new);

      result.add(NonPersistentLineMarkerInfo.createFrom(builder, anchor));
    }
  }

  // -----------------------------------------------------------------------
  // Gutter icon builders (shared by multiple annotate* methods)
  // -----------------------------------------------------------------------

  /**
   * Adds a "navigate to binding / @Provides" gutter icon for an injection point.
   * Used by {@code @Inject} fields, parameters, and {@code .to()} call sites.
   */
  private static void addBindingTargetsGutterIcon(@NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                                  @NotNull GuiceLiveIndex index,
                                                  @NotNull InjectionPointDescriptor ip,
                                                  @NotNull PsiElement anchor) {
    Set<BindDescriptor> descriptors = index.findMatchingBindings(ip);
    Set<GuiceProvides> providesSet = index.findMatchingProvides(ip);

    List<PsiElement> allTargets = new ArrayList<>();
    for (BindDescriptor descriptor : descriptors) {
      PsiElement bindExpr = descriptor.getBindExpression();
      if (bindExpr != null) allTargets.add(bindExpr);
    }
    for (GuiceProvides provide : providesSet) {
      PsiElement method = provide.getPsiElement();
      if (method != null) allTargets.add(method);
    }

    if (!allTargets.isEmpty()) {
      NavigationGutterIconBuilder<PsiElement> builder =
        NavigationGutterIconBuilder.create(JavaUltimateIcons.Cdi.Gutter.ShowAutowiredCandidates, GuiceBundle.GUICE)
          .setPopupTitle(GuiceBundle.message("GuiceClassAnnotator.popup.title"))
          .setTooltipText(GuiceBundle.message("GuiceClassAnnotator.popup.tooltip.text"))
          .setTargetRenderer(GuiceBindingClassTargetPresentationRenderer::new)
          .setTargets(allTargets);

      result.add(NonPersistentLineMarkerInfo.createFrom(builder, anchor));
    }
  }

  /**
   * Adds a "navigate to injection point" gutter icon for a binding or @Provides.
   * Used by {@code configure()} bind expressions, {@code @Inject} constructors, and @Provides methods.
   */
  private static void addInjectionPointsGutterIcon(@NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                                   @NotNull Set<? extends InjectionPointDescriptor> ips,
                                                   @NotNull PsiElement anchor) {
    if (!ips.isEmpty()) {
      List<PsiElement> members = ContainerUtil.mapNotNull(ips, InjectionPointDescriptor::getOwner);
      NavigationGutterIconBuilder<PsiElement> builder =
        NavigationGutterIconBuilder.create(JavaUltimateIcons.Cdi.Gutter.ShowAutowiredDependencies, GuiceBundle.GUICE)
          .setTargets(members)
          .setPopupTitle(GuiceBundle.message("gutter.choose.injected.point"))
          .setTooltipText(GuiceBundle.message("gutter.navigate.to.injection.point"))
          .setTargetRenderer(GuiceInjectionPointTargetPresentationRenderer::new);

      result.add(NonPersistentLineMarkerInfo.createFrom(builder, anchor));
    }
  }

  // -----------------------------------------------------------------------
  // Binding anchor resolution
  // -----------------------------------------------------------------------

  private static @NotNull PsiElement getBindingAnchor(@NotNull PsiElement bindExpression) {
    UElement uElement = UastContextKt.toUElement(bindExpression);
    UCallExpression innermost = getInnermostCall(uElement);
    if (innermost != null) {
      UIdentifier methodIdentifier = innermost.getMethodIdentifier();
      if (methodIdentifier != null) {
        PsiElement sourcePsi = methodIdentifier.getSourcePsi();
        if (sourcePsi != null) {
          return sourcePsi;
        }
      }
    }
    return PsiTreeUtil.getDeepestFirst(bindExpression);
  }

  private static @Nullable UCallExpression getInnermostCall(@Nullable UElement element) {
    if (!(element instanceof UExpression)) return null;
    UExpression receiver = GuiceUtils.getQualifierExpression((UExpression)element);
    if (receiver != null) {
      UCallExpression inner = getInnermostCall(receiver);
      if (inner != null) return inner;
    }
    UExpression selector = GuiceUtils.getSelectorIfQualified(element);
    return selector instanceof UCallExpression ? (UCallExpression)selector : null;
  }
}