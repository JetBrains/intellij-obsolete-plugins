package com.intellij.gwt.references;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.gwt.GwtTestCase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReference;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class GwtReferencesTestCase extends GwtTestCase {
  protected static void assertUnresolvable(final PsiLiteralExpression unresolvable) {
    assertEquals(0, getResolveResults(unresolvable).size());
  }

  protected static <T extends PsiElement> T assertResolvesTo(PsiElement element, Class<T> resolvedClass) {
    return assertInstanceOf(assertOneElement(getResolveResults(element)), resolvedClass);
  }

  protected static List<PsiElement> getResolveResults(final PsiElement element) {
    return Arrays.stream(element.getReferences()).map(PsiReference::resolve).filter(Objects::nonNull).collect(Collectors.toList());
  }

  protected static void assertVariantsContains(final PsiElement expression, String... variants) {
    final PsiReference[] references = expression.getReferences();
    Set<String> allVariants =
      Arrays.stream(references)
        .flatMap(ref -> Arrays.stream(ref.getVariants()))
        .map(GwtReferencesTestCase::getLookupString)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    assertTrue("All variants: " + allVariants, allVariants.containsAll(Arrays.asList(variants)));
  }

  private static String getLookupString(Object variant) {
    if (variant instanceof String) {
      return (String)variant;
    }
    else if (variant instanceof LookupElement) {
      return ((LookupElement)variant).getLookupString();
    }
    else {
      return null;
    }
  }
}
