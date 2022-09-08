package com.intellij.seam.model.jam.context;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMethod;
import com.intellij.seam.constants.SeamAnnotationConstants;

public class SeamJamEnd extends JamBaseElement<PsiMethod> {
  public static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(SeamAnnotationConstants.END_ANNOTATION);

  public SeamJamEnd(PsiElementRef<?> ref) {
    super(ref);
  }

  public PsiElement getIdentifyingAnnotation() {
    return ANNOTATION_META.getAnnotation(getPsiElement());
  }
}