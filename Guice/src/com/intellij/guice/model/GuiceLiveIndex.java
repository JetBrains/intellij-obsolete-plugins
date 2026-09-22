// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.extensions.GuiceBindingMatchStrategy;
import com.intellij.guice.model.jam.GuiceProvides;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mutable, thread-safe storage layer that maintains type-indexed maps of Guice bindings,
 * injection points, and {@code @Provides} methods.
 *
 * <p>This class is responsible for <em>storage and per-file mutation</em> only.
 * All navigation queries go through {@link GuiceNavigationIndex}, which provides
 * symmetric navigation guarantees.
 *
 * <h3>Thread safety</h3>
 * <p>A {@link ReentrantReadWriteLock} protects all internal state.
 * Mutation methods ({@code updateFile}, {@code removeFile}, {@code clear})
 * acquire the <em>write lock</em> — exclusive access.</p>
 *
 * <h3>Per-file contribution tracking</h3>
 * <p>The key innovation is {@link FileContributions}: a record that tracks not just what data a
 * file contributed, but also which <em>map keys</em> each element was indexed under.  This
 * enables surgical removal without accessing stale PSI (which would throw exceptions since the
 * old PSI tree is gone after a file edit).</p>
 *
 * @see GuiceNavigationIndex
 */
public final class GuiceLiveIndex {

  // -----------------------------------------------------------------------
  // Per-file contribution tracking
  // -----------------------------------------------------------------------

  /**
   * Tracks what a specific file contributed to the live index.
   * Pre-computed keys enable surgical removal without accessing stale PSI.
   *
   * @param bindings            the set of {@link BindDescriptor}s contributed by the file
   * @param injectionPoints     the set of {@link InjectionPointDescriptor}s contributed by the file
   * @param provides            the list of {@link GuiceProvides} contributed by the file
   * @param bindingBoundKeys    pre-computed FQN → descriptors that were added to {@link #bindingsByBoundTypeFqn}
   * @param bindingImplKeys     pre-computed FQN → descriptors that were added to {@link #bindingsByBindingTypeFqn}
   * @param ipKeys              pre-computed FQN → IPs that were added to {@link #injectionPointsByTypeFqn}
   * @param providesKeys        pre-computed FQN → provides that were added to {@link #providesByReturnTypeFqn}
   */
  record FileContributions(
      @NotNull Set<BindDescriptor> bindings,
      @NotNull Set<InjectionPointDescriptor> injectionPoints,
      @NotNull List<GuiceProvides> provides,
      @NotNull Map<String, Set<BindDescriptor>> bindingBoundKeys,
      @NotNull Map<String, Set<BindDescriptor>> bindingImplKeys,
      @NotNull Map<String, Set<InjectionPointDescriptor>> ipKeys,
      @NotNull Map<String, Set<GuiceProvides>> providesKeys
  ) {}

  // -----------------------------------------------------------------------
  // Lock
  // -----------------------------------------------------------------------

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  // -----------------------------------------------------------------------
  // Per-file tracking
  // -----------------------------------------------------------------------

  /** Maps each file to the contributions it currently has in the index. */
  private final Map<VirtualFile, FileContributions> perFileData = new HashMap<>();

  // -----------------------------------------------------------------------
  // Live indexed maps (mutable, protected by lock)
  // -----------------------------------------------------------------------

  /** Key: qualified name of the <em>bound</em> class ({@code descriptor.getBoundClass().getQualifiedName()}). */
  private final Map<String, Set<BindDescriptor>> bindingsByBoundTypeFqn = new HashMap<>();

  /** Key: qualified name of the <em>binding</em> (implementation) class ({@code descriptor.getBindingClass().getQualifiedName()}). */
  private final Map<String, Set<BindDescriptor>> bindingsByBindingTypeFqn = new HashMap<>();

  /**
   * Special-case bindings indexed by descriptor class — typically very few, scanned linearly.
   * Populated and queried via {@link GuiceBindingMatchStrategy} extension point.
   */
  private final Map<Class<? extends BindDescriptor>, List<BindDescriptor>> specialBindings = new HashMap<>();

  /** Key: qualified name of the injection-point type (or unwrapped Provider type). */
  private final Map<String, Set<InjectionPointDescriptor>> injectionPointsByTypeFqn = new HashMap<>();

  /** Key: qualified name of the {@code @Provides} method's return type. */
  private final Map<String, Set<GuiceProvides>> providesByReturnTypeFqn = new HashMap<>();

  // -----------------------------------------------------------------------
  // Mutation methods (writeLock)
  // -----------------------------------------------------------------------

  /**
   * Surgically updates the index for a single file.
   *
   * <ol>
   *   <li>Removes old contributions for this file (if any) using {@link #removeContributions}.</li>
   *   <li>Adds new contributions using {@link #addAndTrack}, which inserts each element into the
   *       appropriate indexed maps and records the keys used for later removal.</li>
   *   <li>Stores the resulting {@link FileContributions} in {@link #perFileData}.</li>
   * </ol>
   *
   * @param file     the virtual file whose contributions are being updated
   * @param bindings the new set of {@link BindDescriptor}s discovered in the file
   * @param ips      the new set of {@link InjectionPointDescriptor}s discovered in the file
   * @param provides the new list of {@link GuiceProvides} discovered in the file
   */
  public void updateFile(@NotNull VirtualFile file,
                         @NotNull Set<BindDescriptor> bindings,
                         @NotNull Set<InjectionPointDescriptor> ips,
                         @NotNull List<GuiceProvides> provides) {
    lock.writeLock().lock();
    try {
      // Resolve strategies once for both remove + add.
      List<GuiceBindingMatchStrategy> strategies = GuiceBindingMatchStrategy.EP_NAME.getExtensionList();

      // 1. Remove old contributions for this file (if any).
      FileContributions old = perFileData.remove(file);
      if (old != null) {
        removeContributions(old, strategies);
      }

      // 2. Add new contributions and record keys.
      FileContributions newContribs = addAndTrack(bindings, ips, provides, strategies);

      // 3. Store for later removal.
      perFileData.put(file, newContribs);
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Removes all contributions from the given file and forgets about it.
   *
   * @param file the virtual file to remove from the index
   */
  public void removeFile(@NotNull VirtualFile file) {
    lock.writeLock().lock();
    try {
      FileContributions old = perFileData.remove(file);
      if (old != null) {
        removeContributions(old, GuiceBindingMatchStrategy.EP_NAME.getExtensionList());
      }
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Clears all data from the index, resetting it to an empty state.
   */
  public void clear() {
    lock.writeLock().lock();
    try {
      perFileData.clear();
      bindingsByBoundTypeFqn.clear();
      bindingsByBindingTypeFqn.clear();
      specialBindings.clear();
      injectionPointsByTypeFqn.clear();
      providesByReturnTypeFqn.clear();
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  // -----------------------------------------------------------------------
  // Private mutation helpers
  // -----------------------------------------------------------------------

  /**
   * Surgically removes entries using pre-computed keys.
   *
   * <p><b>Important:</b> This method <em>never</em> accesses PSI on the old descriptors
   * (no {@code getBoundClass()}, {@code getType()}, etc.).  It uses only the pre-computed
   * keys stored in {@link FileContributions}.  This is crucial because old PSI elements
   * become invalid after a file edit.</p>
   *
   * @param old        the contributions to remove
   * @param strategies pre-resolved match strategies (avoids per-call EP lookup)
   */
  private void removeContributions(@NotNull FileContributions old,
                                    @NotNull List<GuiceBindingMatchStrategy> strategies) {
    // Remove from special-case lists via match strategies.
    for (BindDescriptor bd : old.bindings()) {
      for (GuiceBindingMatchStrategy strategy : strategies) {
        if (strategy.getDescriptorClass().isInstance(bd)) {
          List<BindDescriptor> list = specialBindings.get(strategy.getDescriptorClass());
          if (list != null) {
            list.remove(bd);
          }
          break;
        }
      }
    }

    removeTracked(bindingsByBoundTypeFqn, old.bindingBoundKeys());
    removeTracked(bindingsByBindingTypeFqn, old.bindingImplKeys());
    removeTracked(injectionPointsByTypeFqn, old.ipKeys());
    removeTracked(providesByReturnTypeFqn, old.providesKeys());
  }

  /**
   * Adds entries to all maps AND records which keys were used, returning a
   * {@link FileContributions} for later surgical removal.
   *
   * @param bindings   the bind descriptors to add
   * @param ips        the injection-point descriptors to add
   * @param provides   the provides descriptors to add
   * @param strategies pre-resolved match strategies (avoids per-call EP lookup)
   * @return a new {@link FileContributions} recording what was added and under which keys
   */
  private @NotNull FileContributions addAndTrack(@NotNull Set<BindDescriptor> bindings,
                                                 @NotNull Set<InjectionPointDescriptor> ips,
                                                 @NotNull List<GuiceProvides> provides,
                                                 @NotNull List<GuiceBindingMatchStrategy> strategies) {
    // ---- Track keys for bindings ----
    Map<String, Set<BindDescriptor>> boundKeys = new HashMap<>();
    Map<String, Set<BindDescriptor>> implKeys = new HashMap<>();

    for (BindDescriptor bd : bindings) {
      // Sort into special-case lists via match strategies.
      for (GuiceBindingMatchStrategy strategy : strategies) {
        if (strategy.getDescriptorClass().isInstance(bd)) {
          specialBindings.computeIfAbsent(strategy.getDescriptorClass(), k -> new ArrayList<>()).add(bd);
          break;
        }
      }

      // Index by bound class FQN.
      PsiClass boundClass = bd.getBoundClass();
      if (boundClass != null) {
        indexAndTrack(bindingsByBoundTypeFqn, boundKeys, boundClass.getQualifiedName(), bd);
      }

      // Index by binding (implementation) class FQN.
      PsiClass bindingClass = bd.getBindingClass();
      if (bindingClass != null) {
        indexAndTrack(bindingsByBindingTypeFqn, implKeys, bindingClass.getQualifiedName(), bd);
      }
    }

    // ---- Track keys for injection points ----
    Map<String, Set<InjectionPointDescriptor>> ipTrackedKeys = new HashMap<>();

    for (InjectionPointDescriptor ip : ips) {
      PsiType ipType = ip.getType();
      if (ipType == null) continue;

      String directFqn = getTypeFqn(ipType);
      indexAndTrack(injectionPointsByTypeFqn, ipTrackedKeys, directFqn, ip);

      // If the IP type is Provider<T>, also index under T's FQN so that bindings for T
      // can discover injection points declared as Provider<T>.
      PsiType unwrappedProvider = GuiceUtils.getProviderType(ipType);
      indexUnwrappedType(injectionPointsByTypeFqn, ipTrackedKeys, ip, unwrappedProvider, directFqn);

      // Also index under Optional<T>, Set<T>, Map<K,V> unwrapped types.
      PsiType providerOrDirect = unwrappedProvider != null ? unwrappedProvider : ipType;
      indexUnwrappedType(injectionPointsByTypeFqn, ipTrackedKeys, ip,
          GuiceUtils.getOptionalType(providerOrDirect), directFqn);
      indexUnwrappedType(injectionPointsByTypeFqn, ipTrackedKeys, ip,
          GuiceUtils.getMultibinderElementType(providerOrDirect), directFqn);
      indexUnwrappedType(injectionPointsByTypeFqn, ipTrackedKeys, ip,
          GuiceUtils.getMultibinderValueType(providerOrDirect), directFqn);
    }

    // ---- Track keys for @Provides ----
    Map<String, Set<GuiceProvides>> providesTrackedKeys = new HashMap<>();

    for (GuiceProvides p : provides) {
      PsiType productType = p.getProductType();
      indexAndTrack(providesByReturnTypeFqn, providesTrackedKeys, getTypeFqn(productType), p);
    }

    return new FileContributions(bindings, ips, provides, boundKeys, implKeys, ipTrackedKeys, providesTrackedKeys);
  }

  /**
   * Adds {@code element} to both a live index map and a tracking map under the given FQN key.
   * No-op if {@code fqn} is {@code null}.
   */
  private static <T> void indexAndTrack(@NotNull Map<String, Set<T>> liveMap,
                                        @NotNull Map<String, Set<T>> trackingMap,
                                        @Nullable String fqn,
                                        @NotNull T element) {
    if (fqn == null) return;
    liveMap.computeIfAbsent(fqn, k -> new HashSet<>()).add(element);
    trackingMap.computeIfAbsent(fqn, k -> new HashSet<>()).add(element);
  }

  /**
   * Removes tracked elements from a live map using pre-computed keys.
   * Cleans up empty sets to avoid memory leaks.
   */
  private static <T> void removeTracked(@NotNull Map<String, Set<T>> liveMap,
                                        @NotNull Map<String, Set<T>> trackedKeys) {
    for (Map.Entry<String, Set<T>> entry : trackedKeys.entrySet()) {
      Set<T> set = liveMap.get(entry.getKey());
      if (set != null) {
        set.removeAll(entry.getValue());
        if (set.isEmpty()) {
          liveMap.remove(entry.getKey());
        }
      }
    }
  }

  /**
   * Adds {@code ip} under {@code unwrappedType}'s FQN, unless it equals {@code directFqn}.
   */
  private static void indexUnwrappedType(@NotNull Map<String, Set<InjectionPointDescriptor>> liveMap,
                                         @NotNull Map<String, Set<InjectionPointDescriptor>> trackingMap,
                                         @NotNull InjectionPointDescriptor ip,
                                         @Nullable PsiType unwrappedType,
                                         @Nullable String directFqn) {
    if (unwrappedType == null) return;
    String fqn = getTypeFqn(unwrappedType);
    if (fqn != null && !fqn.equals(directFqn)) {
      indexAndTrack(liveMap, trackingMap, fqn, ip);
    }
  }

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

  /**
   * Resolves the FQN from a {@link PsiType}.  For class types, resolves the class and returns
   * its qualified name.  For primitive types ({@code boolean}, {@code int}, etc.), returns the
   * FQN of the corresponding boxed wrapper ({@code java.lang.Boolean}, {@code java.lang.Integer})
   * so that primitives and their wrappers share the same index key.
   *
   * @param type the PSI type to resolve
   * @return the fully-qualified class name (or boxed equivalent), or {@code null}
   */
  private static @Nullable String getTypeFqn(@Nullable PsiType type) {
    if (type instanceof PsiPrimitiveType primitiveType) {
      return primitiveType.getBoxedTypeName();
    }
    if (type instanceof PsiClassType) {
      PsiClass cls = ((PsiClassType) type).resolve();
      return cls != null ? cls.getQualifiedName() : null;
    }
    return null;
  }
}

