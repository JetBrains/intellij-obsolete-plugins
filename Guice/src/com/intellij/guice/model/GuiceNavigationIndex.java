// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The unified navigation index for the Guice plugin.
 *
 * <p>Stores all {@link GuiceEntry} instances and provides the <b>single</b> matching
 * method {@link #findCounterparts}, which is used for both forward and reverse navigation.
 * This guarantees symmetric navigation by construction.
 *
 * <p>Entries are indexed by the FQN of their binding key's type for O(1) candidate lookup.
 * Per-file tracking enables efficient incremental updates when files change.
 */
public final class GuiceNavigationIndex {
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  /** All entries, indexed by type FQN for fast candidate lookup. */
  private final Map<String, Set<GuiceEntry>> entriesByTypeFqn = new HashMap<>();

  /** Secondary index: entries by gutter anchor pointer for O(1) annotator lookups.
   *  Keyed by SmartPsiElementPointer which is stable across reparses
   *  (equals compares by element class + file + text range). */
  private final Map<SmartPsiElementPointer<PsiElement>, Set<GuiceEntry>> entriesByAnchor = new HashMap<>();

  /** Per-file tracking: which entries came from which file. */
  private final Map<String, FileEntries> entriesByFile = new HashMap<>();

  /**
   * Tracks the entries and their index keys for a single file,
   * enabling efficient removal when the file is re-indexed.
   */
  private record FileEntries(
      @NotNull Set<GuiceEntry> entries,
      @NotNull Map<String, Set<GuiceEntry>> keyToEntries
  ) {}

  // -----------------------------------------------------------------------
  // Index mutation
  // -----------------------------------------------------------------------

  /**
   * Updates the index with entries from a single file.
   * Removes any previously tracked entries for that file, then adds the new ones.
   *
   * @param filePath the virtual file path (used as the tracking key)
   * @param entries  the complete set of entries contributed by the file
   */
  public void updateFile(@NotNull String filePath, @NotNull Set<GuiceEntry> entries) {
    lock.writeLock().lock();
    try {
      removeFileEntries(filePath);
      addFileEntries(filePath, entries);
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Removes all entries contributed by a file.
   */
  public void removeFile(@NotNull String filePath) {
    lock.writeLock().lock();
    try {
      removeFileEntries(filePath);
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Clears the entire index.
   */
  public void clear() {
    lock.writeLock().lock();
    try {
      entriesByTypeFqn.clear();
      entriesByAnchor.clear();
      entriesByFile.clear();
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  // -----------------------------------------------------------------------
  // The SINGLE matching method — guarantees symmetric navigation
  // -----------------------------------------------------------------------

  /**
   * Finds all counterpart entries for the given entry: entries with the
   * <b>same binding key</b> and <b>opposite role</b>.
   *
   * <p>This is the <b>only</b> matching method. Both gutter directions
   * (injection point → binding site, and binding site → injection point)
   * use this same method, which makes asymmetric navigation impossible
   * by construction.
   *
   * @param entry the entry to find counterparts for
   * @return the set of matching entries (never null, may be empty)
   */
  public @NotNull Set<GuiceEntry> findCounterparts(@NotNull GuiceEntry entry) {
    lock.readLock().lock();
    try {
      String fqn = entry.getKey().getTypeFqn();
      if (fqn == null) return Set.of();

      Set<GuiceEntry> candidates = entriesByTypeFqn.get(fqn);
      if (candidates == null || candidates.isEmpty()) return Set.of();

      EntryRole oppositeRole = entry.getRole() == EntryRole.INJECTION_POINT
          ? EntryRole.BINDING_SITE
          : EntryRole.INJECTION_POINT;

      Set<GuiceEntry> result = new HashSet<>();
      for (GuiceEntry candidate : candidates) {
        try {
          if (candidate.getRole() == oppositeRole
              && candidate.isValid()
              && entry.getKey().matches(candidate.getKey())) {
            result.add(candidate);
          }
        }
        catch (PsiInvalidElementAccessException e) {
          // Stale PSI — skip.
        }
      }
      return result;
    }
    finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Finds all entries of a specific role that match the given binding key.
   * Used by the annotator when it has a key but no entry yet (e.g., JIT constructors).
   */
  public @NotNull Set<GuiceEntry> findByKey(@NotNull GuiceBindingKey key, @NotNull EntryRole role) {
    lock.readLock().lock();
    try {
      String fqn = key.getTypeFqn();
      if (fqn == null) return Set.of();

      Set<GuiceEntry> candidates = entriesByTypeFqn.get(fqn);
      if (candidates == null) return Set.of();

      Set<GuiceEntry> result = new HashSet<>();
      for (GuiceEntry candidate : candidates) {
        try {
          if (candidate.getRole() == role
              && candidate.isValid()
              && key.matches(candidate.getKey())) {
            result.add(candidate);
          }
        }
        catch (PsiInvalidElementAccessException e) {
          // Stale PSI — skip.
        }
      }
      return result;
    }
    finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Finds all entries whose gutter anchor matches the given PSI element.
   *
   * <p>This is used by the annotator to retrieve pre-computed entries for an element
   * without reconstructing them. The index already has the correct entries (including
   * proper type wrapping from strategies and contributors), so there is no need to
   * duplicate that logic in the annotator.
   *
   * @param anchor the gutter anchor element to match
   * @return entries anchored at this element (never null, may be empty)
   */
  public @NotNull Set<GuiceEntry> findEntriesByAnchor(@NotNull PsiElement anchor) {
    lock.readLock().lock();
    try {
      SmartPsiElementPointer<PsiElement> key = SmartPointerManager.createPointer(anchor);
      Set<GuiceEntry> entries = entriesByAnchor.get(key);
      return entries != null ? new HashSet<>(entries) : Set.of();
    }
    finally {
      lock.readLock().unlock();
    }
  }

  // -----------------------------------------------------------------------
  // Internal helpers
  // -----------------------------------------------------------------------

  private void removeFileEntries(@NotNull String filePath) {
    FileEntries old = entriesByFile.remove(filePath);
    if (old == null) return;

    for (Map.Entry<String, Set<GuiceEntry>> e : old.keyToEntries().entrySet()) {
      Set<GuiceEntry> indexSet = entriesByTypeFqn.get(e.getKey());
      if (indexSet != null) {
        indexSet.removeAll(e.getValue());
        if (indexSet.isEmpty()) {
          entriesByTypeFqn.remove(e.getKey());
        }
      }
    }

    // Remove from anchor index
    for (GuiceEntry entry : old.entries()) {
      SmartPsiElementPointer<PsiElement> anchorPtr = entry.getGutterAnchorPointer();
      Set<GuiceEntry> anchorSet = entriesByAnchor.get(anchorPtr);
      if (anchorSet != null) {
        anchorSet.remove(entry);
        if (anchorSet.isEmpty()) entriesByAnchor.remove(anchorPtr);
      }
    }
  }

  private void addFileEntries(@NotNull String filePath, @NotNull Set<GuiceEntry> entries) {
    if (entries.isEmpty()) return;

    Map<String, Set<GuiceEntry>> keyToEntries = new HashMap<>();

    for (GuiceEntry entry : entries) {
      String fqn = entry.getKey().getTypeFqn();
      if (fqn == null) continue;

      entriesByTypeFqn.computeIfAbsent(fqn, k -> new HashSet<>()).add(entry);
      keyToEntries.computeIfAbsent(fqn, k -> new HashSet<>()).add(entry);

      // Populate anchor index
      entriesByAnchor.computeIfAbsent(entry.getGutterAnchorPointer(), k -> new HashSet<>()).add(entry);
    }

    entriesByFile.put(filePath, new FileEntries(entries, keyToEntries));
  }

  /**
   * Extracts the FQN from a {@link PsiType} for index lookups.
   * For class types, resolves the class and returns its qualified name.
   * For primitive types ({@code boolean}, {@code int}, etc.), returns the
   * FQN of the corresponding boxed wrapper ({@code java.lang.Boolean},
   * {@code java.lang.Integer}) so that primitives and their wrappers share
   * the same index key.
   *
   * @return the FQN, or {@code null} for types without a resolvable class
   */
  static @Nullable String getTypeFqn(@NotNull PsiType type) {
    if (type instanceof PsiPrimitiveType primitiveType) {
      return primitiveType.getBoxedTypeName();
    }
    if (type instanceof PsiClassType classType) {
      PsiClass psiClass = classType.resolve();
      return psiClass != null ? psiClass.getQualifiedName() : null;
    }
    return null;
  }

}
