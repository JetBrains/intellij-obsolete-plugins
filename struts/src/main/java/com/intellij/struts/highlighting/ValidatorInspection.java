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