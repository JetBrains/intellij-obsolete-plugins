package com.intellij.gwt.uiBinder;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;

public class GwtUiParameterAttributeDescriptor extends GwtXmlAttributeDescriptorBase {
  private final PsiParameter myParameter;

  public GwtUiParameterAttributeDescriptor(PsiParameter parameter, boolean required) {
    super(parameter.getType(), required);
    myParameter = parameter;
  }

  @Override
  public PsiElement getDeclaration() {
    return myParameter;
  }

  @Override
  public @NotNull String getName() {
    return myParameter.getName();
  }
}
