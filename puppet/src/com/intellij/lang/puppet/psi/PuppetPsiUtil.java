package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.lexer.PuppetLexerBase;
import com.intellij.lang.puppet.lexer.PuppetTokenTypeSets;
import com.intellij.lang.puppet.util.PuppetElementFactory;
import com.intellij.psi.ElementManipulator;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.intellij.lang.puppet.PuppetTokenTypes.DEFAULT_WRAPPER;
import static com.intellij.lang.puppet.PuppetTokenTypes.QUOTED_TEXT;
import static com.intellij.lang.puppet.PuppetTokenTypes.REGULAR_NAME_WRAPPER;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.RESOURCE_NAME_CONTAINERS;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.RESOURCE_NAME_HOLDERS;
import static com.intellij.lang.puppet.psi.PuppetResourceInstanceDeclaration.HEAVY_NAME;

public final class PuppetPsiUtil {

  /**
   * Searches for previous sibling, skipping whitespaces and comments
   *
   * @param element element to start from
   */
  public static @Nullable PsiElement getPrevNonSpaceSibling(@NotNull PsiElement element) {
    while ((element = element.getPrevSibling()) != null) {
      if (!PuppetTokenTypeSets.WHITESPACE_OR_COMMENTS.contains(PsiUtilCore.getElementType(element))) {
        return element;
      }
    }
    return null;
  }

  public static PsiElement setName(@NotNull PsiNameIdentifierOwner element, @NotNull String name) {
    PsiElement identifier = element.getNameIdentifier();

    if (identifier == null) {
      return element;
    }

    if (identifier instanceof LeafPsiElement) {
      ((LeafPsiElement)identifier).replaceWithText(name);
    }

    ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(identifier);
    if (manipulator != null) {
      manipulator.handleContentChange(identifier, name);
    }

    return element;
  }

  /**
   * Utility method for processing psi elements recursively, entering specific elements
   *
   * @param currentElement    element to start, inclusive
   * @param processor            result processor
   * @param elementsToProcess name holders to collect
   * @param elementsToEnter   containers to step into
   */
  // fixme we could wrap this into class to avoid passing many arguments recursively, but good for now
  public static boolean processPsiElementsRecursively(@Nullable PsiElement currentElement,
                                                      @NotNull Processor<? super PsiElement> processor,
                                                      @NotNull TokenSet elementsToProcess,
                                                      @NotNull TokenSet elementsToEnter) {
    while (currentElement != null) {
      IElementType currentElementType = PsiUtilCore.getElementType(currentElement);
      if (elementsToProcess.contains(currentElementType)) {
        if (!processor.process(currentElement)) {
          return false;
        }
      }
      else if (elementsToEnter.contains(currentElementType)) {
        if (!processPsiElementsRecursively(currentElement.getFirstChild(), processor, elementsToProcess, elementsToEnter)) {
          return false;
        }
      }
      currentElement = currentElement.getNextSibling();
    }
    return true;
  }

  public static @NotNull List<String> computeResourceLikeNamesList(PuppetPolyNamedElement polyNamedElement) {
    List<PsiElement> identifiersList = polyNamedElement.getNameIdentifiersList();
    if (identifiersList.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> result = new ArrayList<>();

    for (PsiElement identifier : identifiersList) {
      String name = polyNamedElement.getNameFromIdentifier(identifier);
      if (name != null) {
        result.add(name);
      }
    }

    return result;
  }

  public static String getResourceLikeNameFromIdentifier(PsiElement nameIdentifier) {
    IElementType type = PsiUtilCore.getElementType(nameIdentifier);

    // fixme possible holders should be self-resolvable
    if (type == QUOTED_TEXT || type == REGULAR_NAME_WRAPPER) {
      return ElementManipulators.getValueText(nameIdentifier);
    }
    else if (type == DEFAULT_WRAPPER) {
      return PuppetDefaultWrapper.DEFAULT_NAME;
    }

    return HEAVY_NAME;
  }

  public static List<PsiElement> getResourceLikeIdentifiersList(PsiElement firstChild) {
    if (RESOURCE_NAME_HOLDERS.contains(PsiUtilCore.getElementType(firstChild))) {
      return Collections.singletonList(firstChild);
    }
    return collectPsiElementsRecursively(firstChild.getFirstChild(), RESOURCE_NAME_HOLDERS, RESOURCE_NAME_CONTAINERS);
  }

  public static List<PsiElement> collectPsiElementsRecursively(@Nullable PsiElement currentElement,
                                                               @NotNull TokenSet elementsToCollect,
                                                               @NotNull TokenSet elementsToEnter)

  {
    if (currentElement == null) {
      return Collections.emptyList();
    }

    CommonProcessors.CollectProcessor<PsiElement> processor = new CommonProcessors.CollectProcessor<>(new ArrayList<>());
    processPsiElementsRecursively(currentElement,
                                  processor,
                                  elementsToCollect,
                                  elementsToEnter);
    return (List<PsiElement>)processor.getResults();
  }

  /**
   * Renames identifier in resource instance usage or declaration, changing bareword to single-quoted string if necessary
   *
   * @param nameIdentifier current identifier element
   * @param newName        new name
   * @return new identifier element or null if something bad happened
   */
  public static @Nullable PsiElement renameResourceInstanceIdentifier(@Nullable PsiElement nameIdentifier, @NotNull String newName) {
    if (nameIdentifier == null) {
      return null;
    }

    if (PsiUtilCore.getElementType(nameIdentifier) == REGULAR_NAME_WRAPPER &&
        !PuppetLexerBase.IDENTIFIER_PATTERN.matcher(newName).matches()) {
      return nameIdentifier.replace(PuppetElementFactory.createQuotedStringElementWithContent(nameIdentifier.getProject(), newName));
    }

    return ElementManipulators.handleContentChange(nameIdentifier, newName);
  }
}
