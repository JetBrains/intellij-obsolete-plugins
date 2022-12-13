package com.intellij.seam.model.jam.bijection;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.psi.*;
import com.intellij.seam.constants.SeamAnnotationConstants;
import org.jetbrains.annotations.NotNull;

/**
 * @author Serega.Vasiliev
 */
@Presentation(typeName = SeamJamOutjection.SEAM_OUTJECTION_OUT)
public abstract class SeamJamOutjection<T extends PsiMember & PsiNamedElement> extends SeamJamBijection<T> {
  public static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(SeamAnnotationConstants.OUT_ANNOTATION);
  public static final String SEAM_OUTJECTION_OUT = "Seam outjection (@Out)";

  public SeamJamOutjection(T member) {
    super(member);
  }

  @Override
  @NotNull
  public JamAnnotationMeta getJamAnnotationMeta() {
    return ANNOTATION_META;
  }

  public static class Method extends SeamJamOutjection<PsiMethod> {
    public Method(PsiMethod member) {
      super(member);
    }

    @Override
    public PsiType getType() {
      return getPsiElement().getReturnType();
    }
  }

  public static class Field extends SeamJamOutjection<PsiField> {
    public Field(PsiField member) {
      super(member);
    }

    @Override
    public PsiType getType() {
      return getPsiElement().getType();
    }
  }
}
