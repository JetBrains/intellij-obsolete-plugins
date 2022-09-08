package com.intellij.seam.model.jam.context;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamConverter;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMethod;
import com.intellij.seam.constants.SeamAnnotationConstants;
import org.jetbrains.annotations.Nullable;

public class SeamJamBegin extends JamBaseElement<PsiMethod> {
  public static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(SeamAnnotationConstants.BEGIN_ANNOTATION);

  public static final JamStringAttributeMeta.Single<String> CONVERSATION_META = JamAttributeMeta.singleString("conversation");
  public static final JamStringAttributeMeta.Single<Boolean> JOIN_META =
    JamAttributeMeta.singleString("join", JamConverter.BOOLEAN_CONVERTER);
  public static final JamStringAttributeMeta.Single<Boolean> NESTED_META =
    JamAttributeMeta.singleString("nested", JamConverter.BOOLEAN_CONVERTER);

  public SeamJamBegin(PsiElementRef<?> ref) {
    super(ref);
  }

  @Nullable
  public String conversation() {
    return ANNOTATION_META.getAttribute(getPsiElement(), CONVERSATION_META).getStringValue();
  }

  public boolean isJoin() {
    return ANNOTATION_META.getAttribute(getPsiElement(), JOIN_META).getValue();
  }

  public boolean isNested() {
    return ANNOTATION_META.getAttribute(getPsiElement(), NESTED_META).getValue();
  }

  public PsiElement getIdentifyingAnnotation() {
    return ANNOTATION_META.getAnnotation(getPsiElement());
  }
}