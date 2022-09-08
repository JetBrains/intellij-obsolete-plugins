package com.intellij.seam.model.jam;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.ide.presentation.Presentation;
import com.intellij.jam.JamPomTarget;
import com.intellij.jam.JamService;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.jam.reflect.*;
import com.intellij.javaee.ejb.model.EnterpriseBean;
import com.intellij.javaee.ejb.model.SessionBean;
import com.intellij.javaee.ejb.role.EjbClassRole;
import com.intellij.javaee.model.annotations.AnnotationModelUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.references.PomService;
import com.intellij.psi.*;
import com.intellij.psi.targets.DecapitalizedAliasingPsiTarget;
import com.intellij.seam.constants.SeamAnnotationConstants;
import com.intellij.seam.model.SeamComponentScope;
import com.intellij.seam.model.jam.bijection.SeamJamInjection;
import com.intellij.seam.model.jam.bijection.SeamJamOutjection;
import com.intellij.seam.model.jam.context.SeamJamBegin;
import com.intellij.seam.model.jam.context.SeamJamEnd;
import com.intellij.seam.model.jam.context.SeamJamObserver;
import com.intellij.seam.model.jam.context.SeamJamRaiseEvent;
import com.intellij.seam.model.jam.dataModel.SeamJamDataModel;
import com.intellij.seam.model.jam.dataModel.SeamJamDataModelSelection;
import com.intellij.seam.model.jam.dataModel.SeamJamDataModelSelectionIndex;
import com.intellij.seam.model.jam.jsf.SeamJamConverter;
import com.intellij.seam.model.jam.jsf.SeamJamValidator;
import com.intellij.seam.model.jam.lifecycle.SeamJamCreate;
import com.intellij.seam.model.jam.lifecycle.SeamJamDestroy;
import com.intellij.seam.utils.SeamCommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Presentation(typeName = SeamJamComponent.SEAM_COMPONENT)
public class SeamJamComponent extends SeamJamComponentBase<PsiClass> {

  public static final JamClassMeta<SeamJamComponent> META = new JamClassMeta<>(SeamJamComponent.class)
    .addPomTargetProducer((seamJamComponent, consumer) -> consumer.consume(seamJamComponent.getPsiTarget()));

  public static final JamStringAttributeMeta.Single<String> NAME_VALUE_META = JamAttributeMeta.singleString("value");

  private static final JamAnnotationMeta ANNOTATION_META =
    new JamAnnotationMeta(SeamAnnotationConstants.COMPONENT_ANNOTATION).addAttribute(NAME_VALUE_META);

  // context
  private static final JamChildrenQuery<SeamJamBegin> BEGINS_QUERY =
    JamChildrenQuery.annotatedMethods(SeamJamBegin.ANNOTATION_META, SeamJamBegin.class);

  private static final JamChildrenQuery<SeamJamEnd> ENDS_QUERY =
    JamChildrenQuery.annotatedMethods(SeamJamEnd.ANNOTATION_META, SeamJamEnd.class);

  private static final JamChildrenQuery<SeamJamObserver> OBSERVERS_QUERY =
    JamChildrenQuery.annotatedMethods(SeamJamObserver.ANNOTATION_META, SeamJamObserver.class);

  private static final JamChildrenQuery<SeamJamRaiseEvent> RAISE_EVENTS_QUERY =
    JamChildrenQuery.annotatedMethods(SeamJamRaiseEvent.ANNOTATION_META, SeamJamRaiseEvent.class);

  // lifecycle
  private static final JamChildrenQuery<SeamJamCreate> CREATES_QUERY =
    JamChildrenQuery.annotatedMethods(SeamJamCreate.ANNOTATION_META, SeamJamCreate.class);
  private static final JamChildrenQuery<SeamJamDestroy> DESTROY_QUERY =
    JamChildrenQuery.annotatedMethods(SeamJamDestroy.ANNOTATION_META, SeamJamDestroy.class);

  // bijection
  private static final JamChildrenQuery<SeamJamInjection.Field> IN_FIELD_QUERY =
    JamChildrenQuery.annotatedFields(SeamJamInjection.ANNOTATION_META, SeamJamInjection.Field.class);
  private static final JamChildrenQuery<SeamJamInjection.Method> IN_METHOD_QUERY =
    JamChildrenQuery.annotatedMethods(SeamJamInjection.ANNOTATION_META, SeamJamInjection.Method.class);

  private static final JamChildrenQuery<SeamJamOutjection.Field> OUT_FIELD_QUERY =
    JamChildrenQuery.annotatedFields(SeamJamOutjection.ANNOTATION_META, SeamJamOutjection.Field.class);
  private static final JamChildrenQuery<SeamJamOutjection.Method> OUT_METHOD_QUERY =
    JamChildrenQuery.annotatedMethods(SeamJamOutjection.ANNOTATION_META, SeamJamOutjection.Method.class);

  // dataModel
  private static final JamChildrenQuery<SeamJamDataModel.Method> DM_METHOD_QUERY =
    JamChildrenQuery.annotatedMethods(SeamJamDataModel.ANNOTATION_META, SeamJamDataModel.Method.META);
  private static final JamChildrenQuery<SeamJamDataModel.Field> DM_FIELD_QUERY =
    JamChildrenQuery.annotatedFields(SeamJamDataModel.ANNOTATION_META, SeamJamDataModel.Field.META);

  private static final JamChildrenQuery<SeamJamDataModelSelection.Method> DM_SELECTION_METHOD_QUERY =
    JamChildrenQuery.annotatedMethods(SeamJamDataModelSelection.ANNOTATION_META, SeamJamDataModelSelection.Method.class);
  private static final JamChildrenQuery<SeamJamDataModelSelection.Field> DM_SELECTION_FIELD_QUERY =
    JamChildrenQuery.annotatedFields(SeamJamDataModelSelection.ANNOTATION_META, SeamJamDataModelSelection.Field.class);

  private static final JamChildrenQuery<SeamJamDataModelSelectionIndex.Method> DM_SELECTION_INDEX_METHOD_QUERY =
    JamChildrenQuery.annotatedMethods(SeamJamDataModelSelectionIndex.ANNOTATION_META, SeamJamDataModelSelectionIndex.Method.class);
  private static final JamChildrenQuery<SeamJamDataModelSelectionIndex.Field> DM_SELECTION_INDEX_FIELD_QUERY =
    JamChildrenQuery.annotatedFields(SeamJamDataModelSelectionIndex.ANNOTATION_META, SeamJamDataModelSelectionIndex.Field.class);

  private static final JamChildrenQuery<SeamJamUnwrap> UNWRAP_QUERY =
    JamChildrenQuery.annotatedMethods(SeamJamUnwrap.ANNOTATION_META, SeamJamUnwrap.class);

  private static final JamChildrenQuery<SeamJamFactory> FACTORY_QUERY =
    JamChildrenQuery.annotatedMethods(SeamJamFactory.ANNOTATION_META, SeamJamFactory.META);
  public static final String SEAM_COMPONENT = "Seam Component";

  static {
    META.addAnnotation(ANNOTATION_META);

    META.addChildrenQuery(BEGINS_QUERY);
    META.addChildrenQuery(ENDS_QUERY);
    META.addChildrenQuery(OBSERVERS_QUERY);
    META.addChildrenQuery(RAISE_EVENTS_QUERY);

    META.addChildrenQuery(CREATES_QUERY);
    META.addChildrenQuery(DESTROY_QUERY);

    META.addChildrenQuery(IN_FIELD_QUERY);
    META.addChildrenQuery(IN_METHOD_QUERY);
    META.addChildrenQuery(OUT_FIELD_QUERY);
    META.addChildrenQuery(OUT_METHOD_QUERY);

    META.addChildrenQuery(DM_FIELD_QUERY);
    META.addChildrenQuery(DM_METHOD_QUERY);
    META.addChildrenQuery(DM_SELECTION_FIELD_QUERY);
    META.addChildrenQuery(DM_SELECTION_METHOD_QUERY);
    META.addChildrenQuery(DM_SELECTION_INDEX_FIELD_QUERY);
    META.addChildrenQuery(DM_SELECTION_INDEX_METHOD_QUERY);

    META.addChildrenQuery(UNWRAP_QUERY);
    META.addChildrenQuery(FACTORY_QUERY);
  }

  public SeamJamComponent(PsiElementRef<?> ref) {
    super(ref);
  }

  @Override
  @NotNull
  public String getComponentName() {
    String nameValue = getNamedAttributeValue();

    return StringUtil.isEmptyOrSpaces(nameValue) ? getDefaultName() : nameValue;
  }

  @Nullable
  private String getNamedAttributeValue() {
    JamStringAttributeElement<String> namedStringAttributeElement = getNamedStringAttributeElement();

    if (namedStringAttributeElement == null) return null;

    return namedStringAttributeElement.getStringValue();
  }

  @Nullable
  protected JamStringAttributeElement<String> getNamedStringAttributeElement() {
    return ANNOTATION_META.getAttribute(getPsiElement(), NAME_VALUE_META);
  }

  @NotNull
  public String getDefaultName() {
    return StringUtil.decapitalize(StringUtil.notNullize(getPsiElement().getName()));
  }

  @Override
  public PsiElement getIdentifyingPsiElement() {
    return PomService.convertToPsi(getProject(), getPsiTarget());
  }

  private Project getProject() {
    return getPsiElement().getProject();
  }

  public PsiTarget getPsiTarget() {
    final JamStringAttributeElement<String> namedAttributeValue = getNamedStringAttributeElement();
    if (namedAttributeValue == null || StringUtil.isEmptyOrSpaces(namedAttributeValue.getStringValue())) {
      return getAliasingPsiTarget();
    }

    return new JamPomTarget(this, namedAttributeValue);
  }

  private PsiTarget getAliasingPsiTarget() {
    return new DecapitalizedAliasingPsiTarget(getPsiElement());
  }

  @NotNull
  public List<SeamJamFactory> getFactories() {
    return FACTORY_QUERY.findChildren(getPsiRef());
  }

  @NotNull
  public List<SeamJamRole> getRoles() {
    return JamCommonUtil.getElementsIncludingSingle(getPsiElement(), SeamJamRoles.ANNOTATION_META, SeamJamRoles.ROLES_COLLECTION_ATTRIBUTE);
  }

  @Nullable
  public SeamJamValidator getValidator() {
    return JamService.getJamService(getProject()).getJamElement(getPsiElement(), SeamJamValidator.META);
  }

  @Nullable
  public SeamJamConverter getConverter() {
    return JamService.getJamService(getProject()).getJamElement(getPsiElement(), SeamJamConverter.META);
  }

  @NotNull
  public List<SeamJamInjection> getInjections() {
    List<SeamJamInjection> injections = new ArrayList<>();

    injections.addAll(IN_FIELD_QUERY.findChildren(getPsiRef()));
    injections.addAll(IN_METHOD_QUERY.findChildren(getPsiRef()));

    return injections;
  }

  @NotNull
  public List<SeamJamOutjection> getOutjections() {
    List<SeamJamOutjection> outjections = new ArrayList<>();

    outjections.addAll(OUT_FIELD_QUERY.findChildren(getPsiRef()));
    outjections.addAll(OUT_METHOD_QUERY.findChildren(getPsiRef()));

    return outjections;
  }

  @NotNull
  public List<? extends SeamJamUnwrap> getUnwraps() {
    return UNWRAP_QUERY.findChildren(getPsiRef());
  }

  @NotNull
  public List<SeamJamCreate> getCreates() {
    return CREATES_QUERY.findChildren(getPsiRef());
  }

  @NotNull
  public List<SeamJamDestroy> getDestroys() {
    return DESTROY_QUERY.findChildren(getPsiRef());
  }

  @NotNull
  public List<SeamJamBegin> getBegins() {
    return BEGINS_QUERY.findChildren(getPsiRef());
  }

  private PsiElementRef<PsiClass> getPsiRef() {
    return PsiElementRef.real(getPsiElement());
  }

  @NotNull
  public List<SeamJamEnd> getEnds() {
    return ENDS_QUERY.findChildren(getPsiRef());
  }

  @NotNull
  public List<? extends SeamJamObserver> getObservers() {
    return OBSERVERS_QUERY.findChildren(getPsiRef());
  }

  @NotNull
  public List<? extends SeamJamRaiseEvent> getRaiseEvents() {
    return RAISE_EVENTS_QUERY.findChildren(getPsiRef());
  }

  @NotNull
  public List<? extends SeamJamDataModel> getDataModels() {
    List<SeamJamDataModel> dataModels = new ArrayList<>();

    dataModels.addAll(DM_FIELD_QUERY.findChildren(getPsiRef()));
    dataModels.addAll(DM_METHOD_QUERY.findChildren(getPsiRef()));

    return dataModels;
  }

  @NotNull
  public List<SeamJamDataModelSelection> getDataModelSelections() {
    List<SeamJamDataModelSelection> list = new ArrayList<>();

    list.addAll(DM_SELECTION_FIELD_QUERY.findChildren(getPsiRef()));
    list.addAll(DM_SELECTION_METHOD_QUERY.findChildren(getPsiRef()));

    return list;
  }

  @NotNull
  public List<SeamJamDataModelSelectionIndex> getDataModelSelectionIndexes() {
    List<SeamJamDataModelSelectionIndex> list = new ArrayList<>();

    list.addAll(DM_SELECTION_INDEX_FIELD_QUERY.findChildren(getPsiRef()));
    list.addAll(DM_SELECTION_INDEX_METHOD_QUERY.findChildren(getPsiRef()));

    return list;
  }

  @Nullable
  public SeamJamInstall getInstall() {
    return JamService.getJamService(getProject()).getJamElement(getPsiElement(), SeamJamInstall.META);
  }

  @Nullable
  public PsiAnnotation getIdentifyingAnnotation() {
    return ANNOTATION_META.getAnnotation(getPsiElement());
  }

  @Override
  public SeamComponentScope getComponentScope() {
    PsiAnnotation scopeAnnotation = AnnotationUtil.findAnnotation(getPsiElement(), SeamAnnotationConstants.SCOPE_ANNOTATION);

    final SeamComponentScope defaultScope = getDefaultScope();
    if (scopeAnnotation == null) return defaultScope;

    SeamComponentScope value = AnnotationModelUtil.getEnumValue(scopeAnnotation, "value", SeamComponentScope.class).getValue();

    return value == null ? defaultScope : value;
  }

  private SeamComponentScope getDefaultScope() {
    for (EjbClassRole role : SeamCommonUtils.getEjbRoles(this)) {
      final EnterpriseBean bean = role.getEnterpriseBean();
      if (bean instanceof SessionBean && !SeamCommonUtils.isStateful((SessionBean)bean)) {
        return SeamComponentScope.STATELESS;
      }
    }

    return SeamComponentScope.CONVERSATION;
  }

  @Override
  public PsiType getComponentType() {
    PsiClass psiClass = getPsiElement();

    final PsiType unwrapType = SeamCommonUtils.getUnwrapType(psiClass);

    return unwrapType == null ? JavaPsiFacade.getInstance(psiClass.getProject()).getElementFactory().createType(psiClass) : unwrapType;
  }
}
