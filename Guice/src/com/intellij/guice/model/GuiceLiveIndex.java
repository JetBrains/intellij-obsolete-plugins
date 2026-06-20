// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.guice.model.beans.*;
import com.intellij.guice.model.extensions.GuiceBindingMatchStrategy;
import com.intellij.guice.model.jam.GuiceProvides;
import com.intellij.guice.utils.GuiceUtils;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Mutable, thread-safe live model that maintains type-indexed maps of Guice bindings,
 * injection points, and {@code @Provides} methods.
 *
 * <h3>Key difference from {@link GuiceModuleIndex}</h3>
 * <p>{@code GuiceModuleIndex} is an immutable snapshot that is rebuilt from scratch on every
 * change.  {@code GuiceLiveIndex} supports <em>surgical per-file updates</em>: when a file
 * changes, only that file's contributions are removed and re-added to the maps — no full
 * rebuild is needed.</p>
 *
 * <h3>Thread safety</h3>
 * <p>A {@link ReentrantReadWriteLock} protects all internal state:</p>
 * <ul>
 *   <li><b>Query methods</b> ({@code findMatching*}, {@code getAll*}, etc.) acquire the
 *       <em>read lock</em> — multiple concurrent readers are allowed.</li>
 *   <li><b>Mutation methods</b> ({@code updateFile}, {@code removeFile}, {@code clear})
 *       acquire the <em>write lock</em> — exclusive access.</li>
 * </ul>
 * <p>This is critical because query methods are called from multiple highlighting threads
 * concurrently, while {@code updateFile} is called from a single processing thread.</p>
 *
 * <h3>Per-file contribution tracking</h3>
 * <p>The key innovation is {@link FileContributions}: a record that tracks not just what data a
 * file contributed, but also which <em>map keys</em> each element was indexed under.  This
 * enables surgical removal without accessing stale PSI (which would throw exceptions since the
 * old PSI tree is gone after a file edit).</p>
 *
 * @see GuiceModuleIndex
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

  // Raw collections for full iteration (some queries need to scan all entries).
  private final Set<BindDescriptor> allBindings = new HashSet<>();
  private final List<GuiceProvides> allProvides = new ArrayList<>();

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
      allBindings.clear();
      allProvides.clear();
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
    // Remove from allBindings.
    allBindings.removeAll(old.bindings());

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

    // Remove from bindingsByBoundTypeFqn using tracked keys.
    for (Map.Entry<String, Set<BindDescriptor>> entry : old.bindingBoundKeys().entrySet()) {
      Set<BindDescriptor> set = bindingsByBoundTypeFqn.get(entry.getKey());
      if (set != null) {
        set.removeAll(entry.getValue());
        if (set.isEmpty()) {
          bindingsByBoundTypeFqn.remove(entry.getKey());
        }
      }
    }

    // Remove from bindingsByBindingTypeFqn using tracked keys.
    for (Map.Entry<String, Set<BindDescriptor>> entry : old.bindingImplKeys().entrySet()) {
      Set<BindDescriptor> set = bindingsByBindingTypeFqn.get(entry.getKey());
      if (set != null) {
        set.removeAll(entry.getValue());
        if (set.isEmpty()) {
          bindingsByBindingTypeFqn.remove(entry.getKey());
        }
      }
    }

    // Remove from injectionPointsByTypeFqn using tracked keys.
    for (Map.Entry<String, Set<InjectionPointDescriptor>> entry : old.ipKeys().entrySet()) {
      Set<InjectionPointDescriptor> set = injectionPointsByTypeFqn.get(entry.getKey());
      if (set != null) {
        set.removeAll(entry.getValue());
        if (set.isEmpty()) {
          injectionPointsByTypeFqn.remove(entry.getKey());
        }
      }
    }

    // Remove from providesByReturnTypeFqn using tracked keys.
    for (Map.Entry<String, Set<GuiceProvides>> entry : old.providesKeys().entrySet()) {
      Set<GuiceProvides> set = providesByReturnTypeFqn.get(entry.getKey());
      if (set != null) {
        set.removeAll(entry.getValue());
        if (set.isEmpty()) {
          providesByReturnTypeFqn.remove(entry.getKey());
        }
      }
    }

    // Remove from allProvides.
    allProvides.removeAll(old.provides());
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
      allBindings.add(bd);

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
        String fqn = boundClass.getQualifiedName();
        if (fqn != null) {
          bindingsByBoundTypeFqn.computeIfAbsent(fqn, k -> new HashSet<>()).add(bd);
          boundKeys.computeIfAbsent(fqn, k -> new HashSet<>()).add(bd);
        }
      }

      // Index by binding (implementation) class FQN.
      PsiClass bindingClass = bd.getBindingClass();
      if (bindingClass != null) {
        String fqn = bindingClass.getQualifiedName();
        if (fqn != null) {
          bindingsByBindingTypeFqn.computeIfAbsent(fqn, k -> new HashSet<>()).add(bd);
          implKeys.computeIfAbsent(fqn, k -> new HashSet<>()).add(bd);
        }
      }
    }

    // ---- Track keys for injection points ----
    Map<String, Set<InjectionPointDescriptor>> ipTrackedKeys = new HashMap<>();

    for (InjectionPointDescriptor ip : ips) {
      PsiType ipType = ip.getType();
      if (ipType == null) continue;

      String directFqn = getTypeFqn(ipType);
      if (directFqn != null) {
        injectionPointsByTypeFqn.computeIfAbsent(directFqn, k -> new HashSet<>()).add(ip);
        ipTrackedKeys.computeIfAbsent(directFqn, k -> new HashSet<>()).add(ip);
      }

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
      allProvides.add(p);
      PsiType productType = p.getProductType();
      String fqn = getTypeFqn(productType);
      if (fqn != null) {
        providesByReturnTypeFqn.computeIfAbsent(fqn, k -> new HashSet<>()).add(p);
        providesTrackedKeys.computeIfAbsent(fqn, k -> new HashSet<>()).add(p);
      }
    }

    return new FileContributions(bindings, ips, provides, boundKeys, implKeys, ipTrackedKeys, providesTrackedKeys);
  }

  /**
   * Adds {@code ip} to both the live index map and the tracking map under
   * {@code unwrappedType}'s FQN, unless the type is {@code null} or its FQN
   * equals the already-indexed {@code directFqn}.
   *
   * @param liveMap       the live index map to insert into
   * @param trackingMap   the tracking map that records keys for later removal
   * @param ip            the injection-point descriptor to add
   * @param unwrappedType the unwrapped type (e.g. the T from Optional&lt;T&gt;)
   * @param directFqn     the FQN already indexed for the IP's direct type (to avoid duplicates)
   */
  private static void indexUnwrappedType(@NotNull Map<String, Set<InjectionPointDescriptor>> liveMap,
                                         @NotNull Map<String, Set<InjectionPointDescriptor>> trackingMap,
                                         @NotNull InjectionPointDescriptor ip,
                                         @Nullable PsiType unwrappedType,
                                         @Nullable String directFqn) {
    if (unwrappedType == null) return;
    String fqn = getTypeFqn(unwrappedType);
    if (fqn != null && !fqn.equals(directFqn)) {
      liveMap.computeIfAbsent(fqn, k -> new HashSet<>()).add(ip);
      trackingMap.computeIfAbsent(fqn, k -> new HashSet<>()).add(ip);
    }
  }

  // -----------------------------------------------------------------------
  // Query methods (readLock)
  // -----------------------------------------------------------------------

  /**
   * For an {@code @Inject} field or parameter, finds all matching {@code bind()} call descriptors.
   *
   * <p>Handles:
   * <ul>
   *   <li>{@code Provider<T>} unwrapping</li>
   *   <li>{@code Optional<T>}, {@code Set<T>}, and {@code Map<K,V>} multibinder types</li>
   *   <li>Binding annotation matching via
   *       {@link GuiceInjectionUtil#checkBindingAnnotations(InjectionPointDescriptor, BindDescriptor)}</li>
   *   <li>JIT binding fallback via {@link GuiceUtils#getJitConstructor(PsiClass)}</li>
   * </ul>
   *
   * @param ip the injection point to find bindings for
   * @return a (possibly empty) set of matching bind descriptors
   */
  public @NotNull Set<BindDescriptor> findMatchingBindings(@NotNull InjectionPointDescriptor ip) {
    lock.readLock().lock();
    try {
      Set<BindDescriptor> result = new HashSet<>();
      PsiType ipType = ip.getType();
      if (ipType == null) return result;

      // For binding-declaration IPs (.to(), .toProvider()), the referenced class IS the
      // exact target — Provider unwrapping would be incorrect.
      PsiType providerType = ip.isBindingCall() ? null : GuiceUtils.getProviderType(ipType);
      PsiType targetType = providerType != null ? providerType : ipType;

      // Resolve the target type to a FQN for index lookup.
      String fqn = getTypeFqn(targetType);
      if (fqn == null) return result;

      // Cache strategies for this query pass.
      List<GuiceBindingMatchStrategy> strategies = GuiceBindingMatchStrategy.EP_NAME.getExtensionList();

      // 1. Direct lookup by bound-type FQN.
      {
        Set<BindDescriptor> candidates = bindingsByBoundTypeFqn.get(fqn);
        if (candidates != null) {
          for (BindDescriptor descriptor : candidates) {
            try {
              if (descriptor.matchesType(targetType)) {
                if (GuiceInjectionUtil.checkBindingAnnotations(ip, descriptor)) {
                  result.add(descriptor);
                }
              }
            }
            catch (com.intellij.psi.PsiInvalidElementAccessException e) {
              // Stale PSI — skip.
            }
          }
        }
      }

      // 2. Check special bindings via match strategies (small lists, linear scan).
      for (GuiceBindingMatchStrategy strategy : strategies) {
        if (strategy.isRelevantType(targetType)) {
          List<BindDescriptor> tracked = specialBindings.get(strategy.getDescriptorClass());
          if (tracked != null && !tracked.isEmpty()) {
            strategy.findMatchingBindings(tracked, targetType, ip, result);
          }
        }
      }

      // 6. JIT fallback: if no explicit binding found, check for an @Inject constructor.
      if (result.isEmpty() && targetType instanceof PsiClassType) {
        PsiClass psiClass = ((PsiClassType) targetType).resolve();
        if (psiClass != null) {
          PsiMethod jitConstructor = GuiceUtils.getJitConstructor(psiClass);
          if (jitConstructor != null) {
            result.add(new JitBindDescriptor(jitConstructor, psiClass));
          }
        }
      }

      return result;
    }
    catch (com.intellij.psi.PsiInvalidElementAccessException e) {
      // The ip itself has stale PSI — return empty.
      return new HashSet<>();
    }
    finally {
      lock.readLock().unlock();
    }
  }

  /**
   * For an {@code @Inject} field or parameter, finds all matching {@code @Provides} methods.
   *
   * <p>Handles:
   * <ul>
   *   <li>{@code Provider<T>} unwrapping</li>
   *   <li>{@code Optional<T>} unwrapping</li>
   *   <li>{@code Set<T>} with {@code @ProvidesIntoSet}</li>
   *   <li>{@code Map<K,V>} with {@code @ProvidesIntoMap}</li>
   *   <li>Binding annotation matching</li>
   * </ul>
   *
   * @param ip the injection point to find provides for
   * @return a (possibly empty) set of matching {@link GuiceProvides} descriptors
   */
  public @NotNull Set<GuiceProvides> findMatchingProvides(@NotNull InjectionPointDescriptor ip) {
    lock.readLock().lock();
    try {
      Set<GuiceProvides> result = new HashSet<>();
      PsiType ipType = ip.getType();
      if (ipType == null) return result;

      PsiType providerType = ip.isBindingCall() ? null : GuiceUtils.getProviderType(ipType);
      PsiType targetType = providerType != null ? providerType : ipType;
      PsiType optionalType = GuiceUtils.getOptionalType(targetType);
      if (optionalType != null) {
        targetType = optionalType;
      }

      Set<PsiAnnotation> ipAnnotations = ip.getBindingAnnotations();

      // 1. O(1) lookup by target type FQN for direct matches.
      String targetFqn = getTypeFqn(targetType);
      if (targetFqn != null) {
        Set<GuiceProvides> candidates = providesByReturnTypeFqn.get(targetFqn);
        if (candidates != null) {
          for (GuiceProvides provides : candidates) {
            try {
              PsiType productType = provides.getProductType();
              if (productType != null && TypeConversionUtil.isAssignable(targetType, productType) &&
                  GuiceInjectionUtil.checkBindingAnnotations(ipAnnotations, provides.getBindingAnnotations())) {
                result.add(provides);
              }
            }
            catch (com.intellij.psi.PsiInvalidElementAccessException e) {
              // Stale PSI — skip.
            }
          }
        }
      }

      // 2. Check @ProvidesInto* methods via match strategies
      //    (e.g., @ProvidesIntoSet for Set<T>, @ProvidesIntoMap for Map<K,V>).
      List<GuiceBindingMatchStrategy> strategies = GuiceBindingMatchStrategy.EP_NAME.getExtensionList();
      for (GuiceBindingMatchStrategy strategy : strategies) {
        PsiType unwrapped = strategy.unwrapType(targetType);
        if (unwrapped != null) {
          String unwrappedFqn = getTypeFqn(unwrapped);
          if (unwrappedFqn != null) {
            Set<GuiceProvides> candidates = providesByReturnTypeFqn.get(unwrappedFqn);
            if (candidates != null && !candidates.isEmpty()) {
              strategy.findMatchingProvides(candidates, unwrapped, ipAnnotations, result);
            }
          }
        }
      }

      return result;
    }
    catch (com.intellij.psi.PsiInvalidElementAccessException e) {
      // The ip itself has stale PSI — return empty.
      return new HashSet<>();
    }
    finally {
      lock.readLock().unlock();
    }
  }

  /**
   * For a {@code bind()} call in {@code configure()}, finds all matching {@code @Inject}
   * fields and parameters.
   *
   * <p>Uses the injection-point index for O(1) lookup by type, then verifies each candidate
   * with {@link BindDescriptor#matchesType(PsiType)} and binding-annotation checks.</p>
   *
   * @param descriptor the bind descriptor to find injection points for
   * @return a (possibly empty) set of matching injection-point descriptors
   */
  public @NotNull Set<InjectionPointDescriptor> findMatchingInjectionPoints(@NotNull BindDescriptor descriptor) {
    lock.readLock().lock();
    try {
      Set<InjectionPointDescriptor> result = new HashSet<>();

      PsiClass boundClass = descriptor.getBoundClass();
      if (boundClass == null) return result;
      String boundFqn = boundClass.getQualifiedName();
      if (boundFqn == null) return result;

      // Collect candidate IPs — indexed under the bound type's FQN
      // (direct, Provider<T>, Optional<T>, etc.)
      Set<InjectionPointDescriptor> candidates = new LinkedHashSet<>();
      Set<InjectionPointDescriptor> directCandidates = injectionPointsByTypeFqn.get(boundFqn);
      if (directCandidates != null) {
        candidates.addAll(directCandidates);
      }

      // Cache strategies for this query.
      List<GuiceBindingMatchStrategy> strategies = GuiceBindingMatchStrategy.EP_NAME.getExtensionList();

      // Verify each candidate against the descriptor.
      // Guard against stale PSI: the live index may hold descriptors whose underlying
      // PsiElements have been invalidated by a concurrent reparse.  The isValid() guards
      // in InjectionPointDescriptor/BindDescriptor catch most cases, but some edge cases
      // (e.g., language mismatch during resolve) can still throw.
      for (InjectionPointDescriptor ip : candidates) {
        try {
          PsiType ipType = ip.getType();
          if (ipType == null) continue;

          PsiType provType = GuiceUtils.getProviderType(ipType);
          PsiType effectiveType = provType != null ? provType : ipType;

          if (effectiveType instanceof PsiClassType || effectiveType instanceof PsiPrimitiveType) {
            boolean matches = descriptor.matchesType(effectiveType);

            // Check unwrapped types via match strategies (e.g., Optional<T> → T, Set<T> → T).
            // Each strategy unwraps the effective type and checks if the descriptor matches
            // the inner type, but only if the descriptor is NOT itself the special type
            // (e.g., OptionalBindDescriptor should not match via Optional<T> unwrapping).
            if (!matches) {
              for (GuiceBindingMatchStrategy strategy : strategies) {
                PsiType unwrapped = strategy.unwrapType(effectiveType);
                if (unwrapped != null && !strategy.shouldExcludeStandardBinding(descriptor) &&
                    descriptor.matchesType(unwrapped)) {
                  matches = true;
                  break;
                }
              }
            }

            if (matches && GuiceInjectionUtil.checkBindingAnnotations(ip, descriptor)) {
              result.add(ip);
            }
          }
        }
        catch (com.intellij.psi.PsiInvalidElementAccessException e) {
          // Stale PSI element — skip this candidate silently.
          // The file will be re-indexed on the next dirty processing pass.
        }
      }

      return result;
    }
    finally {
      lock.readLock().unlock();
    }
  }

  /**
   * For a {@code @Provides} method, finds all matching {@code @Inject} fields and parameters.
   *
   * @param provides the provides descriptor to find injection points for
   * @return a (possibly empty) set of matching injection-point descriptors
   */
  public @NotNull Set<InjectionPointDescriptor> findMatchingInjectionPoints(@NotNull GuiceProvides provides) {
    lock.readLock().lock();
    try {
      Set<InjectionPointDescriptor> result = new HashSet<>();
      PsiType productType = provides.getProductType();
      if (productType == null) return result;

      String productFqn = getTypeFqn(productType);
      if (productFqn == null) return result;

      // Gather candidate IPs from the index under the product FQN.
      Set<InjectionPointDescriptor> candidates = new LinkedHashSet<>();
      Set<InjectionPointDescriptor> direct = injectionPointsByTypeFqn.get(productFqn);
      if (direct != null) {
        candidates.addAll(direct);
      }

      for (InjectionPointDescriptor ip : candidates) {
        PsiType ipType = ip.getType();
        if (ipType == null) continue;

        PsiType provType = GuiceUtils.getProviderType(ipType);
        PsiType effectiveType = provType != null ? provType : ipType;
        PsiType optType = GuiceUtils.getOptionalType(effectiveType);
        if (optType != null) {
          effectiveType = optType;
        }

        if (TypeConversionUtil.isAssignable(productType, effectiveType) &&
            GuiceInjectionUtil.checkBindingAnnotations(ip.getBindingAnnotations(), provides.getBindingAnnotations())) {
          result.add(ip);
        }
      }

      return result;
    }
    finally {
      lock.readLock().unlock();
    }
  }

  /**
   * For a class declaration, finds all bindings that reference this class — as the bound class,
   * the binding (implementation) class, or (for {@link BindToProviderDescriptor}) the provider class.
   *
   * @param psiClass the class to search for
   * @return a (possibly empty) set of matching bind descriptors
   */
  public @NotNull Set<BindDescriptor> findBindingsForClass(@NotNull PsiClass psiClass) {
    lock.readLock().lock();
    try {
      Set<BindDescriptor> result = new HashSet<>();
      String fqn = psiClass.getQualifiedName();
      if (fqn == null) return result;

      // Bindings where this class is the bound type.
      Set<BindDescriptor> byBound = bindingsByBoundTypeFqn.get(fqn);
      if (byBound != null) {
        for (BindDescriptor descriptor : byBound) {
          if (GuiceUtils.areClassesEquivalent(psiClass, descriptor.getBoundClass())) {
            result.add(descriptor);
          }
        }
      }

      // Bindings where this class is the binding (implementation) type.
      Set<BindDescriptor> byBinding = bindingsByBindingTypeFqn.get(fqn);
      if (byBinding != null) {
        for (BindDescriptor descriptor : byBinding) {
          PsiClass bindingClass = descriptor.getBindingClass();
          PsiClass baseClass = getBindingBaseClass(bindingClass);
          if (GuiceUtils.areClassesEquivalent(psiClass, baseClass)) {
            result.add(descriptor);
          }
        }
      }

      // Bindings where this class is the provider class (BindToProviderDescriptor).
      // Linear scan — these are rare.
      for (BindDescriptor descriptor : allBindings) {
        if (descriptor instanceof BindToProviderDescriptor providerDescriptor) {
          PsiClass providerClass = providerDescriptor.getProviderClass();
          PsiClass baseProvider = getBindingBaseClass(providerClass);
          if (GuiceUtils.areClassesEquivalent(psiClass, baseProvider)) {
            result.add(descriptor);
          }
        }
      }

      return result;
    }
    finally {
      lock.readLock().unlock();
    }
  }

  /**
   * For a {@code @Provides} method, finds matching multibinder binding targets
   * ({@link OptionalBindDescriptor}, {@link SetMultibindDescriptor},
   * {@link MapMultibindDescriptor}).
   *
   * @param providesMethod the {@code @Provides} method to find multibinder targets for
   * @return a list of {@link PsiElement} targets (bind expressions) for navigation
   */
  public @NotNull List<PsiElement> findMultibinderTargets(@NotNull PsiMethod providesMethod) {
    lock.readLock().lock();
    try {
      List<PsiElement> targets = new ArrayList<>();
      PsiType returnType = providesMethod.getReturnType();
      if (!(returnType instanceof PsiClassType returnClassType)) return targets;

      PsiClass returnClass = returnClassType.resolve();
      if (returnClass == null) return targets;

      // Dispatch to match strategies for multibinder target finding.
      for (GuiceBindingMatchStrategy strategy : GuiceBindingMatchStrategy.EP_NAME.getExtensionList()) {
        List<BindDescriptor> tracked = specialBindings.get(strategy.getDescriptorClass());
        if (tracked != null && !tracked.isEmpty()) {
          targets.addAll(strategy.findMultibinderTargets(providesMethod, tracked));
        }
      }

      return targets;
    }
    finally {
      lock.readLock().unlock();
    }
  }

  // -----------------------------------------------------------------------
  // Getters
  // -----------------------------------------------------------------------

  /**
   * Returns a defensive copy of all binding descriptors currently in the index.
   *
   * @return a new, unmodifiable set of all bindings
   */
  public @NotNull Set<BindDescriptor> getAllBindings() {
    lock.readLock().lock();
    try {
      return Set.copyOf(allBindings);
    }
    finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Returns a defensive copy of all {@code @Provides} descriptors currently in the index.
   *
   * @return a new, unmodifiable list of all provides
   */
  public @NotNull List<GuiceProvides> getAllProvides() {
    lock.readLock().lock();
    try {
      return List.copyOf(allProvides);
    }
    finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Returns {@code true} if the index contains no bindings and no {@code @Provides} methods.
   * Useful for quick bail-out before performing expensive queries.
   *
   * @return {@code true} if the index is empty
   */
  public boolean isEmpty() {
    lock.readLock().lock();
    try {
      return allBindings.isEmpty() && allProvides.isEmpty();
    }
    finally {
      lock.readLock().unlock();
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

  /**
   * Returns the base class of a binding class.  If the class is an anonymous class,
   * returns the resolved base-class type; otherwise returns the class itself.
   *
   * @param bindingClass the class to unwrap
   * @return the base class, or the input class if not anonymous
   */
  private static @Nullable PsiClass getBindingBaseClass(@Nullable PsiClass bindingClass) {
    if (bindingClass instanceof PsiAnonymousClass) {
      return ((PsiAnonymousClass) bindingClass).getBaseClassType().resolve();
    }
    return bindingClass;
  }
}
