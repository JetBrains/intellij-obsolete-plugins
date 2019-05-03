/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.tree;

import com.intellij.javaee.model.xml.web.WebApp;
import com.intellij.openapi.project.Project;
import com.intellij.struts.StrutsProjectComponent;
import com.intellij.struts.ValidationModel;
import com.intellij.struts.dom.StrutsConfig;
import com.intellij.struts.dom.validator.*;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericDomValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Setup DOM-Tree for Validator config files.
 *
 * @author Dmitry Avdeev
 */
public class ValidatorDomTree extends StrutsTreeBase<FormValidation, ValidationModel> {

  private final static Map<Class, Boolean> hiders = new HashMap<>();

  private final static List<Class> consolidated = Arrays.asList(
    new Class[]{Validator.class, Global.class, Constant.class, Formset.class, Form.class, Field.class, Msg.class, Arg.class, Var.class}

  );

  static {
    ValidatorDomTree.hiders.put(DomElement.class, true);
    ValidatorDomTree.hiders.put(GenericDomValue.class, false);
  }

  public ValidatorDomTree(final Project project) {
    super(project, StrutsProjectComponent.getInstance(project).getValidatorFactory(), hiders, consolidated, null,
          Arrays.asList(FormValidation.class, StrutsConfig.class, WebApp.class));
  }

}