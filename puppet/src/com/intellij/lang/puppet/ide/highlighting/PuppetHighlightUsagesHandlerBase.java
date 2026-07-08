package com.intellij.lang.puppet.ide.highlighting;

import com.intellij.codeInsight.highlighting.HighlightUsagesHandlerBase;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.lang.puppet.psi.resolve.PuppetResolveUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulator;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class PuppetHighlightUsagesHandlerBase extends HighlightUsagesHandlerBase<PsiElement> implements PuppetTokenTypes {


  public PuppetHighlightUsagesHandlerBase(@NotNull Editor editor, @NotNull PsiFile file) {
    super(editor, file);
  }

  protected void addWriteUsage(@NotNull PsiElement element) {
    addUsageRange(myWriteUsages, getRangeToHighlight(element), element);
  }

  protected void addUsageRange(List<? super TextRange> result, TextRange range, PsiElement element) {
    if (range != null) {
      range = InjectedLanguageManager.getInstance(element.getProject()).injectedToHost(element, range);
      result.add(range);
    }
  }

  /**
   * Iterating through targets, find synonyms and populate writeUsages list
   *
   * @param currentFile    current file to mark write usages
   * @param currentTargets target elements to find synonyms for
   * @return list of currentTargets plus synonyms
   */
  protected @NotNull List<PsiElement> getTargetsWithSynonyms(@NotNull PsiFile currentFile,
                                                             @NotNull Collection<? extends PsiElement> currentTargets) {
    Set<PsiElement> result = new HashSet<>();
    Set<String> processedSynonyms = new HashSet<>();

    for (PsiElement targetElement : currentTargets) {
      if (result.add(targetElement) && currentFile.equals(targetElement.getContainingFile())) {
        addWriteUsage(targetElement.getNavigationElement());
      }

      PuppetResolveUtil.processElementSynonims(targetElement, currentFile, processedSynonyms, synonym -> {
        if (result.add(synonym) && currentFile.equals(synonym.getContainingFile())) {
          addWriteUsage(synonym.getNavigationElement());
        }
        return true;
      });
    }
    return new ArrayList<>(result);
  }

  @Override
  protected void selectTargets(@NotNull List<? extends PsiElement> targets, @NotNull Consumer<? super List<? extends PsiElement>> selectionConsumer) {
    selectionConsumer.consume(targets);
  }

  @Override
  public void computeUsages(@NotNull List<? extends PsiElement> targets) {
    for (PsiElement target : targets) {
      for (PsiReference reference : ReferencesSearch.search(target, GlobalSearchScope.fileScope(myFile)).asIterable()) {
        PsiElement sourceElement = reference.getElement();
        addUsageRange(myReadUsages, reference.getRangeInElement().shiftRight(sourceElement.getNode().getStartOffset()), sourceElement);
      }
    }
  }

  private static @Nullable TextRange getRangeToHighlight(PsiElement element) {
    ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(element);
    return manipulator == null
           ? element.getTextRange()
           : manipulator.getRangeInElement(element).shiftRight(element.getNode().getStartOffset());
  }
}
