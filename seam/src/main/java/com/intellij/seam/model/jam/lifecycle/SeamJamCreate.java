package com.intellij.seam.model.jam.lifecycle;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMethod;
import com.intellij.seam.constants.SeamAnnotationConstants;

public class SeamJamCreate extends JamBaseElement<PsiMethod> {
  public static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(SeamAnnotationConstants.CREATE_ANNOTATION);

  public SeamJamCreate(PsiElementRef<?> ref) {
    super(ref);
  }

  public PsiAnnotation getIdentifyingAnnotation() {
    return ANNOTATION_META.getAnnotation(getPsiElement());
  }
}