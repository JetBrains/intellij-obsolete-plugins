package com.intellij.seam.model.jam;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.seam.constants.SeamAnnotationConstants;
import com.intellij.seam.model.SeamInstallPrecedence;

public class SeamJamInstall extends JamBaseElement<PsiClass> {
  public static final JamClassMeta<SeamJamInstall> META = new JamClassMeta<>(SeamJamInstall.class);

  private static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(SeamAnnotationConstants.INSTALL_ANNOTATION);

  public SeamJamInstall(PsiElementRef<?> ref) {
    super(ref);
  }

  public int getPrecedence() {
    PsiAnnotation annotation = ANNOTATION_META.getAnnotation(getPsiElement());
    if (annotation == null) return SeamInstallPrecedence.APPLICATION;
    final Integer value = JamCommonUtil.getObjectValue(annotation.findDeclaredAttributeValue("precedence"), Integer.class);

    return value == null ? SeamInstallPrecedence.APPLICATION : value;
  }

  public boolean isInstall() {
    PsiAnnotation annotation = ANNOTATION_META.getAnnotation(getPsiElement());
    if (annotation == null) return true;

    Boolean value = JamCommonUtil.getObjectValue(annotation.findDeclaredAttributeValue("value"), Boolean.class);

    return value == null || value;
  }
}