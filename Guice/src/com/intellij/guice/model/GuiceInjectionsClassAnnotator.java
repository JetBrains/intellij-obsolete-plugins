// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.model.renderers.GuiceEntryTargetRenderer;
import com.intellij.guice.model.extensions.GuiceBindingContributor;

import com.intellij.java.ultimate.icons.JavaUltimateIcons;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.*;

import java.util.*;

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
public final class GuiceInjectionsClassAnnotator extends RelatedItemLineMarkerProvider {

  /**
   * Method names that represent Guice call-site identifiers in the source.
   * Computed dynamically from all {@link GuiceBindingContributor} EPs' binding words,
   * plus {@code getProvider} (an injection-point call, not a binding contributor word).
   */
  private static Set<String> getGuiceCallNames() {
    Set<String> names = new HashSet<>();
    names.add("getProvider"); // not a binding word, but a call we want gutters on
    for (GuiceBindingContributor c : GuiceBindingContributor.EP_NAME.getExtensionList()) {
      names.addAll(c.getBindingWords());
    }
    return names;
  }

  // -----------------------------------------------------------------------
  // Entry points
  // -----------------------------------------------------------------------

  @Override
  public void collectNavigationMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                       boolean forNavigation) {
    if (elements.isEmpty()) return;
    Module module = ModuleUtilCore.findModuleForPsiElement(elements.getFirst());
    if (module == null) return;
    GuiceProjectModel model = GuiceProjectModel.getInstance(module.getProject());
    if (!model.isGuiceAvailable(module)) return;

    // Re-index the current file inline (cancellable) for immediate feedback.
    // This ensures the file the user is editing has fresh entries in the
    // navigation index, without waiting for the background debounced processing.
    PsiFile psiFile = elements.getFirst().getContainingFile();
    if (psiFile != null) {
      model.reindexCurrentFile(psiFile);
    }

    super.collectNavigationMarkers(elements, result, forNavigation);
  }

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

    for (GuiceEntry entry : entries) {
      Set<GuiceEntry> counterparts = navIndex.findCounterparts(entry);
      if (counterparts.isEmpty()) continue;

      // Filter to valid entries with resolvable navigation targets.
      List<GuiceEntry> validCounterparts = new ArrayList<>();
      for (GuiceEntry cp : counterparts) {
        if (cp.getNavigationTarget() != null) validCounterparts.add(cp);
      }
      if (validCounterparts.isEmpty()) continue;

      addGutterIcon(result, validCounterparts, entry.getRole(), psiElement);
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
   * {@code bind(Foo.class).to(Bar.class)}.
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
    // Build PsiElement target list + entry lookup for the renderer.
    Map<PsiElement, GuiceEntry> entryByTarget = new HashMap<>();
    List<PsiElement> targets = new ArrayList<>(counterparts.size());
    for (GuiceEntry cp : counterparts) {
      PsiElement target = cp.getNavigationTarget();
      if (target != null) {
        targets.add(target);
        entryByTarget.put(target, cp);
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
        .setTargetRenderer(() -> new GuiceEntryTargetRenderer(entryByTarget));
    result.add(NonPersistentLineMarkerInfo.createFrom(builder, anchor));
  }
}