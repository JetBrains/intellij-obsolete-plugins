package com.intellij.seam.model.jam.dataModel;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.*;
import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jam.reflect.*;
import com.intellij.javaee.model.annotations.AnnotationModelUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.PomTarget;
import com.intellij.psi.*;
import com.intellij.psi.targets.AliasingPsiTarget;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.seam.constants.SeamAnnotationConstants;
import com.intellij.seam.model.SeamComponentScope;
import com.intellij.semantic.SemKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Presentation(typeName = SeamJamDataModel.SEAM_DATA_MODEL)
public abstract class SeamJamDataModel<T extends PsiMember & PsiNamedElement> extends JamCommonModelElement<T>
  implements JamElement, CommonModelElement {

  public static final JamAnnotationMeta ANNOTATION_META = new JamAnnotationMeta(SeamAnnotationConstants.JSF_DATA_MODEL_ANNOTATION);
  public static final String SEAM_DATA_MODEL = "Seam Data Model";

  public SeamJamDataModel(T member) {
    super(PsiElementRef.real(member));
  }

  public static final JamStringAttributeMeta.Single<String> NAME_META = JamAttributeMeta.singleString("value");

  public static class Method extends SeamJamDataModel<PsiMethod> {
    public static final SemKey<Method> JAM_KEY = JamService.JAM_ALIASING_ELEMENT_KEY.subKey("SeamJamMethod");

    public static final JamMemberMeta<PsiMethod, Method> META = new JamMemberMeta<PsiMethod, Method>(null, Method.class, JAM_KEY).
      addPomTargetProducer((dataModel, consumer) -> consumer.consume(dataModel.getPsiTarget()));

    static {
      META.addAnnotation(ANNOTATION_META);
    }

    public Method(PsiMethod member) {
      super(member);
    }

    @Override
    public PsiType getType() {
      return getPsiElement().getReturnType();
    }

    @Override
    public String getNameAlias(@NotNull String delegatePsiTargetName) {
      return PropertyUtilBase.getPropertyName(delegatePsiTargetName);
    }
  }

  public static class Field extends SeamJamDataModel<PsiField> {
    public static final JamMemberMeta<PsiField, Field> META = new JamFieldMeta<>(Field.class).addPomTargetProducer(
      (dataModel, consumer) -> consumer.consume(dataModel.getPsiTarget()));

    static {
      META.addAnnotation(ANNOTATION_META);
    }

    public Field(PsiField member) {
      super(member);
    }

    @Override
    public PsiType getType() {
      return getPsiElement().getType();
    }

    @Override
    public String getNameAlias(@NotNull String delegatePsiTargetName) {
      return delegatePsiTargetName;
    }
  }

  @Nullable
  public String getDefaultName() {
    return PropertyUtilBase.getPropertyName(getPsiElement());
  }

  @Nullable
  public abstract PsiType getType();

  @Nullable
  public String getName() {
    String nameValue = getNamedStringAttributeElement().getStringValue();

    return StringUtil.isEmptyOrSpaces(nameValue) ? getDefaultName() : nameValue;
  }

  @NotNull
  public JamStringAttributeElement<String> getNamedStringAttributeElement() {
    return ANNOTATION_META.getAttribute(getPsiElement(), NAME_META);
  }

  @Nullable
  public SeamComponentScope getScope() {
    return AnnotationModelUtil.getEnumValue(getIdentifyingAnnotation(), "scope", SeamComponentScope.class).getValue();
  }

  @Nullable
  public PsiAnnotation getIdentifyingAnnotation() {
    return ANNOTATION_META.getAnnotation(getPsiElement());
  }

  public PomTarget getPsiTarget() {
    final JamStringAttributeElement<String> namedAttributeValue = getNamedStringAttributeElement();
    if (StringUtil.isEmptyOrSpaces(namedAttributeValue.getStringValue())) {
      return getAliasingPsiTarget();
    }

    return new JamPomTarget(this, namedAttributeValue);
  }

  public PsiTarget getAliasingPsiTarget() {
    return new AliasingPsiTarget(getPsiElement()) {
      @Override
      public String getNameAlias(@NotNull String delegatePsiTargetName) {
        return SeamJamDataModel.this.getNameAlias(delegatePsiTargetName);
      }
    };
  }

  public abstract String getNameAlias(@NotNull String delegatePsiTargetName);
}
