// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.GuiceIcons;
import com.intellij.guice.model.renderers.GuiceEntryTargetRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.UAnnotation;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class-level gutter icon: shows which {@code bind()} calls reference this class.
 *
 * <p>Uses the unified {@link GuiceNavigationIndex}: constructs a {@link GuiceBindingKey}
 * for the class type and finds all {@link EntryRole#BINDING_SITE} entries with a matching key.
 * It also finds the implementation references, for example {@code bind(Foo.class).to(ThisClass.class)}.
 */
public final class GuiceBindingClassAnnotator extends GuiceLineMarkerProviderBase {

  @Override
  public @NotNull String getId() {
    return "guice.bound.class";
  }

  @Override
  public @NotNull String getName() {
    return GuiceBundle.message("gutter.option.bound.class");
  }

  @Override
  public @NotNull Icon getIcon() {
    return GuiceIcons.GoogleSmall;
  }

  @Override
  protected void collectNavigationMarkers(@NotNull PsiElement psiElement,
                                          @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
    final PsiElement parent = psiElement.getParent();
    if (!(parent instanceof PsiNameIdentifierOwner nio)
        || !psiElement.equals(nio.getNameIdentifier())) {
      return;
    }

    final UClass uClass = UastContextKt.toUElement(parent, UClass.class);
    if (uClass == null) return;
    final PsiClass psiClass = uClass.getJavaPsi();
    if (psiClass.getQualifiedName() == null) return;

    final Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    if (module == null || !GuiceProjectModel.isGuiceAvailable(module)) return;

    GuiceProjectModel model = GuiceProjectModel.getInstance(module.getProject());

    // Find all BINDING_SITE entries whose type matches this class.
    GuiceBindingKey key = GuiceBindingKey.forClass(psiClass);
    GuiceNavigationIndex navIndex = model.getNavigationIndex(module);
    Set<GuiceEntry> bindings = new HashSet<>(navIndex.findByKey(key, EntryRole.BINDING_SITE));
    // Bindings that use this class as the implementation: bind(Foo.class).to(ThisClass.class).
    for (GuiceEntry entry : navIndex.findByKey(key, EntryRole.INJECTION_POINT)) {
      if (entry.isImplementationReference()) bindings.add(entry);
    }

    if (!bindings.isEmpty()) {
      List<GuiceEntry> filteredEntries = new ArrayList<>(bindings.size());
      for (GuiceEntry binding : bindings) {
        PsiElement target = binding.getNavigationTarget();
        if (target == null) continue;

        // Skip self-references: an @Inject constructor or an @ImplementedBy annotation of this class
        // is a BINDING_SITE for the class's own type, but navigating from the
        // class declaration to it is redundant.
        if (isOwnConstructorOrAnnotation(target, psiClass)) {
          continue;
        }

        filteredEntries.add(binding);
      }
      GuiceEntryTargetRenderer.SortedTargets sorted = GuiceEntryTargetRenderer.sortTargets(filteredEntries, psiElement);
      if (!sorted.targets().isEmpty()) {
        List<GuiceEntry> immutableEntries = sorted.entries();
        final GuiceGutterIconBuilder builder = new GuiceGutterIconBuilder(GuiceIcons.GoogleSmall);
        builder
          .setPopupTitle(GuiceBundle.message("GuiceClassAnnotator.popup.title"))
          .setTooltipText(GuiceBundle.message("GuiceClassAnnotator.popup.tooltip.text"))
          .setTargetRenderer(() -> new GuiceEntryTargetRenderer(immutableEntries))
          .setTargets(sorted.targets());

        result.add(builder.createGuiceLineMarkerInfo(psiElement));
      }
    }
  }

  private static boolean isOwnConstructorOrAnnotation(@NotNull PsiElement target, @NotNull PsiClass psiClass) {
    UElement uTarget = UastContextKt.toUElement(target);
    if (!(uTarget instanceof UMethod) && !(uTarget instanceof UAnnotation)) return false;
    UClass owner = UastUtils.getParentOfType(uTarget, UClass.class);
    return owner != null && psiClass.getManager().areElementsEquivalent(owner.getJavaPsi(), psiClass);
  }
}
