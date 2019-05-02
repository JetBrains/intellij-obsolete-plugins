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
