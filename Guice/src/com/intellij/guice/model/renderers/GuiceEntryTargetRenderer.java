// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.renderers;

import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer;
import com.intellij.guice.model.GuiceEntry;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;

/**
 * Unified target presentation renderer for Guice navigation popups.
 *
 * <p>Delegates text and icon rendering to the {@link GuiceEntry} model,
 * which computes the correct presentable text at entry creation time
 * (via an optional text provider) and extracts the icon from the
 * navigation target's PSI element.
 *
 * <p>This replaces the former separate {@code GuiceBindingClassTargetPresentationRenderer}
 * and {@code GuiceInjectionPointTargetPresentationRenderer}.
 */
public final class GuiceEntryTargetRenderer extends PsiTargetPresentationRenderer<PsiElement> {
  private final @NotNull List<GuiceEntry> myEntries;

  public GuiceEntryTargetRenderer(@NotNull Collection<GuiceEntry> entries) {
    myEntries = List.copyOf(entries);
  }

  private @Nullable GuiceEntry findEntry(@NotNull PsiElement element) {
    for (GuiceEntry entry : myEntries) {
      PsiElement target = entry.getNavigationTarget();
      if (target != null && (target.equals(element) || element.getManager().areElementsEquivalent(target, element))) {
        return entry;
      }
    }
    return null;
  }

  @Override
  public @NotNull String getElementText(@NotNull PsiElement element) {
    GuiceEntry entry = findEntry(element);
    if (entry != null) {
      String text = entry.getPresentableText();
      if (text != null) return text;
    }
    return SymbolPresentationUtil.getSymbolPresentableText(element);
  }

  @Override
  public @Nullable String getContainerText(@NotNull PsiElement element) {
    PsiFile file = element.getContainingFile();
    return file != null ? file.getName() : null;
  }

  @Override
  public @Nullable Icon getIcon(@NotNull PsiElement element) {
    GuiceEntry entry = findEntry(element);
    if (entry != null) {
      Icon icon = entry.getIcon();
      if (icon != null) return icon;
    }
    return element.getIcon(0);
  }

  @Override
  public @NotNull TargetPresentation getPresentation(@NotNull PsiElement element) {
    var builder = TargetPresentation.builder(getElementText(element))
        .icon(getIcon(element));

    String containerText = getContainerText(element);
    if (containerText != null) {
      builder = builder.containerText(containerText);
    }

    var location = PsiElementListCellRenderer.getModuleTextWithIcon(element);
    if (location != null) {
      builder = builder.locationText(location.getText(), location.getIcon());
    }

    return builder.presentation();
  }
}
