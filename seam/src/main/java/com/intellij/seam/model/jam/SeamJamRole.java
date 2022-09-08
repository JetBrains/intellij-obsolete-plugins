package com.intellij.seam.model.jam;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.JamCommonModelElement;
import com.intellij.jam.JamElement;
import com.intellij.jam.JamPomTarget;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jam.reflect.JamAnnotationMeta;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.jam.reflect.JamClassMeta;
import com.intellij.jam.reflect.JamStringAttributeMeta;
import com.intellij.javaee.model.annotations.AnnotationModelUtil;
import com.intellij.pom.references.PomService;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.seam.constants.SeamAnnotationConstants;
import com.intellij.seam.model.SeamComponentScope;
import com.intellij.seam.utils.SeamCommonUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Presentation(typeName = SeamJamRole.SEAM_ROLE)
public class SeamJamRole extends JamCommonModelElement<PsiMember> implements JamElement, CommonModelElement {
  @NonNls public static final String NAME_ATTRIBUTE = "name";
  @NonNls public static final String SCOPE_ATTRIBUTE = "scope";
  public static final String SEAM_ROLE = "Seam Role";
  public static JamStringAttributeMeta.Single<String> NAME_ATTRIBUTE_META = JamAttributeMeta.singleString(SeamJamRole.NAME_ATTRIBUTE);

  public static final JamAnnotationMeta ANNOTATION_META =
    new JamAnnotationMeta(SeamAnnotationConstants.ROLE_ANNOTATION).addAttribute(NAME_ATTRIBUTE_META);

  public static final JamClassMeta<SeamJamRole> META = new JamClassMeta<>(SeamJamRole.class).addAnnotation(ANNOTATION_META).
    addPomTargetProducer((seamJamRole, consumer) -> consumer.consume(seamJamRole.getPsiTarget())
    );

  private final PsiElementRef<PsiAnnotation> myPsiAnnotation;

  public SeamJamRole(PsiMember psiMember) {
    super(PsiElementRef.real(psiMember));
    myPsiAnnotation = ANNOTATION_META.getAnnotationRef(psiMember);
  }

  public SeamJamRole(PsiAnnotation psiAnnotation) {
    super(PsiElementRef.real(Objects.requireNonNull(PsiTreeUtil.getParentOfType(psiAnnotation, PsiMember.class, true))));
    myPsiAnnotation = PsiElementRef.real(psiAnnotation);
  }

  protected JamStringAttributeElement<String> getNamedStringAttributeElement() {
    return NAME_ATTRIBUTE_META.getJam(myPsiAnnotation);
  }

  public PsiTarget getPsiTarget() {
    final JamStringAttributeElement<String> namedAttributeValue = getNamedStringAttributeElement();
    if (namedAttributeValue.getStringValue() != null) return new JamPomTarget(this, namedAttributeValue);

    return null;
  }

  @Override
  public PsiElement getIdentifyingPsiElement() {
    return PomService.convertToPsi(getPsiElement().getProject(), getPsiTarget());
  }

  @NonNls
  public String getName() {
    return NAME_ATTRIBUTE_META.getJam(myPsiAnnotation).getStringValue();
  }

  @Nullable
  public PsiAnnotation getIdentifyingAnnotation() {
    return myPsiAnnotation.getPsiElement();
  }

  @Nullable
  public PsiType getComponentType() {
    PsiClass psiClass = (PsiClass)getPsiElement();

    final PsiType unwrapType = SeamCommonUtils.getUnwrapType(psiClass);

    return unwrapType == null ? JavaPsiFacade.getInstance(psiClass.getProject()).getElementFactory().createType(psiClass) : unwrapType;
  }

  public SeamComponentScope getScope() {
    return AnnotationModelUtil.getEnumValue(getIdentifyingAnnotation(), SCOPE_ATTRIBUTE, SeamComponentScope.class).getValue();
  }
}
