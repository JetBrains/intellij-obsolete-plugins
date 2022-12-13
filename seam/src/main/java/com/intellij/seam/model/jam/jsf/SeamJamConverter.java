package com.intellij.seam.model.jam.jsf;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.*;
import com.intellij.jam.reflect.*;
import com.intellij.jsf.model.common.JsfCommonConverter;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.references.PomService;
import com.intellij.psi.*;
import com.intellij.psi.targets.DecapitalizedAliasingPsiTarget;
import com.intellij.seam.constants.SeamAnnotationConstants;
import com.intellij.semantic.SemKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Presentation(typeName = SeamJamConverter.SEAM_CONVERTER)
public class SeamJamConverter extends JamBaseElement<PsiClass> implements JamElement, JsfCommonConverter {
  public static final SemKey<SeamJamConverter> JAM_KEY = JamService.JAM_ALIASING_ELEMENT_KEY.subKey("SeamJamConverter");

  public static final JamClassMeta<SeamJamConverter> META = new JamClassMeta<>(null, SeamJamConverter.class, JAM_KEY)
    .addPomTargetProducer((converter, consumer) -> consumer.consume(converter.getPsiTarget()));

  private static final JamAnnotationMeta myMeta = new JamAnnotationMeta(SeamAnnotationConstants.JSF_CONVERTER_ANNOTATION);

  public static final JamStringAttributeMeta.Single<String> CONVERTER_ID_META = JamAttributeMeta.singleString("id");
  public static final JamClassAttributeMeta.Single FOR_CLASS_META = JamAttributeMeta.singleClass("forClass");
  public static final String SEAM_CONVERTER = "Seam Converter";

  public SeamJamConverter(PsiElementRef<?> ref) {
    super(ref);
  }

  @NotNull
  private JamStringAttributeElement<String> getConverterIdAttributeElement() {
    return myMeta.getAttribute(getPsiElement(), CONVERTER_ID_META);
  }

  @NotNull
  private JamClassAttributeElement getForClassAttributeElement() {
    return myMeta.getAttribute(getPsiElement(), FOR_CLASS_META);
  }

  @Nullable
  public PsiAnnotation getAnnotation() {
    return myMeta.getAnnotation(getPsiElement());
  }

  @Override
  public String getConverterId() {
    return getConverterIdAttributeElement().getValue();
  }

  @Override
  public PsiType getForClass() {
    final PsiClass psiClass = getForClassAttributeElement().getValue();

    return psiClass == null ? null : JavaPsiFacade.getElementFactory(psiClass.getProject()).createType(psiClass);
  }

  public PsiTarget getPsiTarget() {
    final JamStringAttributeElement<String> namedAttributeValue = getConverterIdAttributeElement();
    if (StringUtil.isEmptyOrSpaces(namedAttributeValue.getStringValue())) {
      return getAliasingPsiTarget();
    }

    return new JamPomTarget(this, namedAttributeValue);
  }

  private PsiTarget getAliasingPsiTarget() {
    return new DecapitalizedAliasingPsiTarget(getPsiElement());
  }

  @Override
  public PsiElement getIdentifyingElement() {
    final JamStringAttributeElement<String> namedAttributeValue = getConverterIdAttributeElement();
    if (StringUtil.isEmptyOrSpaces(namedAttributeValue.getStringValue())) {
      return getPsiElement().getNameIdentifier();
    }
    return PomService.convertToPsi(new JamPomTarget(this, namedAttributeValue));
  }

  public PsiAnnotation getIdentifyingAnnotation() {
    return myMeta.getAnnotation(getPsiElement());
  }
}
