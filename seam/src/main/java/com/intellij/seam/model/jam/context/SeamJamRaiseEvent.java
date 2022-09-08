package com.intellij.seam.model.jam.context;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.psi.PsiElementRef;
import com.intellij.psi.PsiMethod;
import com.intellij.seam.constants.SeamAnnotationConstants;

/**
 * @author Serega.Vasiliev
 */
public class SeamJamRaiseEvent extends JamBaseElement<PsiMethod> {
  public static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(SeamAnnotationConstants.RAISE_EVENT_ANNOTATION);

  public static final JamStringAttributeMeta.Single<String> NAME_META = JamAttributeMeta.singleString("value");

  public SeamJamRaiseEvent(PsiElementRef<?> ref) {
    super(ref);
  }

  public String getEventType() {
    return ANNOTATION_META.getAttribute(getPsiElement(), NAME_META).getStringValue();
  }
}