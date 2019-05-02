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

import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.dom.FormBean;
import com.intellij.struts.inplace.reference.XmlAttributeReferenceProvider;
import com.intellij.struts.inplace.reference.XmlValueReference;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.Nullable;

/**
 * Reference to FormBean declaration.
 */
public class FormReferenceProvider extends XmlAttributeReferenceProvider {

  public FormReferenceProvider() {
    super(FormBean.class);
  }

  @Override
  protected PsiReference[] create(final XmlAttributeValue attribute) {

    return new PsiReference[]{new XmlValueReference(attribute, this) {

      @Override
      public XmlTag doResolve() {
        final StrutsModel model = StrutsManager.getInstance().getStrutsModel(myValue);
        if (model == null) {
          return null;
        }

        final FormBean bean = model.findFormBean(getValue());
        return bean == null ? null : bean.getName().getXmlTag();
      }

      @Override
      public Object[] doGetVariants() {
        final StrutsModel model = StrutsManager.getInstance().getStrutsModel(myValue);
        return model == null ? null : getItems(model.getFormBeans());
      }

      @Override
      @Nullable
      protected DomElement getScope() {
        final StrutsModel model = StrutsManager.getInstance().getStrutsModel(myValue);
        if (model == null) {
          return null;
        }
        return model.getMergedModel().getFormBeans();
      }
    }};
  }

}