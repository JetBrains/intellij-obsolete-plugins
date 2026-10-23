// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.guice.model.extensions.GuiceExtensionIndex;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UDeclaration;
import org.jetbrains.uast.UastContextKt;
import org.jetbrains.uast.UastUtils;

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
 *   <li>{@link #getGutterAnchorPointer()} — <b>WHERE</b> the gutter icon is placed</li>
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
@ApiStatus.Experimental
public final class GuiceEntry {

  /**
   * Identifies the gutter anchor of an entry by position, for O(1) lookups in {@link GuiceNavigationIndex}.
   *
   * <p>A position key is valid only while the index holds a fresh extraction of the file. The line marker
   * providers guarantee this for the highlighted file ({@link GuiceProjectModel#ensureFileCurrent}), and the
   * index re-keys a file's entries on every update, also when the entries did not change.
   * A {@link SmartPsiElementPointer} would survive edits, but its hash code is only the file and the element
   * kind, so a map keyed by pointers degrades to range comparisons over all anchors of a file.
   *
   * @param filePath    the path of the file that holds the anchor
   * @param startOffset the start offset of the anchor element
   * @param leaf        whether the anchor is a leaf (a call identifier) rather than a declaration;
   *                    a declaration and a leaf can start at the same offset
   */
  public record AnchorKey(@NotNull String filePath, int startOffset, boolean leaf) {
    public static @Nullable AnchorKey of(@NotNull PsiElement element) {
      PsiFile file = element.getContainingFile();
      VirtualFile virtualFile = file != null ? file.getViewProvider().getVirtualFile() : null;
      TextRange range = element.getTextRange();
      if (virtualFile == null || range == null) return null;
      return new AnchorKey(virtualFile.getPath(), range.getStartOffset(), element.getFirstChild() == null);
    }
  }

  private final @NotNull GuiceBindingKey myKey;
  private final @NotNull SmartPsiElementPointer<PsiElement> myNavigationTarget;
  private final @NotNull SmartPsiElementPointer<PsiElement> myGutterAnchor;
  private final @Nullable AnchorKey myAnchorKey;
  /** Start offset of the navigation target at extraction time, for sorting without PSI access. */
  private final int myNavigationStartOffset;
  private final @NotNull EntryRole myRole;
  private final @NotNull Function<PsiElement, String> myTextProvider;
  private final boolean myImplementationReference;
  private final boolean myDefaultBinding;

  /**
   * Creates an {@link EntryRole#INJECTION_POINT} entry for the implementation class of a binding,
   * for example {@code FooImpl} in {@code bind(Foo.class).to(FooImpl.class)}.
   * The class gutter of the implementation class shows such entries.
   */
  public static @NotNull GuiceEntry implementationReference(@NotNull GuiceBindingKey key,
                                                            @NotNull PsiElement navigationTarget,
                                                            @NotNull PsiElement gutterAnchor,
                                                            @NotNull Function<PsiElement, String> textProvider) {
    return new GuiceEntry(key, navigationTarget, gutterAnchor, EntryRole.INJECTION_POINT, textProvider, true, false);
  }

  /**
   * Creates a default (JIT annotation) {@link EntryRole#BINDING_SITE} entry for {@code @ImplementedBy}
   * or {@code @ProvidedBy}. Explicit module bindings take precedence over default bindings.
   */
  public static @NotNull GuiceEntry defaultBinding(@NotNull GuiceBindingKey key,
                                                   @NotNull PsiElement navigationTarget,
                                                   @NotNull PsiElement gutterAnchor,
                                                   @NotNull Function<PsiElement, String> textProvider) {
    return new GuiceEntry(key, navigationTarget, gutterAnchor, EntryRole.BINDING_SITE, textProvider, false, true);
  }

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
                    @Nullable Function<PsiElement, String> textProvider) {
    this(key, navigationTarget, gutterAnchor, role, textProvider, false, false);
  }

  private GuiceEntry(@NotNull GuiceBindingKey key,
                     @NotNull PsiElement navigationTarget,
                     @NotNull PsiElement gutterAnchor,
                     @NotNull EntryRole role,
                     @Nullable Function<PsiElement, String> textProvider,
                     boolean implementationReference,
                     boolean defaultBinding) {
    myKey = key;
    myNavigationTarget = SmartPointerManager.createPointer(navigationTarget);
    myGutterAnchor = SmartPointerManager.createPointer(gutterAnchor);
    myAnchorKey = AnchorKey.of(gutterAnchor);
    TextRange targetRange = navigationTarget.getTextRange();
    myNavigationStartOffset = targetRange != null ? targetRange.getStartOffset() : -1;
    myRole = role;
    myTextProvider = textProvider != null ? textProvider : SymbolPresentationUtil::getSymbolPresentableText;
    myImplementationReference = implementationReference;
    myDefaultBinding = defaultBinding;
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
   * Returns the smart pointer for the gutter anchor. It survives edits, so it is part of {@link #equals}.
   */
  public @NotNull SmartPsiElementPointer<PsiElement> getGutterAnchorPointer() {
    return myGutterAnchor;
  }

  /**
   * Returns the position key of the gutter anchor at extraction time, or {@code null} if the anchor has no
   * file or range. See {@link AnchorKey}.
   */
  public @Nullable AnchorKey getAnchorKey() {
    return myAnchorKey;
  }

  /**
   * Returns the start offset of the navigation target at extraction time, or {@code -1}.
   *
   * <p>Sorting uses it instead of {@link PsiElement#getTextOffset()}: for a stub-based element that call
   * loads the AST of its file, and a popular key has hundreds of counterparts in files that are not loaded.
   * An edit in the target file re-extracts the entry, so the offset is stale only between the edit and
   * the background update, which only affects the order of targets in the same file.
   */
  public int getNavigationStartOffset() {
    return myNavigationStartOffset;
  }

  public @NotNull EntryRole getRole() {
    return myRole;
  }

  /**
   * Whether this entry refers to the implementation class of a binding, for example {@code .to(FooImpl.class)}.
   */
  public boolean isImplementationReference() {
    return myImplementationReference;
  }

  /**
   * Whether this entry is a default binding from {@code @ImplementedBy} or {@code @ProvidedBy},
   * which is overridden when an explicit module binding exists for the same key.
   */
  public boolean isDefaultBinding() {
    return myDefaultBinding;
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

    try {
      return myTextProvider.apply(target);
    }
    catch (Throwable t) {
      GuiceExtensionIndex.rethrowIfControlFlow(t);
      GuiceExtensionIndex.get().reportContributorFailure(myTextProvider.getClass(), "text provider on " + target, t);
      return SymbolPresentationUtil.getSymbolPresentableText(target);
    }
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
    if (target == null) return null;
    int flags = Iconable.ICON_FLAG_VISIBILITY | Iconable.ICON_FLAG_READ_STATUS;
    Icon icon = target.getIcon(flags);
    if (icon != null) return icon;
    UDeclaration uDecl = UastUtils.getParentOfType(UastContextKt.toUElement(target), UDeclaration.class);
    PsiElement declPsi = uDecl != null ? uDecl.getJavaPsi() : null;
    return declPsi != null ? declPsi.getIcon(flags) : null;
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
    // One binding expression can produce entries for several keys, for example Set<T> and Collection<Provider<T>>.
    // So the key and the gutter anchor are part of the identity.
    return myRole == that.myRole
        && myImplementationReference == that.myImplementationReference
        && myDefaultBinding == that.myDefaultBinding
        && myKey.equals(that.myKey)
        && Objects.equals(myNavigationTarget, that.myNavigationTarget)
        && Objects.equals(myGutterAnchor, that.myGutterAnchor);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myRole, myImplementationReference, myDefaultBinding, myKey, myNavigationTarget, myGutterAnchor);
  }

  @Override
  public String toString() {
    PsiElement target = myNavigationTarget.getElement();
    String targetStr = target != null ? target.getText() : "<invalid>";
    return myRole + "[" + myKey + " -> " + targetStr + "]";
  }
}
