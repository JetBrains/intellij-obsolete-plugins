// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model.renderers;

import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer;
import com.intellij.guice.model.GuiceEntry;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.TestSourcesFilter;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil;
import com.intellij.platform.backend.presentation.TargetPresentation;
import com.intellij.psi.PsiAnchor;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * Unified target presentation renderer for Guice navigation popups.
 *
 * <p>Delegates text and icon rendering to the {@link GuiceEntry} model,
 * which computes the correct presentable text (via an optional text provider)
 * and extracts the icon from the navigation target's PSI element.
 *
 * <p>Presentations for all entries are computed lazily once in \(O(N)\) time,
 * including disambiguating duplicate {@code (fileName, presentableText)} pairs
 * with 1-based line numbers.
 */
public final class GuiceEntryTargetRenderer extends PsiTargetPresentationRenderer<PsiElement> {
  private final @Unmodifiable @NotNull List<GuiceEntry> myEntries;
  private final @NotNull NotNullLazyValue<Map<PsiElement, TargetPresentation>> myPresentations =
      NotNullLazyValue.lazy(this::computePresentations);

  public GuiceEntryTargetRenderer(@Unmodifiable @NotNull List<GuiceEntry> entries) {
    myEntries = entries;
  }

  public record SortedTargets(@Unmodifiable @NotNull List<GuiceEntry> entries,
                              @Unmodifiable @NotNull List<PsiElement> targets) {}

  private static final String[] EMPTY_SEGMENTS = new String[0];

  private record SortableEntry(@NotNull GuiceEntry entry,
                               @NotNull PsiElement target,
                               boolean isTest,
                               boolean isSameFile,
                               int upSteps,
                               int downSteps,
                               @NotNull String presentableText,
                               @NotNull String fileName,
                               @NotNull String filePath,
                               int textOffset) {}

  private static final Comparator<SortableEntry> ENTRY_COMPARATOR = (a, b) -> {
    // 1. Production files before test files
    int cmp = Boolean.compare(a.isTest, b.isTest);
    if (cmp != 0) return cmp;

    // 2. Targets in the same file as the context element come first
    cmp = Boolean.compare(b.isSameFile, a.isSameFile);
    if (cmp != 0) return cmp;

    // 3. Package hierarchy distance: fewer steps up from current package first, then fewer steps down
    cmp = Integer.compare(a.upSteps, b.upSteps);
    if (cmp != 0) return cmp;

    cmp = Integer.compare(a.downSteps, b.downSteps);
    if (cmp != 0) return cmp;

    // 4. Presentable text (case-insensitive, then exact)
    cmp = String.CASE_INSENSITIVE_ORDER.compare(a.presentableText, b.presentableText);
    if (cmp != 0) return cmp;
    cmp = a.presentableText.compareTo(b.presentableText);
    if (cmp != 0) return cmp;

    // 5. File name (case-insensitive, then exact)
    cmp = String.CASE_INSENSITIVE_ORDER.compare(a.fileName, b.fileName);
    if (cmp != 0) return cmp;
    cmp = a.fileName.compareTo(b.fileName);
    if (cmp != 0) return cmp;

    // 6. Full path: two files with the same name in different directories
    cmp = a.filePath.compareTo(b.filePath);
    if (cmp != 0) return cmp;

    // 7. Source offset in file (top-to-bottom line order)
    return Integer.compare(a.textOffset, b.textOffset);
  };

  /**
   * Resolves navigation targets and sorts entries relative to {@code contextElement}:
   * <ol>
   *   <li>Production sources before test sources ({@link TestSourcesFilter#isTestSources})</li>
   *   <li>Same file as {@code contextElement} first</li>
   *   <li>Package hierarchy proximity: {@code upSteps} ascending, then {@code downSteps} ascending</li>
   *   <li>Presentable text, file name, file path, and source offset</li>
   * </ol>
   */
  public static @NotNull SortedTargets sortTargets(@NotNull Collection<GuiceEntry> entries,
                                                   @NotNull PsiElement contextElement) {
    if (entries.isEmpty()) {
      return new SortedTargets(List.of(), List.of());
    }

    PsiFile contextFile = contextElement.getContainingFile();
    Project project = contextElement.getProject();
    String[] contextPkg = getPackageSegments(contextFile);

    List<SortableEntry> sortable = new ArrayList<>(entries.size());
    Set<PsiElement> seenTargets = new HashSet<>(entries.size());

    for (GuiceEntry entry : entries) {
      PsiElement target = entry.getNavigationTarget();
      if (target == null || !seenTargets.add(target)) continue;

      PsiFile targetFile = target.getContainingFile();
      VirtualFile vFile = targetFile != null ? targetFile.getVirtualFile() : null;
      boolean isTest = vFile != null && TestSourcesFilter.isTestSources(vFile, project);
      boolean isSameFile = contextFile != null && contextFile.equals(targetFile);

      String[] targetPkg = getPackageSegments(targetFile);
      int common = commonPrefixLength(contextPkg, targetPkg);
      int upSteps = contextPkg.length - common;
      int downSteps = targetPkg.length - common;

      String text = entry.getPresentableText();
      if (text == null) {
        text = SymbolPresentationUtil.getSymbolPresentableText(target);
      }
      if (text == null) {
        text = "";
      }
      String fileName = targetFile != null ? targetFile.getName() : "";
      String filePath = vFile != null ? vFile.getPath() : "";
      // No PsiElement.getTextOffset() here: it loads the AST of the target file, see GuiceEntry.getNavigationStartOffset.
      int offset = entry.getNavigationStartOffset();

      sortable.add(new SortableEntry(entry, target, isTest, isSameFile, upSteps, downSteps, text, fileName, filePath, offset));
    }

    sortable.sort(ENTRY_COMPARATOR);

    List<GuiceEntry> sortedEntries = new ArrayList<>(sortable.size());
    List<PsiElement> sortedTargets = new ArrayList<>(sortable.size());
    for (SortableEntry item : sortable) {
      sortedEntries.add(item.entry());
      sortedTargets.add(item.target());
    }
    return new SortedTargets(Collections.unmodifiableList(sortedEntries), Collections.unmodifiableList(sortedTargets));
  }

  private static String @NotNull [] getPackageSegments(@Nullable PsiFile file) {
    if (!(file instanceof PsiClassOwner classOwner)) {
      return EMPTY_SEGMENTS;
    }
    String packageName = classOwner.getPackageName();
    if (packageName.isEmpty()) {
      return EMPTY_SEGMENTS;
    }
    return packageName.split("\\.");
  }

  private static int commonPrefixLength(String @NotNull [] a, String @NotNull [] b) {
    int limit = Math.min(a.length, b.length);
    int i = 0;
    while (i < limit && a[i].equals(b[i])) {
      i++;
    }
    return i;
  }

  private record FileAndText(@NotNull String fileName, @NotNull String text) {}

  private record ResolvedEntry(@NotNull PsiElement target,
                               @NotNull GuiceEntry entry,
                               @NotNull String text,
                               @Nullable PsiFile file) {}

  private @NotNull Map<PsiElement, TargetPresentation> computePresentations() {
    if (myEntries.isEmpty()) return Map.of();

    List<ResolvedEntry> resolved = new ArrayList<>(myEntries.size());
    Set<PsiElement> seenTargets = new HashSet<>(myEntries.size());
    Set<FileAndText> seenKeys = new HashSet<>(myEntries.size());
    Set<FileAndText> duplicateKeys = new HashSet<>();

    for (GuiceEntry entry : myEntries) {
      PsiElement target = entry.getNavigationTarget();
      if (target == null || !seenTargets.add(target)) continue;

      String text = entry.getPresentableText();
      if (text == null) {
        text = SymbolPresentationUtil.getSymbolPresentableText(target);
      }
      PsiFile file = target.getContainingFile();
      if (file != null) {
        FileAndText key = new FileAndText(file.getName(), text);
        if (!seenKeys.add(key)) {
          duplicateKeys.add(key);
        }
      }
      resolved.add(new ResolvedEntry(target, entry, text, file));
    }

    Map<PsiElement, TargetPresentation> result = new HashMap<>(resolved.size());
    for (ResolvedEntry item : resolved) {
      String containerText = computeContainerText(item.target(), item.file(), item.text(), duplicateKeys);
      Icon icon = item.entry().getIcon();
      if (icon == null) {
        icon = defaultIcon(item.target());
      }
      result.put(item.target(), buildPresentation(item.target(), item.file(), item.text(), containerText, icon));
    }
    return result;
  }

  private static @Nullable String computeContainerText(@NotNull PsiElement target,
                                                       @Nullable PsiFile file,
                                                       @NotNull String text,
                                                       @NotNull Set<FileAndText> duplicateKeys) {
    if (file == null) return null;
    String fileName = file.getName();
    if (duplicateKeys.contains(new FileAndText(fileName, text))) {
      Document document = PsiDocumentManager.getInstance(target.getProject()).getDocument(file);
      if (document != null) {
        int offset = target.getTextOffset();
        if (offset >= 0 && offset <= document.getTextLength()) {
          int line = document.getLineNumber(offset) + 1;
          return fileName + ":" + line;
        }
      }
    }
    return fileName;
  }

  private static @NotNull TargetPresentation buildPresentation(@NotNull PsiElement element,
                                                               @Nullable PsiFile psiFile,
                                                               @NotNull String elementText,
                                                               @Nullable String containerText,
                                                               @Nullable Icon icon) {
    var builder = TargetPresentation.builder(elementText).icon(icon);

    VirtualFile virtualFile = psiFile != null ? psiFile.getVirtualFile() : null;
    if (virtualFile != null) {
      builder = builder.backgroundColor(
          VfsPresentationUtil.getFileBackgroundColor(element.getProject(), virtualFile));
    }

    if (containerText != null) {
      builder = builder.containerText(containerText);
    }

    return builder.presentation();
  }

  private static @Nullable Icon defaultIcon(@NotNull PsiElement element) {
    return element.getIcon(Iconable.ICON_FLAG_VISIBILITY | Iconable.ICON_FLAG_READ_STATUS);
  }

  /**
   * Finds the presentation of a target. The platform restores the targets from its own smart pointers, so a
   * target is normally the same instance as the entry's navigation target and the direct lookup hits. When
   * the instances differ (a light element or a re-created PSI), a second map keyed by {@link PsiAnchor} finds
   * the presentation in \(O(1)\); it is built once, on the first miss, so a popup of N items costs \(O(N)\)
   * and not \(O(N^2)\).
   */
  private @Nullable TargetPresentation findPresentation(@NotNull PsiElement element) {
    Map<PsiElement, TargetPresentation> presentations = myPresentations.getValue();
    TargetPresentation direct = presentations.get(element);
    if (direct != null) return direct;
    if (presentations.isEmpty() || !element.isValid()) return null;
    return myPresentationsByAnchor.getValue().get(PsiAnchor.create(element));
  }

  private final @NotNull NotNullLazyValue<Map<PsiAnchor, TargetPresentation>> myPresentationsByAnchor =
      NotNullLazyValue.lazy(() -> {
        Map<PsiElement, TargetPresentation> presentations = myPresentations.getValue();
        Map<PsiAnchor, TargetPresentation> result = new HashMap<>(presentations.size());
        for (Map.Entry<PsiElement, TargetPresentation> entry : presentations.entrySet()) {
          PsiElement target = entry.getKey();
          if (target.isValid()) {
            result.putIfAbsent(PsiAnchor.create(target), entry.getValue());
          }
        }
        return result;
      });

  @Override
  public @NotNull String getElementText(@NotNull PsiElement element) {
    TargetPresentation presentation = findPresentation(element);
    return presentation != null
           ? presentation.getPresentableText()
           : SymbolPresentationUtil.getSymbolPresentableText(element);
  }

  @Override
  public @Nullable String getContainerText(@NotNull PsiElement element) {
    TargetPresentation presentation = findPresentation(element);
    if (presentation != null) {
      return presentation.getContainerText();
    }
    PsiFile file = element.getContainingFile();
    return file != null ? file.getName() : null;
  }

  @Override
  public @Nullable Icon getIcon(@NotNull PsiElement element) {
    TargetPresentation presentation = findPresentation(element);
    return presentation != null ? presentation.getIcon() : defaultIcon(element);
  }

  @Override
  public @NotNull TargetPresentation getPresentation(@NotNull PsiElement element) {
    TargetPresentation presentation = findPresentation(element);
    if (presentation != null) {
      return presentation;
    }
    PsiFile file = element.getContainingFile();
    return buildPresentation(
        element,
        file,
        SymbolPresentationUtil.getSymbolPresentableText(element),
        file != null ? file.getName() : null,
        defaultIcon(element));
  }
}
