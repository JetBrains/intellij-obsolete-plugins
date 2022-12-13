package com.intellij.seam.model.jam;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.*;
import com.intellij.jam.reflect.*;
import com.intellij.javaee.model.annotations.AnnotationModelUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.references.PomService;
import com.intellij.psi.*;
import com.intellij.psi.targets.AliasingPsiTarget;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.seam.constants.SeamAnnotationConstants;
import com.intellij.seam.model.CommonSeamComponent;
import com.intellij.seam.model.CommonSeamFactoryComponent;
import com.intellij.seam.model.SeamComponentScope;
import com.intellij.semantic.SemKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Presentation(typeName = SeamJamFactory.SEAM_FACTORY)
public class SeamJamFactory extends JamCommonModelElement<PsiMethod> implements JamElement, CommonSeamComponent, CommonSeamFactoryComponent {

  public static final JamStringAttributeMeta.Single<String> NAME_VALUE_META = JamAttributeMeta.singleString("value");

  public static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(SeamAnnotationConstants.FACTORY_ANNOTATION).addAttribute(NAME_VALUE_META);

  public static final SemKey<SeamJamFactory> JAM_KEY = JamService.JAM_ALIASING_ELEMENT_KEY.subKey("SeamJamFactory");

  public static final JamMemberMeta<PsiMethod,SeamJamFactory> META = new JamMethodMeta<>(null, SeamJamFactory.class, JAM_KEY).
    addPomTargetProducer((seamJamFactory, consumer) -> consumer.consume(seamJamFactory.getPsiTarget())).addAnnotation(ANNOTATION_META);
  public static final String SEAM_FACTORY = "Seam Factory";

  public SeamJamFactory(PsiElementRef<?> ref) {
    super(ref);
  }

  protected JamStringAttributeElement<String> getNamedStringAttributeElement() {
    return ANNOTATION_META.getAttribute(getPsiElement(), NAME_VALUE_META);
  }

  public PsiTarget getPsiTarget() {
    final JamStringAttributeElement<String> namedAttributeValue = getNamedStringAttributeElement();
    if (StringUtil.isEmptyOrSpaces(namedAttributeValue.getStringValue())) {
      return getAliasingPsiTarget();
    }

    return new JamPomTarget(this, namedAttributeValue);
  }

  private PsiTarget getAliasingPsiTarget() {
    return new AliasingPsiTarget(getPsiElement()) {

      @Override
      public String getNameAlias(@NotNull String delegatePsiTargetName) {
        return PropertyUtilBase.getPropertyName(delegatePsiTargetName);
      }
    };
  }

  @Override
  public PsiElement getIdentifyingPsiElement() {
    return PomService.convertToPsi(getPsiElement().getProject(), getPsiTarget());
  }

  @Override
  @Nullable
  public String getFactoryName() {
    String nameValue = getNamedStringAttributeElement().getStringValue();

    return StringUtil.isEmptyOrSpaces(nameValue) ? PropertyUtilBase.getPropertyName(getPsiElement()) : nameValue;
  }

  public PsiAnnotation getIdentifyingAnnotation() {
    return ANNOTATION_META.getAnnotation(getPsiElement());
  }

  @Nullable
  private String getVoidMethodFactoryName() {
    PsiMethod method = getPsiElement();

    if (hasGetterName(method)) return PropertyUtilBase.getPropertyNameByGetter(method);

    return null;
  }

  @Override
  public @NotNull String getComponentName() {
    return "";
  }

  @Override
  public @Nullable SeamComponentScope getComponentScope() {
    return null;
  }

  @Override
  public @Nullable PsiType getComponentType() {
    return null;
  }

  private static boolean hasGetterName(@NotNull final PsiMethod method) {
    if (method.isConstructor()) return false;

    String methodName = method.getName();

    final String[] prefixes = {"get", "is"};
    for (String prefix : prefixes) {
      if (methodName.startsWith(prefix) && methodName.length() > prefix.length()) {
        if (Character.isLowerCase(methodName.charAt(prefix.length())) &&
            (methodName.length() == prefix.length() + 1 || Character.isLowerCase(methodName.charAt(prefix.length() + 1)))) {
          return false;
        }
      }
    }

    return true;
  }

  @Override
  @Nullable
  public SeamComponentScope getFactoryScope() {
    return AnnotationModelUtil.getEnumValue(getIdentifyingAnnotation(), "scope", SeamComponentScope.class).getValue();
  }

  @Override
  @Nullable
  public PsiType getFactoryType() {
    PsiMethod method = getPsiElement();

    return method.getReturnType();
  }
}
