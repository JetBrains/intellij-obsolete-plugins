// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
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

  /** Per-file tracking: which entries and cross-file target dependencies came from which file. */
  private final Map<String, FileEntries> entriesByFile = new HashMap<>();

  /** Reverse dependency index: target file path → set of registrar file paths that extract bindings from it. */
  private final Map<String, Set<String>> registrarsByTargetPath = new HashMap<>();

  /** Reverse dependency index: target class short name → set of registrar file paths that reference it. */
  private final Map<String, Set<String>> registrarsByTargetClassName = new HashMap<>();

  /**
   * Tracks the entries, their index keys, and cross-file target dependencies for a single file,
   * enabling efficient removal when the file is re-indexed.
   */
  private record FileEntries(
      @NotNull Set<GuiceEntry> entries,
      @NotNull Map<String, Set<GuiceEntry>> keyToEntries,
      @NotNull Set<String> referencedTargetPaths,
      @NotNull Set<String> referencedTargetClassNames
  ) {}

  // -----------------------------------------------------------------------
  // Index mutation
  // -----------------------------------------------------------------------

  /**
   * Updates the index with entries and cross-file target dependencies from a single file.
   */
  public void updateFile(@NotNull String filePath, @NotNull GuiceEntryProducer.FileExtractionResult extractionResult) {
    lock.writeLock().lock();
    try {
      removeFileEntries(filePath);
      addFileEntries(filePath, extractionResult);
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
      registrarsByTargetPath.clear();
      registrarsByTargetClassName.clear();
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Returns a copy of the paths of all files that have entries in the index.
   */
  public @NotNull Set<String> getIndexedFiles() {
    lock.readLock().lock();
    try {
      return new HashSet<>(entriesByFile.keySet());
    }
    finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Returns the paths of all registrar files that extract cross-class bindings from {@code targetFilePath}.
   */
  public @NotNull Set<String> getDependentRegistrarFiles(@NotNull String targetFilePath) {
    lock.readLock().lock();
    try {
      Set<String> registrars = registrarsByTargetPath.get(targetFilePath);
      return registrars != null ? Set.copyOf(registrars) : Set.of();
    }
    finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Returns the paths of all registrar files that extract cross-class bindings from {@code path}
   * or (when {@code isDirectory} is {@code true}) any child path under {@code path}.
   */
  public @NotNull Set<String> getDependentRegistrarFilesForPathOrPrefix(@NotNull String path, boolean isDirectory) {
    lock.readLock().lock();
    try {
      Set<String> result = new HashSet<>();
      Set<String> exact = registrarsByTargetPath.get(path);
      if (exact != null) {
        result.addAll(exact);
      }
      if (isDirectory) {
        String prefix = path + "/";
        for (Map.Entry<String, Set<String>> entry : registrarsByTargetPath.entrySet()) {
          if (entry.getKey().startsWith(prefix)) {
            result.addAll(entry.getValue());
          }
        }
      }
      return result.isEmpty() ? Set.of() : Set.copyOf(result);
    }
    finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Returns the paths of all registrar files that reference a target class with the given short name.
   */
  public @NotNull Set<String> getDependentRegistrarFilesByClassName(@NotNull String className) {
    lock.readLock().lock();
    try {
      Set<String> registrars = registrarsByTargetClassName.get(className);
      return registrars != null ? Set.copyOf(registrars) : Set.of();
    }
    finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Returns a snapshot of all target file paths currently referenced by registrar files.
   */
  public @NotNull Set<String> getTrackedTargetPaths() {
    lock.readLock().lock();
    try {
      return registrarsByTargetPath.isEmpty() ? Set.of() : Set.copyOf(registrarsByTargetPath.keySet());
    }
    finally {
      lock.readLock().unlock();
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
        if (candidate.getRole() == oppositeRole
            && entry.getKey().matches(candidate.getKey())
            && candidate.isValid()) {
          result.add(candidate);
        }
      }
      if (oppositeRole == EntryRole.BINDING_SITE && result.size() > 1) {
        boolean hasExplicit = false;
        for (GuiceEntry cp : result) {
          if (!cp.isDefaultBinding()) {
            hasExplicit = true;
            break;
          }
        }
        if (hasExplicit) {
          result.removeIf(GuiceEntry::isDefaultBinding);
        }
      }
      else if (entry.isDefaultBinding() && !result.isEmpty()) {
        result.removeIf(cp -> hasExplicitBinding(candidates, cp.getKey()));
      }
      return result;
    }
    finally {
      lock.readLock().unlock();
    }
  }

  private static boolean hasExplicitBinding(@NotNull Set<GuiceEntry> candidates, @NotNull GuiceBindingKey key) {
    for (GuiceEntry candidate : candidates) {
      if (candidate.getRole() == EntryRole.BINDING_SITE
          && !candidate.isDefaultBinding()
          && key.matches(candidate.getKey())
          && candidate.isValid()) {
        return true;
      }
    }
    return false;
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
        if (candidate.getRole() == role
            && key.matches(candidate.getKey())
            && candidate.isValid()) {
          result.add(candidate);
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

    // Remove from cross-file dependency indices
    for (String targetPath : old.referencedTargetPaths()) {
      Set<String> registrars = registrarsByTargetPath.get(targetPath);
      if (registrars != null) {
        registrars.remove(filePath);
        if (registrars.isEmpty()) {
          registrarsByTargetPath.remove(targetPath);
        }
      }
    }
    for (String className : old.referencedTargetClassNames()) {
      Set<String> registrars = registrarsByTargetClassName.get(className);
      if (registrars != null) {
        registrars.remove(filePath);
        if (registrars.isEmpty()) {
          registrarsByTargetClassName.remove(className);
        }
      }
    }
  }

  private void addFileEntries(@NotNull String filePath,
                              @NotNull GuiceEntryProducer.FileExtractionResult extractionResult) {
    Set<GuiceEntry> entries = extractionResult.entries();
    Set<String> targetPaths = extractionResult.referencedTargetPaths();
    Set<String> targetClassNames = extractionResult.referencedTargetClassNames();
    if (entries.isEmpty() && targetPaths.isEmpty() && targetClassNames.isEmpty()) return;

    Map<String, Set<GuiceEntry>> keyToEntries = new HashMap<>();

    for (GuiceEntry entry : entries) {
      String fqn = entry.getKey().getTypeFqn();
      if (fqn == null) continue;

      entriesByTypeFqn.computeIfAbsent(fqn, k -> new HashSet<>()).add(entry);
      keyToEntries.computeIfAbsent(fqn, k -> new HashSet<>()).add(entry);

      // Populate anchor index
      entriesByAnchor.computeIfAbsent(entry.getGutterAnchorPointer(), k -> new HashSet<>()).add(entry);
    }

    for (String targetPath : targetPaths) {
      registrarsByTargetPath.computeIfAbsent(targetPath, k -> new HashSet<>()).add(filePath);
    }
    for (String className : targetClassNames) {
      registrarsByTargetClassName.computeIfAbsent(className, k -> new HashSet<>()).add(filePath);
    }

    entriesByFile.put(filePath, new FileEntries(entries, keyToEntries, targetPaths, targetClassNames));
  }
}
