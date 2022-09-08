package com.intellij.seam.model.jam.bijection;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.psi.*;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.seam.constants.SeamAnnotationConstants;
import org.jetbrains.annotations.NotNull;

/**
 * @author Serega.Vasiliev
 */
@Presentation(typeName = SeamJamInjection.SEAM_INJECTION_IN)
public abstract class SeamJamInjection<T extends PsiMember & PsiNamedElement> extends SeamJamBijection<T> {
  public static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(SeamAnnotationConstants.IN_ANNOTATION);
  public static final String SEAM_INJECTION_IN = "Seam Injection (@In)";

  public SeamJamInjection(T member) {
    super(member);
  }

  @Override
  @NotNull
  protected JamAnnotationMeta getJamAnnotationMeta() {
    return ANNOTATION_META;
  }

  public static class Method extends SeamJamInjection<PsiMethod> {
    public Method(PsiMethod member) {
      super(member);
    }

    @Override
    public PsiType getType() {
      return PropertyUtilBase.getPropertyType(getPsiElement());
    }
  }

  public static class Field extends SeamJamInjection<PsiField> {
    public Field(PsiField member) {
      super(member);
    }

    @Override
    public PsiType getType() {
      return getPsiElement().getType();
    }
  }

  public boolean isCreate() {
    PsiAnnotation annotation = getIdentifyingAnnotation();
    Boolean value = null;
    if (annotation != null) {
      value = JamCommonUtil.getObjectValue(annotation.findDeclaredAttributeValue("create"), Boolean.class);
    }

    return value != null && value;
  }

}
