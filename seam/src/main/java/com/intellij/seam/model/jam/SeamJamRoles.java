package com.intellij.seam.model.jam;

import com.intellij.jam.JamBaseElement;
import com.intellij.jam.reflect.JamAnnotationAttributeMeta;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementRef;
import com.intellij.seam.constants.SeamAnnotationConstants;

public class SeamJamRoles extends JamBaseElement<PsiClass> {
  public static final JamAnnotationAttributeMeta.Collection<SeamJamRole> ROLES_COLLECTION_ATTRIBUTE =
    JamAttributeMeta.annoCollection("value", SeamJamRole.ANNOTATION_META, SeamJamRole.class).addPomTargetProducer(
      (seamJamRole, consumer) -> consumer.consume(seamJamRole.getPsiTarget()));

  public static final JamAnnotationMeta ANNOTATION_META =
    new JamAnnotationMeta(SeamAnnotationConstants.ROLES_ANNOTATION).addAttribute(ROLES_COLLECTION_ATTRIBUTE);

  public static final JamClassMeta<SeamJamRoles> META = new JamClassMeta<>(SeamJamRoles.class)
    .addAnnotation(ANNOTATION_META);

  public SeamJamRoles(PsiElementRef<?> ref) {
    super(ref);
  }
}