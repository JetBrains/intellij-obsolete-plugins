package com.intellij.seam.model.jam.dataModel;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.psi.*;
import com.intellij.seam.constants.SeamAnnotationConstants;
import org.jetbrains.annotations.Nullable;

public abstract class SeamJamDataModelSelectionIndex<T extends PsiMember & PsiNamedElement> extends JamBaseElement<T> {
  public static final JamAnnotationMeta ANNOTATION_META =
    new JamAnnotationMeta(SeamAnnotationConstants.JSF_DATA_MODEL_SELECTION_INDEX_ANNOTATION);

  public SeamJamDataModelSelectionIndex(T member) {
    super(PsiElementRef.real(member));
  }

  public static class Method extends SeamJamDataModelSelectionIndex<PsiMethod> {
    public Method(PsiMethod member) {
      super(member);
    }
  }

  public static class Field extends SeamJamDataModelSelectionIndex<PsiField> {
    public Field(PsiField member) {
      super(member);
    }
  }

  @Nullable
  public PsiAnnotation getIdentifyingAnnotation() {
    return ANNOTATION_META.getAnnotation(getPsiElement());
  }
}