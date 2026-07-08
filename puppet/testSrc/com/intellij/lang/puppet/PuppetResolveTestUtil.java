package com.intellij.lang.puppet;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PuppetResolveTestUtil {
  public static List<PsiPolyVariantReference> getReferencesInFile(PsiFile file,
                                                                  final Class<? extends PsiPolyVariantReference> referenceClassToTest) {
    final List<PsiPolyVariantReference> referencesList = new ArrayList<>();

    file.accept(new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        for (PsiReference reference : element.getReferences()) {
          if (referenceClassToTest.isInstance(reference)) {
            referencesList.add(((PsiPolyVariantReference)reference));
          }
        }
        element.acceptChildren(this);
      }
    });

    referencesList.sort(Comparator.comparingInt(o -> o.getElement().getTextRange().getStartOffset()));

    return referencesList;
  }

  public static PsiPolyVariantReference getReferenceOfType(PsiReference reference,
                                                           final Class<? extends PsiPolyVariantReference> referenceClassToTest) {
    PsiPolyVariantReference result = null;
    if (reference instanceof PsiMultiReference) {
      for (PsiReference psiReference : ((PsiMultiReference)reference).getReferences()) {
        if (referenceClassToTest.isInstance(psiReference)) {
          if (result != null) {
            return null;
          }

          result = ((PsiPolyVariantReference)psiReference);
        }
      }
    }
    else if (referenceClassToTest.isInstance(reference)) {
      result = ((PsiPolyVariantReference)reference);
    }

    return result;
  }
}
