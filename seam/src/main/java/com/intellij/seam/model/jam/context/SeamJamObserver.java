package com.intellij.seam.model.jam.context;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.JamConverter;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.javaee.model.annotations.AnnotationGenericValue;
import com.intellij.javaee.model.annotations.AnnotationModelUtil;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMethod;
import com.intellij.seam.constants.SeamAnnotationConstants;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

/**
 * @author Serega.Vasiliev
 */
public class SeamJamObserver extends JamBaseElement<PsiMethod> {
  public static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(SeamAnnotationConstants.OBSERVER_ANNOTATION);

  public static final JamStringAttributeMeta.Single<Boolean> CREATE_META =
    JamAttributeMeta.singleString("create", JamConverter.BOOLEAN_CONVERTER);

  public SeamJamObserver(PsiElementRef<?> ref) {
    super(ref);
  }

  public String[] getEventTypes() {
    final List<AnnotationGenericValue<String>> list =
      AnnotationModelUtil.getStringArrayValue(ANNOTATION_META.getAnnotation(getPsiElement()), "value");
    return ContainerUtil.map2Array(list, String.class, genericValue -> genericValue.getValue());
  }

  boolean isCreate() {
    return ANNOTATION_META.getAttribute(getPsiElement(), CREATE_META).getValue();
  }
}