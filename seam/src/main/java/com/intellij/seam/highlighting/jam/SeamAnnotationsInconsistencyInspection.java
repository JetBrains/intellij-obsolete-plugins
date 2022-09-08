package com.intellij.seam.highlighting.jam;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.javaee.ejb.model.EnterpriseBean;
import com.intellij.javaee.ejb.model.EntityBean;
import com.intellij.javaee.ejb.model.SessionBean;
import com.intellij.javaee.ejb.role.EjbClassRole;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.seam.constants.SeamAnnotationConstants;
import com.intellij.seam.constants.SeamAnnotationsUtil;
import com.intellij.seam.constants.SeamNonComponentAnnotations;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.xml.SeamDomModelManager;
import com.intellij.seam.model.xml.components.SeamComponents;
import com.intellij.seam.model.xml.components.SeamDomComponent;
import com.intellij.seam.resources.SeamInspectionBundle;
import com.intellij.seam.utils.SeamCommonUtils;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class SeamAnnotationsInconsistencyInspection extends SeamJamModelInspectionBase {

  private enum SeamUnsupportedAnnotations {
    @NonNls STATELESS("Stateless Session Bean", SeamAnnotationConstants.CREATE_ANNOTATION, SeamAnnotationConstants.DESTROY_ANNOTATION,
                      SeamAnnotationConstants.J2EE_TRANSACTIONAL_ANNOTATION, SeamNonComponentAnnotations.DATA_BINDER_CLASS_ANNOTATION,
                      SeamNonComponentAnnotations.DATA_SELECTOR_CLASS_ANNOTATION),

    @NonNls STATEFUL("Stateful Session Bean", SeamAnnotationConstants.J2EE_TRANSACTIONAL_ANNOTATION,
                     SeamNonComponentAnnotations.DATA_BINDER_CLASS_ANNOTATION, SeamNonComponentAnnotations.DATA_SELECTOR_CLASS_ANNOTATION),

    @NonNls ENTITY("Entity Bean", SeamAnnotationConstants.JNDI_NAME_ANNOTATION, SeamAnnotationConstants.IN_ANNOTATION,
                   SeamAnnotationConstants.OUT_ANNOTATION, SeamAnnotationConstants.UNWRAP_ANNOTATION,
                   SeamAnnotationConstants.FACTORY_ANNOTATION, SeamAnnotationConstants.LOGGER_ANNOTATION,
                   SeamAnnotationConstants.REQUEST_PARAMETER_ANNOTATION_2_0, SeamAnnotationConstants.CREATE_ANNOTATION,
                   SeamAnnotationConstants.DESTROY_ANNOTATION, SeamAnnotationConstants.BEGIN_TASK_ANNOTATION,
                   SeamAnnotationConstants.BEGIN_ANNOTATION, SeamAnnotationConstants.END_ANNOTATION,
                   SeamAnnotationConstants.START_TASK_ANNOTATION, SeamAnnotationConstants.RESUME_PROCESS_ANNOTATION,
                   SeamAnnotationConstants.CREATE_PROCESS_ANNOTATION, SeamAnnotationConstants.J2EE_TRANSACTIONAL_ANNOTATION,
                   SeamAnnotationConstants.JSF_DATA_MODEL_ANNOTATION, SeamAnnotationConstants.JSF_DATA_MODEL_SELECTION_ANNOTATION,
                   SeamAnnotationConstants.JSF_DATA_MODEL_SELECTION_INDEX_ANNOTATION,
                   SeamNonComponentAnnotations.DATA_BINDER_CLASS_ANNOTATION, SeamNonComponentAnnotations.DATA_SELECTOR_CLASS_ANNOTATION),
    @NonNls JAVA_BEAN("Java Bean", SeamAnnotationConstants.JNDI_NAME_ANNOTATION),
    @NonNls SEAM_COMPONENT("Seam Component", SeamNonComponentAnnotations.AROUND, SeamNonComponentAnnotations.DATA_BINDER_CLASS_ANNOTATION,
                           SeamNonComponentAnnotations.DATA_SELECTOR_CLASS_ANNOTATION, SeamNonComponentAnnotations.WITHIN);

    private final String myName;
    private final List<String> myUnsupportedAnnotations;

    SeamUnsupportedAnnotations(String name, String... annotations) {
      myName = name;
      myUnsupportedAnnotations = Arrays.asList(annotations);
    }

    public String getFullName() {
      return myName;
    }

    public boolean isUnsupported(String anno) {
      for (String s : myUnsupportedAnnotations) {
        if (s.equals(anno)) return true;
      }
      return false;
    }

    public String getName() {
      return myName;
    }
  }

  @Override
  protected void checkClass(final PsiClass aClass, final ProblemsHolder holder) {
    SeamJamComponent jamComponent = SeamCommonUtils.getSeamJamComponent(aClass);
    if (jamComponent != null) {
      checkSeamJamComponent(jamComponent, holder);
    } else {
      if (!SeamCommonUtils.isAbstractSeamComponent(aClass) && !isDefinedInXml(aClass)) {
        checkClassContainsIllegalSeamAnnotations(aClass, holder);
      }
    }
  }

  private static boolean isDefinedInXml(final PsiClass aClass) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(aClass);
    if (module != null) {
      final PsiClassType type = JavaPsiFacade.getInstance(module.getProject()).getElementFactory().createType(aClass);
      for (SeamComponents seamDomModel : SeamDomModelManager.getInstance(aClass.getProject()).getAllModels(module)) {
        for (SeamDomComponent domComponent : DomUtil.getDefinedChildrenOfType(seamDomModel, SeamDomComponent.class)) {
          if (type.equals(domComponent.getComponentType())) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  protected void checkSeamJamComponent(final SeamJamComponent seamClassRole, final ProblemsHolder holder) {
    checkBeans(seamClassRole, holder);
  }

  private static void checkBeans(final SeamJamComponent seamClassRole, final ProblemsHolder holder) {
    final EjbClassRole[] ejbRoles = SeamCommonUtils.getEjbRoles(seamClassRole);
    for (EjbClassRole ejbClassRole : ejbRoles) {
      EnterpriseBean bean = ejbClassRole.getEnterpriseBean();
      if (bean instanceof SessionBean) {
        if (SeamCommonUtils.isStateful((SessionBean)bean)) {
          checkInconsistency(seamClassRole, holder, SeamUnsupportedAnnotations.STATEFUL);
        }
        else {
          checkInconsistency(seamClassRole, holder, SeamUnsupportedAnnotations.STATELESS);
        }
      }
      else if (bean instanceof EntityBean) {
        checkInconsistency(seamClassRole, holder, SeamUnsupportedAnnotations.ENTITY);
      }
    }

    if (ejbRoles.length == 0) {
      checkInconsistency(seamClassRole, holder, SeamUnsupportedAnnotations.JAVA_BEAN);
    }

    checkInconsistency(seamClassRole, holder, SeamUnsupportedAnnotations.SEAM_COMPONENT);
  }

  private static void checkInconsistency(final SeamJamComponent seamComponent,
                                         final ProblemsHolder holder,
                                         final SeamUnsupportedAnnotations incorrectAnnotations) {

    final PsiClass psiClass = seamComponent.getPsiElement();

    checkAnnotation(psiClass, incorrectAnnotations, holder);

    for (PsiMethod psiMethod : psiClass.getMethods()) {
      checkAnnotation(psiMethod, incorrectAnnotations, holder);
    }

    for (PsiField psiField : psiClass.getFields()) {
      checkAnnotation(psiField, incorrectAnnotations, holder);
    }
  }

  private static void checkAnnotation(final PsiMember psiMember,
                                      final SeamUnsupportedAnnotations incorrectAnnotations,
                                      final ProblemsHolder holder) {
    PsiModifierList modifierList = psiMember.getModifierList();
    if (modifierList != null) {
      PsiAnnotation[] annotations = modifierList.getAnnotations();
      for (PsiAnnotation annotation : annotations) {
        final String qualifiedName = annotation.getQualifiedName();
        if (incorrectAnnotations.isUnsupported(qualifiedName)) {
          holder.registerProblem(annotation,
                                 SeamInspectionBundle.message("jam.annotation.not.allowed", StringUtil.getShortName(qualifiedName),
                                                              incorrectAnnotations.getFullName()));
        }
      }
    }
  }

  private static void checkClassContainsIllegalSeamAnnotations(final PsiClass psiClass, final ProblemsHolder holder) {
    for (String seamAnnotation : SeamAnnotationsUtil.getAllSeamAnnotations()) {
      checkAnnotation(psiClass, seamAnnotation, holder);
      for (PsiMethod psiMethod : psiClass.getMethods()) {
        checkAnnotation(psiMethod, seamAnnotation, holder);
      }

      for (PsiField field : psiClass.getFields()) {
        checkAnnotation(field, seamAnnotation, holder);
      }

    }
  }

  private static void checkAnnotation(final PsiMember psiMember, final String incorrectAnnotation, final ProblemsHolder holder) {
    PsiModifierList modifierList = psiMember.getModifierList();
    if (modifierList != null) {
      PsiAnnotation[] annotations = modifierList.getAnnotations();
      for (PsiAnnotation annotation : annotations) {
        final String qualifiedName = annotation.getQualifiedName();
        if (incorrectAnnotation.equals(qualifiedName)) {
          holder.registerProblem(annotation, SeamInspectionBundle.message("jam.name.annotation.must.be.defined.for.class",
                                                                          StringUtil.getShortName(qualifiedName)));
        }
      }
    }
  }
}