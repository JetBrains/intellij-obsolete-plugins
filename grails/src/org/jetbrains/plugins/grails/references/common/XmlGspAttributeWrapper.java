// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.common;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspAttribute;

public class XmlGspAttributeWrapper implements GspAttributeWrapper {

  private final GspAttribute myGspAttribute;

  public XmlGspAttributeWrapper(GspAttribute gspAttribute) {
    myGspAttribute = gspAttribute;
  }

  @Override
  public @NotNull GspTagWrapper getTag() {
    return new XmlGspTagWrapper(myGspAttribute.getParent());
  }

  @Override
  public @NotNull String getName() {
    return myGspAttribute.getName();
  }

  @Override
  public PsiElement getValue() {
    return myGspAttribute.getValueElement();
  }
}
