package com.intellij.gwt.codeInsight;

import com.intellij.facet.ProjectFacetManager;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.InitialPatternCondition;
import com.intellij.patterns.ObjectPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.Nullable;

public final class GwtPatterns {
  private GwtPatterns() {
  }

  public static <T extends PsiElement> ElementPattern<T> inModuleWithGwtFacet(Class<T> aClass) {
    return new ObjectPattern.Capture<>(new InitialPatternCondition<>(aClass) {
      @Override
      public boolean accepts(@Nullable Object o, ProcessingContext context) {
        return o instanceof PsiElement && GwtFacet.findFacetByPsiElement((PsiElement)o) != null;
      }
    });
  }

  public static <T extends PsiElement> ElementPattern<T> inProjectWithGwtFacet(Class<T> aClass) {
    return new ObjectPattern.Capture<>(new InitialPatternCondition<>(aClass) {
      @Override
      public boolean accepts(@Nullable Object o, ProcessingContext context) {
        if (!(o instanceof PsiElement element)) return false;
        return ProjectFacetManager.getInstance(element.getProject()).hasFacets(GwtFacetType.ID);
      }
    });
  }

}
