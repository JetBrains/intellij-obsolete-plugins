package com.intellij.gwt.uiBinder;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.xml.impl.XmlAttributeDescriptorEx;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GwtUiPropertyAttributeDescriptor extends GwtXmlAttributeDescriptorBase implements XmlAttributeDescriptorEx{
  private final String myPropertyName;
  private final PsiMethod mySetter;

  public GwtUiPropertyAttributeDescriptor(PsiMethod setter, @Nullable PsiType type, String propertyName) {
    super(type, false);
    mySetter = setter;
    myPropertyName = propertyName;
  }

  @Override
  public String handleTargetRename(@NotNull @NonNls String newTargetName) {
    return PropertyUtilBase.getPropertyName(newTargetName);
  }

  @Override
  public PsiElement getDeclaration() {
    return mySetter;
  }

  @Override
  public String getName() {
    return myPropertyName;
  }
}
