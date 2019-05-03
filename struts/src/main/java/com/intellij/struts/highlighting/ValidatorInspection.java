/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.highlighting;

import com.intellij.struts.dom.validator.FormValidation;
import com.intellij.util.xml.highlighting.BasicDomElementsInspection;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class ValidatorInspection extends BasicDomElementsInspection<FormValidation> {

  public ValidatorInspection() {
    super(FormValidation.class);
  }

  @Override
  @NotNull
  @NonNls
  public String getShortName() {
    return "StrutsValidatorInspection";
  }
}