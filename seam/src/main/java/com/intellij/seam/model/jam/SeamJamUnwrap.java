package com.intellij.seam.model.jam;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.seam.constants.SeamAnnotationConstants;
import org.jetbrains.annotations.Nullable;

public class SeamJamUnwrap extends JamBaseElement<PsiMethod> {
  public static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(SeamAnnotationConstants.UNWRAP_ANNOTATION);

  public SeamJamUnwrap(PsiElementRef<?> ref) {
    super(ref);
  }

  @Nullable
  public PsiType getType() {
    return getPsiElement().getReturnType();
  }

  public PsiAnnotation getIdentifyingAnnotation() {
    return ANNOTATION_META.getAnnotation(getPsiElement());
  }
}