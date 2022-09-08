package com.intellij.seam.model.jam.jsf;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.JamBaseElement;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.seam.constants.SeamAnnotationConstants;
import org.jetbrains.annotations.Nullable;

@Presentation(typeName = SeamJamValidator.SEAM_VALIDATOR)
public class SeamJamValidator extends JamBaseElement<PsiClass> {
  public static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(SeamAnnotationConstants.JSF_VALIDATOR_ANNOTATION);
  public static final JamClassMeta<SeamJamValidator> META = new JamClassMeta<>(SeamJamValidator.class).addAnnotation(ANNOTATION_META);
  public static final String SEAM_VALIDATOR = "Seam Validator";

  public SeamJamValidator(PsiElementRef<?> ref) {
    super(ref);
  }

  @Nullable
  public String getId() {
    return JamCommonUtil.getObjectValue(getIdentifyingAnnotation().findDeclaredAttributeValue("id"), String.class);
  }

  public PsiAnnotation getIdentifyingAnnotation() {
    return ANNOTATION_META.getAnnotation(getPsiElement());
  }
}