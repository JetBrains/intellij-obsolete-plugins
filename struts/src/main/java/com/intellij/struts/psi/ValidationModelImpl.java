/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.psi;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.struts.NamedModelImpl;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.ValidationModel;
import com.intellij.struts.dom.validator.FormValidation;
import com.intellij.struts.dom.validator.Global;
import com.intellij.struts.dom.validator.Validator;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Provides functionality for accessing DOM-Model of {@code validation.xml} files.
 */
public class ValidationModelImpl extends NamedModelImpl<FormValidation> implements ValidationModel {

  private final StrutsModel myStrutsModel;

  public ValidationModelImpl(@NotNull Set<XmlFile> configFiles, @NotNull DomFileElement<FormValidation> model, @NotNull StrutsModel strutsModel) {
    super(configFiles, model, strutsModel.getName());
    myStrutsModel = strutsModel;
  }

  @Override
  @NotNull
  public List<Validator> getValidators() {
    List<Global> globals = getMergedModel().getGlobals();
    List<Validator> result = new ArrayList<>();
    for (Global global : globals) {
      final List<Validator> validators = global.getValidators();
      final List<Validator> filteredValidators = ContainerUtil.findAll(validators,
                                                                       validator -> StringUtil.isNotEmpty(validator.getClassname().getStringValue()));

      result.addAll(filteredValidators);
    }
    return result;
  }

  @Override
  @Nullable
  public Validator findValidator(String name) {
    return DomUtil.findByName(getValidators(), name);
  }

  @Override
  @NotNull
  public StrutsModel getStrutsModel() {
    return myStrutsModel;
  }

}
