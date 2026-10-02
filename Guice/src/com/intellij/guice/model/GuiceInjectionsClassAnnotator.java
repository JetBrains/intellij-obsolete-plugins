// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.model.extensions.GuiceBindingContributor;
import com.intellij.guice.model.extensions.GuiceExtensionIndex;
import com.intellij.guice.model.renderers.GuiceEntryTargetRenderer;
import com.intellij.java.ultimate.icons.JavaUltimateIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiReferenceExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides gutter icons for Guice injection points and binding sites.
 *
 * <p>Uses the unified {@link GuiceNavigationIndex} which guarantees symmetric navigation:
 * if you can navigate A→B, you can always navigate B→A. The single matching method
 * {@link GuiceNavigationIndex#findCounterparts} is used for both directions.
 *
 * <p>Flow:
 * <ol>
 *   <li>Resolve leaf PSI element to its owner (field, param, method, call expression)</li>
 *   <li>Create {@link GuiceEntry} instances for the owner via {@link GuiceEntryProducer}</li>
 *   <li>Call {@link GuiceNavigationIndex#findCounterparts} for each entry</li>
 *   <li>Create gutter icons from the counterpart navigation targets</li>
 * </ol>
 */
public final class GuiceInjectionsClassAnnotator extends GuiceLineMarkerProviderBase {

  /**
   * Method names that represent Guice call-site identifiers in the source.
   */
  private static @NotNull Set<String> getGuiceCallNames() {
    return GuiceExtensionIndex.get().getAllCallNames();
  }

  // -----------------------------------------------------------------------
  // Entry points
  // -----------------------------------------------------------------------

  @Override
  protected void collectNavigationMarkers(@NotNull PsiElement psiElement,
                                          @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    // Quick-reject: only process elements that could be Guice-relevant.
    PsiElement owner = resolveAnnotatableOwner(psiElement);
    if (owner == null) return;

    final Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    if (module == null) return;

    GuiceProjectModel model = GuiceProjectModel.getInstance(module.getProject());
    GuiceNavigationIndex navIndex = model.getNavigationIndex(module);

    // Look up pre-computed entries from the index by their gutter anchor.
    // For declarations, the producer stores the source PSI declaration via UAST
    // (PsiMethod for Java, KtNamedFunction for Kotlin, etc.) and resolveAnnotatableOwner
    // returns the same parent element. For calls (bind, to, etc.), both sides use the leaf.
    boolean isDeclaration = (owner == psiElement.getParent());
    PsiElement anchor = isDeclaration ? owner : psiElement;
    Set<GuiceEntry> entries = navIndex.findEntriesByAnchor(anchor);
    if (entries.isEmpty()) return;

    Set<GuiceEntry> injectionPointCounterparts = new LinkedHashSet<>();
    Set<GuiceEntry> bindingSiteCounterparts = new LinkedHashSet<>();

    for (GuiceEntry entry : entries) {
      Set<GuiceEntry> counterparts = navIndex.findCounterparts(entry);
      if (counterparts.isEmpty() && entry.getRole() == EntryRole.INJECTION_POINT && !entry.isImplementationReference()) {
        // Guice uses a JIT binding only when the key has no explicit binding.
        GuiceEntry jitBinding = GuiceJitBindings.findConstructorBinding(entry.getKey(), psiElement);
        if (jitBinding != null) counterparts = Set.of(jitBinding);
      }
      if (counterparts.isEmpty()) continue;

      Set<GuiceEntry> targetSet =
          entry.getRole() == EntryRole.INJECTION_POINT ? injectionPointCounterparts : bindingSiteCounterparts;
      for (GuiceEntry cp : counterparts) {
        if (cp.getNavigationTarget() != null) {
          targetSet.add(cp);
        }
      }
    }

    if (!injectionPointCounterparts.isEmpty()) {
      addGutterIcon(result, new ArrayList<>(injectionPointCounterparts), EntryRole.INJECTION_POINT, psiElement);
    }
    if (!bindingSiteCounterparts.isEmpty()) {
      addGutterIcon(result, new ArrayList<>(bindingSiteCounterparts), EntryRole.BINDING_SITE, psiElement);
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
   *   <li><b>Kotlin constructor keyword</b>: detected via UAST fallback.</li>
   *   <li><b>Guice call identifiers</b> ({@code bind()}, {@code .to()},
   *       {@code .toProvider()}, {@code .getProvider()}, and any call recognized
   *       by {@link GuiceBindingContributor} EPs such as {@code build()}): returns
   *       the call expression PSI element.</li>
   * </ul>
   */
  private static @Nullable PsiElement resolveAnnotatableOwner(@NotNull PsiElement leafElement) {
    // Only process true leaf elements (PsiIdentifier, PsiKeyword, KtNameIdentifier, etc.).
    // The framework passes ALL elements, not just leaves — skip composite nodes like
    // PsiReferenceExpression to avoid duplicate gutter icons.
    if (leafElement.getFirstChild() != null) return null;

    PsiElement parent = leafElement.getParent();
    if (parent == null) return null;

    // 1. Declaration identifiers (field, parameter, method, class name).
    if (parent instanceof PsiNameIdentifierOwner nameOwner
        && leafElement.equals(nameOwner.getNameIdentifier())) {
      return parent;
    }

    // 1b. Kotlin constructor keyword: KtPrimaryConstructor.getNameIdentifier()
    //     returns null, so the check above misses it.
    if ("constructor".equals(leafElement.getText())) {
      UElement u = UastContextKt.toUElement(parent, UMethod.class);
      if (u instanceof UMethod um && um.isConstructor()) {
        return parent;
      }
    }

    // 2. Guice call identifiers (bind, to, build, newMapBinder, etc.).
    if (!getGuiceCallNames().contains(leafElement.getText())) {
      return null;
    }
    return resolveCallOwner(leafElement, parent);
  }

  /**
   * Resolves the owning call expression for a method-call identifier like {@code to} in
   * {@code bind(Foo.class).to(Bar.class)} or a constructor call like {@code new ExperimentFlagModule(...)}.
   */
  private static @Nullable PsiElement resolveCallOwner(@NotNull PsiElement leafElement,
                                                       @NotNull PsiElement parent) {
    // Java: PsiIdentifier → PsiReferenceExpression → PsiMethodCallExpression
    if (parent instanceof PsiReferenceExpression refExpr
        && refExpr.getParent() instanceof PsiMethodCallExpression methodCall
        && leafElement.equals(refExpr.getReferenceNameElement())) {
      return methodCall;
    }

    // Java constructor call: PsiIdentifier → PsiJavaCodeReferenceElement → PsiNewExpression
    if (parent instanceof PsiJavaCodeReferenceElement classRef
        && classRef.getParent() instanceof PsiNewExpression newExpr
        && leafElement.equals(classRef.getReferenceNameElement())) {
      return newExpr;
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
  // Gutter icon creation (unified for both directions)
  // -----------------------------------------------------------------------

  /**
   * Creates a gutter icon appropriate for the entry's role:
   * <ul>
   *   <li>INJECTION_POINT → "navigate to binding" icon</li>
   *   <li>BINDING_SITE → "navigate to injection point" icon</li>
   * </ul>
   */
  private static void addGutterIcon(@NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                    @NotNull List<GuiceEntry> counterparts,
                                    @NotNull EntryRole role,
                                    @NotNull PsiElement anchor) {
    List<GuiceEntry> renderedEntries = new ArrayList<>(counterparts.size());
    List<PsiElement> targets = new ArrayList<>(counterparts.size());
    for (GuiceEntry cp : counterparts) {
      PsiElement target = cp.getNavigationTarget();
      if (target != null) {
        targets.add(target);
        renderedEntries.add(cp);
      }
    }

    NavigationGutterIconBuilder<PsiElement> builder;
    if (role == EntryRole.INJECTION_POINT) {
      // This element is an injection point → navigate to its bindings
      builder = NavigationGutterIconBuilder
          .create(JavaUltimateIcons.Cdi.Gutter.ShowAutowiredCandidates, GuiceBundle.GUICE)
          .setPopupTitle(GuiceBundle.message("GuiceClassAnnotator.popup.title"))
          .setTooltipText(GuiceBundle.message("GuiceClassAnnotator.popup.tooltip.text"));
    }
    else {
      // This element is a binding site → navigate to injection points
      builder = NavigationGutterIconBuilder
          .create(JavaUltimateIcons.Cdi.Gutter.ShowAutowiredDependencies, GuiceBundle.GUICE)
          .setPopupTitle(GuiceBundle.message("gutter.choose.injected.point"))
          .setTooltipText(GuiceBundle.message("gutter.navigate.to.injection.point"));
    }

    builder
        .setTargets(targets)
        .setTargetRenderer(() -> new GuiceEntryTargetRenderer(renderedEntries));
    result.add(NonPersistentLineMarkerInfo.createFrom(builder, anchor));
  }
}