package com.intellij.seam.highlighting.jam;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.jam.model.util.JamCommonUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.jam.SeamJamFactory;
import com.intellij.seam.model.jam.SeamJamUnwrap;
import com.intellij.seam.model.jam.bijection.SeamJamInjection;
import com.intellij.seam.model.jam.bijection.SeamJamOutjection;
import com.intellij.seam.model.jam.dataModel.SeamJamDataModel;
import com.intellij.seam.model.jam.dataModel.SeamJamDataModelSelection;
import com.intellij.seam.model.jam.dataModel.SeamJamDataModelSelectionIndex;
import com.intellij.seam.model.jam.lifecycle.SeamJamCreate;
import com.intellij.seam.model.jam.lifecycle.SeamJamDestroy;
import com.intellij.seam.resources.SeamInspectionBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class SeamAnnotationIncorrectSignatureInspection extends SeamJamModelInspectionBase {
  @NonNls private static final String ORG_JBOSS_SEAM_COMPONENT_CLASS = "org.jboss.seam.Component";

  @Override
  protected void checkSeamJamComponent(final SeamJamComponent seamJamComponent, final ProblemsHolder holder) {
    checkClassModifiers(holder, seamJamComponent);
    checkChildrenSignatures(holder, seamJamComponent);
  }

  private static void checkChildrenSignatures(final ProblemsHolder holder, final SeamJamComponent seamJamComponent) {
    checkInjectionSignature(seamJamComponent.getInjections(), holder);
    checkOutjectionSignature(seamJamComponent.getOutjections(), holder);

    checkDataModelSignature(seamJamComponent.getDataModels(), holder);
    checkDataModelSelectionSignature(seamJamComponent.getDataModelSelections(), holder);
    checkDataModelSelectionIndexSignature(seamJamComponent.getDataModelSelectionIndexes(), holder);

    checkFactorySignature(seamJamComponent.getFactories(), holder);
    checkUnwrapSignature(seamJamComponent.getUnwraps(), holder);
    checkCreateSignature(seamJamComponent.getCreates(), holder);
    checkDestroySignature(seamJamComponent.getDestroys(), holder);
  }

  private static void checkClassModifiers(final ProblemsHolder holder, final SeamJamComponent jamComponent) {
    PsiClass psiClass = jamComponent.getPsiElement();
    if (psiClass != null) {
      if (psiClass.isInterface()) {
        holder.registerProblem(jamComponent.getIdentifyingAnnotation(),
                               SeamInspectionBundle.message("jam.component.name.annotation.on.interface"));
      }
      else {
        if (isAbstract(psiClass)) {
          holder.registerProblem(jamComponent.getIdentifyingAnnotation(),
                                 SeamInspectionBundle.message("jam.component.name.annotation.on.abstract.class"));
        }
        if (!hasEmptyConstructor(psiClass)) {
          holder.registerProblem(jamComponent.getIdentifyingAnnotation(),
                                 SeamInspectionBundle.message("jam.component.name.annotation.no.empty.constructor"));
        }
      }
    }
  }

  private static void checkInjectionSignature(final List<SeamJamInjection> injections, final ProblemsHolder holder) {
    for (SeamJamInjection<?> injection : injections) {
      PsiMember member = injection.getPsiElement();
      if (member instanceof PsiMethod) {
        checkSetterMethodSignature(holder, injection.getIdentifyingAnnotation(), (PsiMethod)member);
      }
    }
  }

  private static void checkOutjectionSignature(final List<SeamJamOutjection> outjections, final ProblemsHolder holder) {
    for (SeamJamOutjection outjection : outjections) {
      PsiElement element = outjection.getPsiElement();

      if (element instanceof PsiMethod) {
        checkGetterMethodSignature(holder, outjection.getIdentifyingAnnotation(), (PsiMethod)element);
      }
    }
  }

  private static void checkDataModelSignature(final List<? extends SeamJamDataModel> dataModels, final ProblemsHolder holder) {

    for (SeamJamDataModel dataModel : dataModels) {
      checkDataModelType(holder, dataModel);
      if (dataModel instanceof SeamJamDataModel.Method) {
        SeamJamDataModel.Method model = (SeamJamDataModel.Method)dataModel;
        checkGetterMethodSignature(holder, model.getIdentifyingAnnotation(), model.getPsiElement());
      }
    }
  }

  private static void checkDataModelSelectionSignature(final List<? extends SeamJamDataModelSelection> dataModelSelections,
                                                       final ProblemsHolder holder) {
    for (SeamJamDataModelSelection dataModelSelection : dataModelSelections) {
      if (dataModelSelection instanceof SeamJamDataModelSelection.Method) {
        SeamJamDataModelSelection.Method model = (SeamJamDataModelSelection.Method)dataModelSelection;
        checkSetterMethodSignature(holder, model.getIdentifyingAnnotation(), model.getPsiElement());
      }
    }
  }

  private static void checkDataModelSelectionIndexSignature(final List<? extends SeamJamDataModelSelectionIndex> dataModelSelectionIndexes,
                                                            final ProblemsHolder holder) {
    for (SeamJamDataModelSelectionIndex dataModelSelectionIndex : dataModelSelectionIndexes) {
      if (dataModelSelectionIndex instanceof SeamJamDataModelSelectionIndex.Method) {
        SeamJamDataModelSelectionIndex.Method model = (SeamJamDataModelSelectionIndex.Method)dataModelSelectionIndex;

        checkSetterMethodSignature(holder, model.getIdentifyingAnnotation(), model.getPsiElement());
      }
    }
  }


  private static void checkDataModelType(final ProblemsHolder holder, final SeamJamDataModel dataModel) {
    String[] dataModelTypes = new String[]{CommonClassNames.JAVA_UTIL_LIST, CommonClassNames.JAVA_UTIL_SET, CommonClassNames.JAVA_UTIL_MAP};
    PsiType type = dataModel.getType();
    if (type == null) return;

    if (type instanceof PsiArrayType) return;

    if (!isAssignableFromAny(dataModel.getPsiManager().getProject(), type, dataModelTypes)) {
      holder.registerProblem(dataModel.getIdentifyingAnnotation(),
                             SeamInspectionBundle.message("jam.component.data.model.incorrect.type"));
    }
  }

  private static void checkHasNoParameters(final ProblemsHolder holder,
                                           final PsiAnnotation annotation,
                                           final PsiMethod psiMethod) {
    PsiParameterList parameterList = psiMethod.getParameterList();
    int parameterCount = parameterList.getParametersCount();
    if (parameterCount > 0) {
      holder.registerProblem(annotation, SeamInspectionBundle.message("method.must.have.no.parameters"));
    }
  }

  private static void checkUnwrapSignature(final List<? extends SeamJamUnwrap> unwraps, final ProblemsHolder holder) {
    for (SeamJamUnwrap seamJamUnwrap : unwraps) {
      PsiMethod psiMethod = seamJamUnwrap.getPsiElement();

      checkMethodIsNotStatic(seamJamUnwrap.getIdentifyingAnnotation(), holder, psiMethod);

      checkMethodHasReturnType(seamJamUnwrap.getIdentifyingAnnotation(), holder, psiMethod);

      checkHasNoParametersOrSingleComponentParam(holder, seamJamUnwrap.getIdentifyingAnnotation(), psiMethod);
    }
  }

  private static void checkFactorySignature(final Collection<SeamJamFactory> factories, final ProblemsHolder holder) {
    for (SeamJamFactory factoryComponent : factories) {
      PsiMethod psiMethod = factoryComponent.getPsiElement();
      assert psiMethod != null;

      checkMethodIsPublic(factoryComponent.getIdentifyingAnnotation(), holder, psiMethod);
      checkHasNoParametersOrSingleComponentParam(holder, factoryComponent.getIdentifyingAnnotation(), psiMethod);


      if (!hasFactoryExplicitVariableName(factoryComponent, holder) && !isJavaStyleName(psiMethod)) {
        holder.registerProblem(factoryComponent.getIdentifyingAnnotation(),
                               SeamInspectionBundle.message("method.factory.signature.and.explicit.variable.declaration"));
      }
    }
  }

  private static boolean isJavaStyleName(final PsiMethod method) {
    if (method == null) return false;

    String[] prefixes = new String[]{"get", "is"};
    String methodName = method.getName();
    for (String prefix : prefixes) {
      if (methodName.startsWith(prefix) &&
          methodName.length() > prefix.length() &&
          Character.isUpperCase(methodName.charAt(prefix.length()))) {
        return true;
      }
    }

    return false;
  }

  private static boolean hasFactoryExplicitVariableName(final SeamJamFactory factoryComponent, final ProblemsHolder holder) {
    return getFactoryExplicitVariableName(factoryComponent) != null;
  }

  @Nullable
  private static String getFactoryExplicitVariableName(final SeamJamFactory factoryComponent) {
    final PsiAnnotation annotation = factoryComponent.getIdentifyingAnnotation();
    if (annotation != null) {
      return JamCommonUtil.getObjectValue(annotation.findDeclaredAttributeValue("value"), String.class);
    }
    return null;
  }

  private static void checkDestroySignature(final List<? extends SeamJamDestroy> destroys, final ProblemsHolder holder) {
    for (SeamJamDestroy jamDestroy : destroys) {
      PsiMethod psiMethod = jamDestroy.getPsiElement();

      checkMethodIsNotStatic(jamDestroy.getIdentifyingAnnotation(), holder, psiMethod);

      checkHasNoParameters(holder, jamDestroy.getIdentifyingAnnotation(), psiMethod);

      checkMethodHasVoidReturnType(jamDestroy.getIdentifyingAnnotation(), holder, psiMethod);
    }
  }

  private static void checkCreateSignature(final List<SeamJamCreate> creates, final ProblemsHolder holder) {
    for (SeamJamCreate seamJamCreate : creates) {
      PsiMethod psiMethod = seamJamCreate.getPsiElement();
      PsiAnnotation annotation = seamJamCreate.getIdentifyingAnnotation();

      checkMethodIsNotStatic(annotation, holder, psiMethod);
      checkHasNoParametersOrSingleComponentParam(holder, annotation, psiMethod);
    }
  }

  private static boolean isAssignableFromAny(Project project, PsiType type, String... clazzNames) {
    for (String clazzName : clazzNames) {
      if (isAssignableFrom(project, clazzName, type)) {
        return true;
      }
    }
    return false;
  }


  private static boolean isAssignableFrom(Project project, String clazzName, PsiType type) {
    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
    PsiClass clazz = psiFacade.findClass(clazzName, GlobalSearchScope.allScope(project));

    if (clazz == null) return false;

    PsiClassType classType = psiFacade.getElementFactory().createType(clazz);

    return classType.isAssignableFrom(type);
  }

  private static void checkHasNoParametersOrSingleComponentParam(final ProblemsHolder holder,
                                                                 final PsiAnnotation psiAnnotation,
                                                                 final PsiMethod psiMethod) {
    PsiParameterList parameterList = psiMethod.getParameterList();
    int parameterCount = parameterList.getParametersCount();
    if (parameterCount != 0 &&
        (parameterCount != 1 ||
         !isAssignableFrom(psiMethod.getProject(), ORG_JBOSS_SEAM_COMPONENT_CLASS, parameterList.getParameters()[0].getType()))) {
      holder.registerProblem(psiAnnotation, SeamInspectionBundle.message("method.must.have.no.parameters.or.component.type"));
    }
  }

  public static void checkGetterMethodSignature(final ProblemsHolder holder, final PsiAnnotation psiAnnotation, final PsiMethod method) {
    if (!PropertyUtilBase.isSimplePropertyGetter(method)) {
      holder.registerProblem(psiAnnotation, SeamInspectionBundle.message("method.is.not.getter"));
    }
    checkMethodIsNotStatic(psiAnnotation, holder, method);
  }

  public static void checkSetterMethodSignature(final ProblemsHolder holder, final PsiAnnotation psiAnnotation, final PsiMethod method) {
    if (!PropertyUtilBase.isSimplePropertySetter(method)) {
      holder.registerProblem(psiAnnotation, SeamInspectionBundle.message("method.is.not.setter"));
    }
    checkMethodIsNotStatic(psiAnnotation, holder, method);
  }

  public static void checkMethodHasReturnType(final PsiAnnotation psiAnnotation, final ProblemsHolder holder, final PsiMethod method) {
    PsiType returnType = method.getReturnType();
    if (returnType == null || PsiType.VOID.equals(returnType)) {
      holder.registerProblem(psiAnnotation, SeamInspectionBundle.message("method.must.have.return.type"));
    }
  }

  public static void checkMethodHasVoidReturnType(final PsiAnnotation psiAnnotation,
                                                  final ProblemsHolder holder,
                                                  final PsiMethod method) {
    PsiType returnType = method.getReturnType();
    if (returnType == null || !PsiType.VOID.equals(returnType)) {
      holder
        .registerProblem(psiAnnotation, SeamInspectionBundle.message("method.must.have.void.return.type"));
    }
  }

  public static void checkMethodIsPublic(final PsiElement psiElement, final ProblemsHolder holder, final PsiMethod method) {
    if (!method.hasModifierProperty(PsiModifier.PUBLIC)) {
      holder.registerProblem(psiElement, SeamInspectionBundle.message("method.must.be.public"));
    }
  }

  public static void checkMethodIsNotStatic(final PsiElement psiElement, final ProblemsHolder holder, final PsiMethod method) {
    if (method.hasModifierProperty(PsiModifier.STATIC)) {
      holder.registerProblem(psiElement, SeamInspectionBundle.message("method.static.modifier.is.not.allowed"));
    }
  }

  public static boolean hasEmptyConstructor(final PsiClass psiClass) {
    PsiMethod[] methods = psiClass.getConstructors();
    if (methods.length == 0) return true;
    for (PsiMethod method : methods) {
      if (method.getParameterList().getParametersCount() == 0) return true;
    }
    return false;
  }

  public static boolean isAbstract(final PsiClass psiClass) {
    PsiModifierList modifierList = psiClass.getModifierList();
    return modifierList != null && modifierList.hasModifierProperty(PsiModifier.ABSTRACT);
  }
}
