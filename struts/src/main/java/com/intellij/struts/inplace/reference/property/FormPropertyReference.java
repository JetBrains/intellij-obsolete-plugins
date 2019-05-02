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

package com.intellij.struts.inplace.reference.property;

import com.intellij.javaee.web.ServletMappingInfo;
import com.intellij.javaee.web.WebUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.core.PsiBeanProperty;
import com.intellij.struts.dom.Action;
import com.intellij.struts.dom.FormBean;
import com.intellij.struts.inplace.reference.BaseReferenceProvider;
import com.intellij.struts.inplace.reference.XmlReferenceUtil;
import com.intellij.struts.util.PsiClassUtil;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author davdeev
 */
class FormPropertyReference extends PropertyReference {

  private final boolean myInJsp;

  FormPropertyReference(PropertyReferenceSet set, int index, TextRange range, boolean inJsp, BaseReferenceProvider provider) {
    super(set, index, range, provider);
    myInJsp = inJsp;
  }

  @Override
  @NotNull
  protected PsiBeanProperty[] getPropertiesForTag(final boolean forVariants) {


    boolean[] soft = new boolean[1];
    FormBean form = getFormBean(soft);
    if (form != null) {
      StrutsModel model = getStrutsModel();
      if (model != null) {
        return model.getFormProperties(form);
      }
    }

    setSoft(soft[0]);
    return PsiBeanProperty.EMPTY_ARRAY;
  }

  @Override
  @Nullable
  protected DomElement getScope() {
    return getFormBean(new boolean[1] );
  }

  /**
   *
   * @param soft true if action name is parametrized (e.g. action="/login${param}")
   * @return form bean context
   */
  @Nullable
  protected FormBean getFormBean(boolean[] soft) {

    XmlTag tag = PsiTreeUtil.getParentOfType(myValue, XmlTag.class);
    if (tag == null) {
      return null;
    }

    XmlTag enclosingFormTag = myInJsp ? XmlReferenceUtil.findEnclosingTagByClass(tag, "form", "org.apache.struts.taglib.html.FormTag") : tag.getParentTag();
    if (enclosingFormTag == null) {
      return null;
    }

    StrutsModel model = getStrutsModel();
    if (model == null) {
      return null;
    }

    if (!myInJsp) {
      String formName = enclosingFormTag.getAttributeValue("name");
      if (formName != null) {
        FormBean formBean = findFormBean(model, formName);
        if (formBean != null) {
          return checkFormBean(formBean, soft);
        }
      }
      return null;
    }

    XmlAttribute actionAttr = enclosingFormTag.getAttribute("action", null);
    if (actionAttr != null && actionAttr.getValue() != null) {
      String path = actionAttr.getValue();
      int paramPos = WebUtil.getLastPosOfURL(path);
      int dynaPos = WebUtil.indexOfDynamicJSP(path);
      if (dynaPos != -1 && (paramPos == -1 || dynaPos < paramPos)) {
        soft[0] = true;
        return null;
      }
      String actionPath = WebUtil.trimURL(path);
      final ServletMappingInfo mappingInfo = model.getServletMappingInfo();
      String action = mappingInfo.stripMapping(actionPath);
      if (action == null) {
        action = actionPath;
      }
      Action a = model.findAction(action);
      if (a != null) {
        final FormBean formBean = a.getName().getValue();
        return checkFormBean(formBean, soft);
      }
    }

    return null;
  }

  @Nullable
  protected StrutsModel getStrutsModel() {
    return StrutsManager.getInstance().getStrutsModel(myValue);
  }

  @Nullable
  protected FormBean findFormBean(final StrutsModel model, final String name) {
    return model.findFormBean(name);
  }

  /**
   * Sets soft
   *
   * @param formBean
   * @param soft
   * @return
   */
  @Nullable
  protected static FormBean checkFormBean(@Nullable final FormBean formBean, final boolean[] soft) {
    if (formBean != null) {
      final PsiClass psiClass = formBean.getType().getValue();
      if (psiClass != null) {
        if (PsiClassUtil.isSuper(psiClass, "org.apache.commons.beanutils.DynaBean")) {
          soft[0] = true;
        }
      }
    }
    return formBean;
  }

}
