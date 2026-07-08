package com.intellij.lang.puppet.psi;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class PuppetElementPatternConditions implements PuppetElementPatterns {
  public static PatternCondition<PsiElement> firstChildOfParent(PsiElementPattern parentCondition) {
    return new PatternCondition<>("firstChildOfParent") {
      @Override
      public boolean accepts(@NotNull PsiElement element, ProcessingContext context) {
        PsiElement parent = element.getParent();
        return parent != null &&
               element.getPrevSibling() == null &&
               parentCondition.accepts(parent, context);
      }
    };
  }

  public static PatternCondition<PsiElement> firstChildOfParent(IElementType... parentTypes) {
    return firstChildOfParent(TokenSet.create(parentTypes));
  }

  public static PatternCondition<PsiElement> firstChildOfParent(TokenSet parentTypes) {
    return new PatternCondition<>("firstChildOfParent") {
      @Override
      public boolean accepts(@NotNull PsiElement element, ProcessingContext context) {
        PsiElement parent = element.getParent();
        return parent != null &&
               element.getPrevSibling() == null &&
               parentTypes.contains(PsiUtilCore.getElementType(parent));
      }
    };
  }

  public static PatternCondition<PsiElement> firstChildOrAfterTokenInParent(ElementPattern<PsiElement> parentCondition, TokenSet prefix) {
    return new PatternCondition<>("firstChildOrAfterTokenInParent") {
      @Override
      public boolean accepts(@NotNull PsiElement element, ProcessingContext context) {
        if (!parentCondition.accepts(element.getParent())) {
          return false;
        }

        PsiElement prevSibling = element.getPrevSibling();
        return prevSibling == null || prevSibling.getPrevSibling() == null && prefix.contains(PsiUtilCore.getElementType(prevSibling));
      }
    };
  }

  public static PatternCondition<PsiElement> nonFirstChildOfParent(ElementPattern<PsiElement> parentCondition) {
    return new PatternCondition<>("nonFirstChildOfParent") {
      @Override
      public boolean accepts(@NotNull PsiElement element, ProcessingContext context) {
        PsiElement parent = element.getParent();
        return parent != null &&
               element.getPrevSibling() != null &&
               parentCondition.accepts(parent, context);
      }
    };
  }

}
