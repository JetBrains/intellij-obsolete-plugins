package com.intellij.seam.highlighting.jam;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.javaee.ejb.model.EnterpriseBean;
import com.intellij.javaee.ejb.model.SessionBean;
import com.intellij.javaee.ejb.model.xml.impl.SessionBeanImpl;
import com.intellij.javaee.ejb.role.EjbClassRole;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.seam.facet.SeamFacet;
import com.intellij.seam.model.jam.SeamJamComponent;
import com.intellij.seam.model.jam.jsf.SeamJamConverter;
import com.intellij.seam.model.jam.jsf.SeamJamValidator;
import com.intellij.seam.model.jam.lifecycle.SeamJamCreate;
import com.intellij.seam.model.jam.lifecycle.SeamJamDestroy;
import com.intellij.seam.resources.SeamBundle;
import com.intellij.seam.resources.SeamInspectionBundle;
import com.intellij.seam.utils.SeamCommonUtils;
import com.intellij.util.xml.XmlDomBundle;

import java.util.List;

import static com.intellij.javaee.ejb.model.EjbAnnotationConstants.REMOVE_ANNO;

public class SeamJamComponentInspection extends SeamJamModelInspectionBase {
  private static final String JSF_VALIDATOR_CLASSNAME = "javax.faces.validator.Validator";
  private static final String JSF_CONVERTEWR_CLASSNAME = "javax.faces.convert.Converter";

  @Override
  protected void checkSeamJamComponent(final SeamJamComponent seamJamComponent, final ProblemsHolder holder) {
    checkEmptyName(holder, seamJamComponent);
    checkDublicatesAnnotations(holder, seamJamComponent);

    checkRemoveAnnotation(holder, seamJamComponent);

    checkValidatorAnnotation(holder, seamJamComponent);
    checkConverterAnnotation(holder, seamJamComponent);
  }

  private SeamFacet getFacet(SeamJamComponent seamJamComponent) {
    Module module = seamJamComponent.getModule();
    return SeamFacet.getInstance(module);
  }

  private static void checkEmptyName(final ProblemsHolder holder, final SeamJamComponent seamJamComponent) {
    final String s = seamJamComponent.getComponentName();
    if (StringUtil.isEmptyOrSpaces(s)) {
      final PsiAnnotation annotation = seamJamComponent.getIdentifyingAnnotation();
      if (annotation != null) { // if component is defined in components.xml without @Name annoation
        holder.registerProblem(annotation, XmlDomBundle.message("dom.inspections.value.must.not.be.empty"));
      }
    }
  }


  private static void checkDublicatesAnnotations(final ProblemsHolder holder, final SeamJamComponent seamJamComponent) {
    List<? extends SeamJamCreate> jamCreates = seamJamComponent.getCreates();
    if (jamCreates.size() > 1) {
      for (SeamJamCreate jamCreate : jamCreates) {
        holder.registerProblem(jamCreate.getIdentifyingAnnotation(),
                               SeamInspectionBundle.message("jam.component.duplicated.annotation", "@Create"));
      }
    }

    List<? extends SeamJamDestroy> destroys = seamJamComponent.getDestroys();
    if (destroys.size() > 1) {
      for (SeamJamDestroy destroy : destroys) {
        holder.registerProblem(destroy.getIdentifyingAnnotation(),
                               SeamInspectionBundle.message("jam.component.duplicated.annotation", "@Destroy"));
      }
    }
  }

  private static void checkConverterAnnotation(final ProblemsHolder holder, final SeamJamComponent jamComponent) {
    final SeamJamConverter seamJamConverter = jamComponent.getConverter();
    if (seamJamConverter != null) {
      final PsiClass psiClass = jamComponent.getPsiElement();

      if (!isAssignable(psiClass, JSF_CONVERTEWR_CLASSNAME)) {
        holder.registerProblem(seamJamConverter.getIdentifyingAnnotation(), SeamBundle.message("converter.must.be.implemented"));
      }
    }
  }

  private static void checkValidatorAnnotation(final ProblemsHolder holder, final SeamJamComponent jamComponent) {
    final SeamJamValidator seamJamValidator = jamComponent.getValidator();
    if (seamJamValidator != null) {
      final PsiClass psiClass = jamComponent.getPsiElement();

      if (!isAssignable(psiClass, JSF_VALIDATOR_CLASSNAME)) {
        holder.registerProblem(seamJamValidator.getIdentifyingAnnotation(), SeamBundle.message("validator.must.be.implemented"));
      }
    }
  }

  private static boolean isAssignable(final PsiClass aClass, final String className) {
    final Project project = aClass.getProject();
    final PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project));

    return psiClass != null && InheritanceUtil.isInheritorOrSelf(aClass, psiClass, true);
  }

  private static void checkRemoveAnnotation(final ProblemsHolder holder, final SeamJamComponent seamJamComponent) {
    // http://docs.jboss.com/seam/2.0.0.GA/reference/en/html/concepts.html#d0e3063
    // stateful session bean components must define a method with no parameters annotated @Remove. This method is called by Seam when the context ends.
    for (EjbClassRole ejbRole : SeamCommonUtils.getEjbRoles(seamJamComponent)) {
      EnterpriseBean bean = ejbRole.getEnterpriseBean();
      if (bean instanceof SessionBean && SeamCommonUtils.isStateful((SessionBean)bean) && !hasRemove((SessionBean)bean)) {
        PsiAnnotation annotation = seamJamComponent.getIdentifyingAnnotation();
        if (annotation != null) {
          holder.registerProblem(annotation, SeamInspectionBundle.message("jam.component.session.stateful.must.have.remove",
                                                                          bean.getEjbName().getStringValue()));
        }
      }
    }
  }

  private static boolean hasRemove(final SessionBean bean) {
    if (bean instanceof SessionBeanImpl) {
      SessionBeanImpl domSessionBean = (SessionBeanImpl)bean;
      return domSessionBean.getRemoveMethods().size() > 0;
    }

    if (bean instanceof com.intellij.javaee.ejb.model.annotations.ejb.SessionBeanImpl) {
      com.intellij.javaee.ejb.model.annotations.ejb.SessionBeanImpl sessionBean =
        (com.intellij.javaee.ejb.model.annotations.ejb.SessionBeanImpl)bean;

      PsiClass member = sessionBean.getPsiClass();
      if (member != null) {
        for (PsiMethod method : member.getMethods()) {
          if (AnnotationUtil.findAnnotation(method, REMOVE_ANNO.javax()) != null) return true;
          if (AnnotationUtil.findAnnotation(method, REMOVE_ANNO.jakarta()) != null) return true;
        }
      }
    }

    return false;
  }
}
