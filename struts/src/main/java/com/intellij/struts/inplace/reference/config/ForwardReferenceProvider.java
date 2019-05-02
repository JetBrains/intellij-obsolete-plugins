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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.dom.Forward;
import com.intellij.struts.inplace.reference.XmlAttributeReferenceProvider;
import com.intellij.struts.inplace.reference.XmlValueReference;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.Nullable;

/**
 * Reference to global forward declaration.
 */
public class ForwardReferenceProvider extends XmlAttributeReferenceProvider {

  public ForwardReferenceProvider(boolean soft) {
    super(Forward.class);
    setSoft(soft);
  }

  @Override
  protected PsiReference[] create(final XmlAttributeValue attribute) {

    final PsiReference psiReference = new XmlValueReference(attribute, this) {

      @Override
      public PsiElement doResolve() {
        final StrutsModel model = StrutsManager.getInstance().getStrutsModel(myValue);
        if (model == null) {
          return null;
        }
        final String forwardName = myValue.getValue();
        final Forward forward = model.findForward(forwardName);
        if (forward != null) {
          return forward.getName().getXmlTag();
        }
        return null;
      }

      @Override
      @Nullable
      protected DomElement getScope() {
        final StrutsModel model = StrutsManager.getInstance().getStrutsModel(myValue);
        if (model == null) {
          return null;
        }
        return model.getMergedModel().getGlobalForwards();
      }

      @Override
      public Object[] doGetVariants() {
        final StrutsModel model = StrutsManager.getInstance().getStrutsModel(myValue);
        if (model == null) {
          return null;
        }

        return getItems(model.getGlobalForwards());
      }

    };

    return new PsiReference[]{psiReference};
  }

}