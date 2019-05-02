/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.struts.inplace.reference.config;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.ValidationModel;
import com.intellij.struts.dom.Action;
import com.intellij.struts.dom.FormBean;
import com.intellij.struts.inplace.reference.XmlAttributeReferenceProvider;
import com.intellij.struts.inplace.reference.XmlValueReference;
import com.intellij.util.xml.DomElement;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Dmitry Avdeev
 */
public class ValidatorFormReferenceProvider extends XmlAttributeReferenceProvider {

  public ValidatorFormReferenceProvider() {
    super("symbol");
  }

  @Override
  protected PsiReference[] create(XmlAttributeValue attribute) {

    return new PsiReference[]{new XmlValueReference(attribute, this) {

      @Override
      public XmlTag doResolve() {

        final ValidationModel validation = StrutsManager.getInstance().getValidation(myValue);
        if (validation == null) {
          return null;
        }
        StrutsModel model = validation.getStrutsModel();
        final String value = getValue();
        FormBean bean;
        if (StringUtil.startsWithChar(value, '/')) {
          final Action action = model.findAction(value);
          return action == null ? null : action.getXmlTag();
        }
        else {
          bean = model.findFormBean(value);
          if (bean == null) {
            // last try for BeanValidatorForm ...
            final Action action = model.findAction("/" + value);
            return action == null ? null : action.getXmlTag();
          }
          else {
            return bean.getXmlTag();
          }
        }
      }

      @Override
      public Object[] doGetVariants() {
        final ValidationModel validation = StrutsManager.getInstance().getValidation(myValue);
        if (validation == null) {
          return null;
        }
        StrutsModel model = validation.getStrutsModel();
        final PsiManager psiManager = PsiManager.getInstance(getProject());
        final GlobalSearchScope scope = GlobalSearchScope.allScope(getProject());
        final PsiClass validatorActionForm =
          JavaPsiFacade.getInstance(psiManager.getProject()).findClass("org.apache.struts.validator.ValidatorActionForm", scope);
        final PsiClass beanValidatorForm =
          JavaPsiFacade.getInstance(psiManager.getProject()).findClass("org.apache.struts.validator.BeanValidatorForm", scope);
        final HashSet<FormBean> pathValidated = new HashSet<>();
        final ArrayList<DomElement> list = new ArrayList<>();
        for (FormBean form : model.getFormBeans()) {
          final PsiClass psiClass = form.getType().getValue();
          if (psiClass != null) {
            if (InheritanceUtil.isInheritorOrSelf(psiClass, validatorActionForm, true)) {
              pathValidated.add(form);
              if (InheritanceUtil.isInheritorOrSelf(psiClass, beanValidatorForm, true)) {
                list.add(form);
              }
            }
            else {
              list.add(form);
            }
          }
        }
        for (Action action : model.getActions()) {
          final FormBean bean = action.getName().getValue();
          if (bean != null && pathValidated.contains(bean)) {
            list.add(action);
          }
        }
        return getItems(list);
      }

    }};
  }
}
