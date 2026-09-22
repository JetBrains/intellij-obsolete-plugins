// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.extensions;

import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Extension point for contributing Guice binding types to the model.
 *
 * <p>Each implementation handles one category of Guice bindings (e.g., standard
 * {@code bind().to()}, OptionalBinder, MapBinder, etc.).  The plugin discovers
 * all registered contributors via the {@link #EP_NAME} extension point and
 * dispatches call expressions to them during file processing.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li><b>File discovery</b>: {@link #getRelevantAnnotations()} and
 *       {@link #getRelevantSuperclasses()} are queried to determine which
 *       files should be scanned during initial population.</li>
 *   <li><b>Binding extraction</b>: For each call expression whose method name
 *       matches one of this contributor's {@link #getBindingWords()}, the
 *       contributor's {@link #processCall} is invoked to create descriptors.</li>
 *   <li><b>Unresolved fallback</b>: When a call cannot be resolved (incomplete
 *       code), {@link #processUnresolvedCall} provides a fallback path.</li>
 * </ol>
 *
 * <h3>Thread safety</h3>
 * <p>Implementations must be stateless and thread-safe.  All methods may be
 * called from multiple threads concurrently.
 */
public interface GuiceBindingContributor {

  ExtensionPointName<GuiceBindingContributor> EP_NAME =
      ExtensionPointName.create("com.intellij.guice.bindingContributor");

  /**
   * Returns the set of method-name tokens this contributor is interested in.
   *
   * <p>Used by the UAST visitor to pre-filter call expressions before dispatching
   * to contributors.  Only calls whose method name is in the merged set of all
   * contributors' binding words will be dispatched.
   *
   * <p>Example: {@code Set.of("to", "toInstance", "toProvider", "toConstructor", "bind")}
   *
   * @return an immutable set of method names, never empty
   */
  @NotNull Set<String> getBindingWords();

  /**
   * Attempts to create binding descriptors from a resolved UAST call expression.
   *
   * <p>Called when a call expression matches one of this contributor's
   * {@link #getBindingWords()} AND the call resolves to a method on a known class.
   *
   * @param call            the UAST call expression
   * @param methodName      the method name (already verified to be in {@link #getBindingWords()})
   * @param resolvedQName   the FQN of the class containing the resolved method
   * @param containingClass the class containing the resolved method (for inheritance checks)
   * @param descriptors     output set — add created descriptors here
   * @return {@code true} if this contributor handled the call (stops further dispatch
   *         to other contributors for this call)
   */
  boolean processCall(@NotNull UCallExpression call,
                      @NotNull String methodName,
                      @NotNull String resolvedQName,
                      @NotNull PsiClass containingClass,
                      @NotNull Set<BindDescriptor> descriptors);

  /**
   * Fallback for unresolved calls (incomplete code, non-existent classes).
   *
   * <p>Called when a call expression matches one of this contributor's
   * {@link #getBindingWords()} but the call cannot be resolved.  Since the caller
   * is already inside a verified Guice module class, contributors can match on
   * the method name text alone.
   *
   * <p>The default implementation does nothing.
   *
   * @param call        the unresolved UAST call expression
   * @param methodName  the method name
   * @param descriptors output set — add created descriptors here
   * @return {@code true} if this contributor handled the call
   */
  default boolean processUnresolvedCall(@NotNull UCallExpression call,
                                        @NotNull String methodName,
                                        @NotNull Set<BindDescriptor> descriptors) {
    return false;
  }

  /**
   * Returns additional annotation FQNs that should trigger file discovery
   * during initial population.
   *
   * <p>Files containing elements annotated with any of these annotations will
   * be included in the set of files to scan.
   *
   * <p>The default implementation returns an empty list (contributor's files
   * are discovered via binding words and Guice module inheritance alone).
   *
   * @return annotation FQNs for file discovery, may be empty
   */
  default @NotNull Collection<String> getRelevantAnnotations() {
    return List.of();
  }

  /**
   * Returns additional class FQNs whose inheritors should be scanned during
   * initial population.
   *
   * <p>For example, a contributor that handles bindings inside classes extending
   * a custom base module would return that base module's FQN here.
   *
   * <p>The default implementation returns an empty list.
   *
   * @return superclass FQNs for file discovery, may be empty
   */
  default @NotNull Collection<String> getRelevantSuperclasses() {
    return List.of();
  }
}
