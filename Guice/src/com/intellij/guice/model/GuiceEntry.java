// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.guice.model.extensions.GuiceBindingMatchStrategy;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;
import java.util.function.Function;

/**
 * A unified representation of any navigatable element in the Guice binding graph.
 *
 * <p>Every Guice navigation participant — {@code @Inject} fields, {@code bind().to()} chains,
 * {@code @Provides} methods, JIT constructors, multibinder contributions — is described by
 * the same structure:
 *
 * <ul>
 *   <li>{@link #getKey()} — <b>WHAT</b> type+qualifier is being bound/injected</li>
 *   <li>{@link #getNavigationTarget()} — <b>WHERE</b> clicking the gutter takes you</li>
 *   <li>{@link #getGutterAnchor()} — <b>WHERE</b> the gutter icon is placed</li>
 *   <li>{@link #getRole()} — whether this is an {@link EntryRole#INJECTION_POINT} or {@link EntryRole#BINDING_SITE}</li>
 *   <li>{@link #getPresentableText()} — <b>HOW</b> this entry is displayed in navigation popups</li>
 *   <li>{@link #getIcon()} — the icon representing this entry's target element</li>
 * </ul>
 *
 * <p><b>Matching rule</b>: two entries are counterparts when they have the same key
 * ({@link GuiceBindingKey#matches}) and <b>opposite</b> roles. This guarantees symmetric
 * navigation by construction.
 *
 * @see GuiceNavigationIndex#findCounterparts
 */
public final class GuiceEntry {
  private final @NotNull GuiceBindingKey myKey;
  private final @NotNull SmartPsiElementPointer<PsiElement> myNavigationTarget;
  private final @NotNull SmartPsiElementPointer<PsiElement> myGutterAnchor;
  private final @NotNull EntryRole myRole;
  private final @NotNull Function<PsiElement, String> myTextProvider;
  private final @Nullable GuiceBindingMatchStrategy myStrategy;

  /**
   * Creates a new entry with a custom text provider for navigation popups.
   *
   * @param key              the binding key (type + optional qualifier)
   * @param navigationTarget where clicking the gutter icon takes you
   * @param gutterAnchor     where the gutter icon is placed
   * @param role             whether this is an injection point or binding site
   * @param textProvider     optional function that produces the display text from the
   *                         navigation target element; when {@code null},
   *                         {@link SymbolPresentationUtil#getSymbolPresentableText} is used
   */
  public GuiceEntry(@NotNull GuiceBindingKey key,
                    @NotNull PsiElement navigationTarget,
                    @NotNull PsiElement gutterAnchor,
                    @NotNull EntryRole role,
                    @NotNull Function<PsiElement, String> textProvider) {
    this(key, navigationTarget, gutterAnchor, role, textProvider, null);
  }

  /**
   * Full constructor with strategy association.
   *
   * @param strategy the {@link GuiceBindingMatchStrategy} that produced this entry,
   *                 or {@code null} for core entries (standard @Provides, @Inject, bind().to())
   */
  public GuiceEntry(@NotNull GuiceBindingKey key,
                    @NotNull PsiElement navigationTarget,
                    @NotNull PsiElement gutterAnchor,
                    @NotNull EntryRole role,
                    @Nullable Function<PsiElement, String> textProvider,
                    @Nullable GuiceBindingMatchStrategy strategy) {
    myKey = key;
    myNavigationTarget = SmartPointerManager.createPointer(navigationTarget);
    myGutterAnchor = SmartPointerManager.createPointer(gutterAnchor);
    myRole = role;
    myTextProvider = textProvider;
    myStrategy = strategy;
  }

  public @NotNull GuiceBindingKey getKey() {
    return myKey;
  }

  /**
   * Returns where clicking the gutter icon takes you,
   * or {@code null} if the element has been deleted.
   */
  public @Nullable PsiElement getNavigationTarget() {
    return myNavigationTarget.getElement();
  }

  /**
   * Returns the smart pointer for the gutter anchor, for use as a stable map key.
   * Unlike {@link #getGutterAnchor()}, this does not resolve the pointer.
   */
  public @NotNull SmartPsiElementPointer<PsiElement> getGutterAnchorPointer() {
    return myGutterAnchor;
  }

  public @NotNull EntryRole getRole() {
    return myRole;
  }

  /**
   * Returns the strategy (EP) that produced this entry, or {@code null} for core entries.
   */
  public @Nullable GuiceBindingMatchStrategy getStrategy() {
    return myStrategy;
  }

  /**
   * Returns the human-readable text for this entry in navigation popups.
   *
   * <p>If a custom text provider was set at creation time, it is called with the
   * navigation target element. Otherwise, falls back to
   * {@link SymbolPresentationUtil#getSymbolPresentableText}.
   *
   * @return the display text, or {@code null} if the navigation target is invalid
   */
  public @Nullable String getPresentableText() {
    PsiElement target = myNavigationTarget.getElement();
    if (target == null) return null;

    return myTextProvider.apply(target);
  }

  /**
   * Returns the icon representing this entry's navigation target.
   *
   * <p>Delegates to {@link PsiElement#getIcon(int)} which reflects the element's
   * kind (field, method, class) and visibility (public, private, protected).
   *
   * @return the icon, or {@code null} if the navigation target is invalid
   */
  public @Nullable Icon getIcon() {
    PsiElement target = myNavigationTarget.getElement();
    return target != null ? target.getIcon(0) : null;
  }

  /**
   * Whether this entry is still valid (underlying PSI elements exist).
   */
  public boolean isValid() {
    return myNavigationTarget.getElement() != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GuiceEntry that)) return false;
    return myRole == that.myRole
        && Objects.equals(myNavigationTarget, that.myNavigationTarget);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myNavigationTarget, myRole);
  }

  @Override
  public String toString() {
    PsiElement target = myNavigationTarget.getElement();
    return myRole + " " + myKey + " → " + (target != null ? target : "<invalid>");
  }
}
