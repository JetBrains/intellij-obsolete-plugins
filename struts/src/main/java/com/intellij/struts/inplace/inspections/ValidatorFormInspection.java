/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.inspections;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts.StrutsBundle;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.ValidationModel;
import com.intellij.struts.dom.Action;
import com.intellij.struts.dom.FormBean;
import com.intellij.struts.dom.validator.Form;
import com.intellij.struts.dom.validator.FormValidation;
import com.intellij.struts.dom.validator.Formset;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.highlighting.DomElementAnnotationHolder;
import com.intellij.util.xml.highlighting.DomElementsInspection;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dmitry Avdeev
 */
public class ValidatorFormInspection extends DomElementsInspection<FormValidation> {

  public ValidatorFormInspection() {
    super(FormValidation.class);
  }

  @NotNull
  @Override
  public String[] getGroupPath() {
    return new String[]{"Struts", getGroupDisplayName()};
  }

  @Override
  @NonNls
  @NotNull
  public String getShortName() {
    return "StrutsValidatorFormInspection";
  }

  @Override
  public void checkFileElement(final DomFileElement<FormValidation> fileElement,
                               final DomElementAnnotationHolder holder) {

    final XmlFile xmlFile = fileElement.getFile();
    final ValidationModel validationModel = StrutsManager.getInstance().getValidation(xmlFile);
    if (validationModel == null) {
      return;
    }
    final StrutsModel strutsModel = validationModel.getStrutsModel();

    final FormValidation formValidation = fileElement.getRootElement();

    final Project project = xmlFile.getProject();
    final PsiManager psiManager = PsiManager.getInstance(project);
    final GlobalSearchScope scope = GlobalSearchScope.allScope(project);

    final PsiClass validatorForm =
      JavaPsiFacade.getInstance(psiManager.getProject()).findClass("org.apache.struts.validator.ValidatorForm", scope);
    final PsiClass dynaValidatorForm =
      JavaPsiFacade.getInstance(psiManager.getProject()).findClass("org.apache.struts.validator.DynaValidatorForm", scope);

    final PsiClass validatorActionForm =
      JavaPsiFacade.getInstance(psiManager.getProject()).findClass("org.apache.struts.validator.ValidatorActionForm", scope);
    final PsiClass dynaValidatorActionForm =
      JavaPsiFacade.getInstance(psiManager.getProject()).findClass("org.apache.struts.validator.DynaValidatorActionForm", scope);

    final PsiClass beanValidatorForm =
      JavaPsiFacade.getInstance(psiManager.getProject()).findClass("org.apache.struts.validator.BeanValidatorForm", scope);

    for (final Formset formset : formValidation.getFormsets()) {
      for (final Form form : formset.getForms()) {
        final String formName = form.getName().getStringValue();
        if (StringUtil.isNotEmpty(formName)) {
          final GenericAttributeValue<String> element = form.getName();
          if (formName.startsWith("/")) {
            final Action action = strutsModel.findAction(formName);
            if (action != null) {
              checkAction(action, element, holder, validatorActionForm, dynaValidatorActionForm);
            }
          }
          final FormBean bean = strutsModel.findFormBean(formName);
          if (bean != null) {
            checkFormBean(bean, element, holder, validatorForm, dynaValidatorForm);
          } else {
            final Action action = strutsModel.findAction("/" + formName);
            if (action != null) {
              checkAction(action, element, holder, beanValidatorForm);
            }
          }
        }
      }
    }
  }

  private static void checkFormBean(final FormBean formBean,
                                    final GenericAttributeValue<String> formName,
                                    final DomElementAnnotationHolder holder,
                                    final PsiClass... requiredClass) {
    final PsiClass psiClass = formBean.getType().getValue();
    if (psiClass == null) {
      holder.createProblem(formName, StrutsBundle.message("inspections.validator.form.not.valid.type"));
    } else {
      PsiClass wanted = null;
      for (final PsiClass required : requiredClass) {
        if (required != null) {
          if (!InheritanceUtil.isInheritorOrSelf(psiClass, required, true)) {
            wanted = required;
          } else {
            return;
          }
        }
      }
      if (wanted != null) {
        holder.createProblem(formName, StrutsBundle.message("inspections.validator.form.not", wanted.getName()));
      }
    }
  }

  private static void checkAction(final Action action,
                                  final GenericAttributeValue<String> formName,
                                  final DomElementAnnotationHolder holder,
                                  final PsiClass... requiredClass) {
    final FormBean formBean = action.getName().getValue();
    if (formBean == null) {
      holder.createProblem(formName, StrutsBundle.message("inspections.validator.form.no.form.bean"));
    } else {
      checkFormBean(formBean, formName, holder, requiredClass);
    }
  }
}
