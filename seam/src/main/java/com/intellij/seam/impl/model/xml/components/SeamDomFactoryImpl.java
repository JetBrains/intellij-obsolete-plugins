package com.intellij.seam.impl.model.xml.components;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.seam.model.CommonSeamFactoryComponent;
import com.intellij.seam.model.SeamComponentScope;
import com.intellij.seam.model.xml.components.SeamDomFactory;
import com.intellij.seam.utils.SeamCommonUtils;
import org.jetbrains.annotations.Nullable;

public abstract class SeamDomFactoryImpl implements CommonSeamFactoryComponent, SeamDomFactory {
  @Override
  @Nullable
  public String getFactoryName() {
    return getName().getValue();
  }

  @Override
  @Nullable
  public PsiType getFactoryType() {
    return SeamCommonUtils.getFactoryType(this, getModule());
  }

  @Override
  @Nullable
  public SeamComponentScope getFactoryScope() {
    return getScope().getValue();
  }

  @Override
  public PsiElement getIdentifyingPsiElement() {
    return getXmlElement();
  }
}
