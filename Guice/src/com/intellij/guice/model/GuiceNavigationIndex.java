// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  /** Secondary index: entries by the position of their gutter anchor for O(1) annotator lookups.
   *  See {@link GuiceEntry.AnchorKey} for why the key is a position and not a smart pointer. */
  private final Map<GuiceEntry.AnchorKey, Set<GuiceEntry>> entriesByAnchor = new HashMap<>();

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
   * Result of a file update or removal.
   *
   * @param changed          whether the stored data of the file changed
   * @param affectedTypeFqns the type FQNs whose entry sets changed; the files with entries of these types
   *                         may show different counterparts now, see {@link #getFilesWithEntriesOfTypes}
   */
  public record FileUpdate(boolean changed, @NotNull Set<String> affectedTypeFqns) {
    public static final FileUpdate NONE = new FileUpdate(false, Set.of());
  }

  /**
   * Updates the index with entries and cross-file target dependencies from a single file.
   *
   * <p>{@link GuiceEntry#equals} compares smart pointers by their current range, so an entry that only moved
   * because of an edit above it is equal to its stored counterpart. An absent {@link FileEntries} stands for
   * an empty result, as {@link #addFileEntries} stores no empty result.
   *
   * @return {@link FileUpdate#NONE} if the new extraction result equals the stored one,
   * so no re-highlighting is necessary
   */
  public @NotNull FileUpdate updateFile(@NotNull String filePath,
                                        @NotNull GuiceEntryProducer.FileExtractionResult extractionResult) {
    // Entry equality goes through smart pointers, whose equals() calls checkCanceled(). A mutation must not stop
    // halfway, so it runs in a non-cancelable section (see GuiceEntry.equals).
    return ProgressManager.getInstance().computeInNonCancelableSection(() -> doUpdateFile(filePath, extractionResult));
  }

  private @NotNull FileUpdate doUpdateFile(@NotNull String filePath,
                                           @NotNull GuiceEntryProducer.FileExtractionResult extractionResult) {
    lock.writeLock().lock();
    try {
      FileEntries old = entriesByFile.get(filePath);
      Map<String, Set<GuiceEntry>> newKeyToEntries = groupByTypeFqn(extractionResult.entries());
      Map<String, Set<GuiceEntry>> oldKeyToEntries = old != null ? old.keyToEntries() : Map.of();

      Set<String> affected = new HashSet<>();
      for (String fqn : oldKeyToEntries.keySet()) {
        if (!oldKeyToEntries.get(fqn).equals(newKeyToEntries.get(fqn))) affected.add(fqn);
      }
      for (String fqn : newKeyToEntries.keySet()) {
        if (!newKeyToEntries.get(fqn).equals(oldKeyToEntries.get(fqn))) affected.add(fqn);
      }
      Set<String> oldPaths = old != null ? old.referencedTargetPaths() : Set.of();
      Set<String> oldClassNames = old != null ? old.referencedTargetClassNames() : Set.of();
      boolean changed = !affected.isEmpty()
                        || !oldPaths.equals(extractionResult.referencedTargetPaths())
                        || !oldClassNames.equals(extractionResult.referencedTargetClassNames());
      // Replace the stored entries also when they are equal: the new entries carry the anchor positions
      // of the current text, and the anchor index is keyed by position.
      removeFileEntries(filePath);
      addFileEntries(filePath, extractionResult, newKeyToEntries);
      return changed ? new FileUpdate(true, Set.copyOf(affected)) : FileUpdate.NONE;
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Removes all entries contributed by a file.
   */
  public @NotNull FileUpdate removeFile(@NotNull String filePath) {
    return ProgressManager.getInstance().computeInNonCancelableSection(() -> {
      lock.writeLock().lock();
      try {
        FileEntries old = removeFileEntries(filePath);
        return old == null ? FileUpdate.NONE : new FileUpdate(true, Set.copyOf(old.keyToEntries().keySet()));
      }
      finally {
        lock.writeLock().unlock();
      }
    });
  }

  /**
   * Returns the files that hold entries of the given types. Their gutter icons may show different
   * counterparts after a change of these types. Uses the smart pointers only, without PSI access.
   */
  public @NotNull Set<VirtualFile> getFilesWithEntriesOfTypes(@NotNull Collection<String> typeFqns) {
    lock.readLock().lock();
    try {
      Set<VirtualFile> files = new HashSet<>();
      for (String fqn : typeFqns) {
        Set<GuiceEntry> entries = entriesByTypeFqn.get(fqn);
        if (entries == null) continue;
        for (GuiceEntry entry : entries) {
          VirtualFile file = entry.getGutterAnchorPointer().getVirtualFile();
          if (file != null) files.add(file);
        }
      }
      return files;
    }
    finally {
      lock.readLock().unlock();
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
      GuiceEntry.AnchorKey key = GuiceEntry.AnchorKey.of(anchor);
      if (key == null) return Set.of();
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

  private @Nullable FileEntries removeFileEntries(@NotNull String filePath) {
    FileEntries old = entriesByFile.remove(filePath);
    if (old == null) return null;

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
      GuiceEntry.AnchorKey anchorKey = entry.getAnchorKey();
      if (anchorKey == null) continue;
      Set<GuiceEntry> anchorSet = entriesByAnchor.get(anchorKey);
      if (anchorSet != null) {
        anchorSet.remove(entry);
        if (anchorSet.isEmpty()) entriesByAnchor.remove(anchorKey);
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
    return old;
  }

  private static @NotNull Map<String, Set<GuiceEntry>> groupByTypeFqn(@NotNull Set<GuiceEntry> entries) {
    if (entries.isEmpty()) return Map.of();
    Map<String, Set<GuiceEntry>> keyToEntries = new HashMap<>();
    for (GuiceEntry entry : entries) {
      String fqn = entry.getKey().getTypeFqn();
      if (fqn == null) continue;
      keyToEntries.computeIfAbsent(fqn, k -> new HashSet<>()).add(entry);
    }
    return keyToEntries;
  }

  private void addFileEntries(@NotNull String filePath,
                              @NotNull GuiceEntryProducer.FileExtractionResult extractionResult,
                              @NotNull Map<String, Set<GuiceEntry>> keyToEntries) {
    Set<GuiceEntry> entries = extractionResult.entries();
    Set<String> targetPaths = extractionResult.referencedTargetPaths();
    Set<String> targetClassNames = extractionResult.referencedTargetClassNames();
    if (entries.isEmpty() && targetPaths.isEmpty() && targetClassNames.isEmpty()) return;

    for (Map.Entry<String, Set<GuiceEntry>> e : keyToEntries.entrySet()) {
      entriesByTypeFqn.computeIfAbsent(e.getKey(), k -> new HashSet<>()).addAll(e.getValue());
    }
    for (GuiceEntry entry : entries) {
      GuiceEntry.AnchorKey anchorKey = entry.getAnchorKey();
      if (entry.getKey().getTypeFqn() == null || anchorKey == null) continue;
      entriesByAnchor.computeIfAbsent(anchorKey, k -> new HashSet<>()).add(entry);
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
