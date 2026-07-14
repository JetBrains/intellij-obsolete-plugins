// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.GuiceIcons;
import com.intellij.guice.model.renderers.GuiceEntryTargetRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UastContextKt;

import java.util.*;

/**
 * Class-level gutter icon: shows which {@code bind()} calls reference this class.
 *
 * <p>Uses the unified {@link GuiceNavigationIndex}: constructs a {@link GuiceBindingKey}
 * for the class type and finds all {@link EntryRole#BINDING_SITE} entries with a matching key.
 */
public final class GuiceBindingClassAnnotator extends RelatedItemLineMarkerProvider {

  @Override
  public void collectNavigationMarkers(@NotNull List<? extends PsiElement> elements,
                                       @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result,
                                       boolean forNavigation) {
    if (elements.isEmpty()) return;
    Module module = ModuleUtilCore.findModuleForPsiElement(elements.getFirst());
    if (module == null) return;
    GuiceProjectModel model = GuiceProjectModel.getInstance(module.getProject());
    if (!model.isGuiceAvailable(module)) return;
    super.collectNavigationMarkers(elements, result, forNavigation);
  }

  @Override
  protected void collectNavigationMarkers(@NotNull PsiElement psiElement,
                                          @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    final PsiElement parent = psiElement.getParent();
    if (!(parent instanceof PsiNameIdentifierOwner nio)
        || !psiElement.equals(nio.getNameIdentifier())) {
      return;
    }

    final UElement uElement = UastContextKt.toUElement(parent);
    if (!(uElement instanceof UClass uClass)) return;
    final PsiClass psiClass = uClass.getJavaPsi();
    if (psiClass.getQualifiedName() == null) return;

    final Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    if (module == null) return;

    GuiceProjectModel model = GuiceProjectModel.getInstance(module.getProject());
    if (!model.isGuiceAvailable(module)) return;

    // Find all BINDING_SITE entries whose type matches this class.
    PsiType classType = JavaPsiFacade.getElementFactory(psiClass.getProject()).createType(psiClass);
    GuiceBindingKey key = new GuiceBindingKey(classType);
    GuiceNavigationIndex navIndex = model.getNavigationIndex(module);
    Set<GuiceEntry> bindings = navIndex.findByKey(key, EntryRole.BINDING_SITE);

    if (!bindings.isEmpty()) {
      Map<PsiElement, GuiceEntry> entryByTarget = new HashMap<>();
      List<PsiElement> targets = new ArrayList<>(bindings.size());
      for (GuiceEntry binding : bindings) {
        PsiElement target = binding.getNavigationTarget();
        if (target == null) continue;

        // Skip self-references: @Inject constructors inside this class create
        // a BINDING_SITE for the class's own type, but navigating from the
        // class declaration to its own constructor is redundant.
        if (target instanceof PsiMember member
            && psiClass.equals(member.getContainingClass())) {
          continue;
        }

        targets.add(target);
        entryByTarget.put(target, binding);
      }
      if (!targets.isEmpty()) {
        final NavigationGutterIconBuilder<PsiElement> builder =
          NavigationGutterIconBuilder.create(GuiceIcons.GoogleSmall).
            setPopupTitle(GuiceBundle.message("GuiceClassAnnotator.popup.title")).
            setTooltipText(GuiceBundle.message("GuiceClassAnnotator.popup.tooltip.text")).
            setTargetRenderer(() -> new GuiceEntryTargetRenderer(entryByTarget)).
            setTargets(targets);

        result.add(NonPersistentLineMarkerInfo.createFrom(builder, psiElement));
      }
    }
  }
}
