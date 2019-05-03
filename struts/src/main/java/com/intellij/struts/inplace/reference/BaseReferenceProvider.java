/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference;

import com.intellij.ide.TypePresentationService;
import com.intellij.util.xml.DomElement;
import com.intellij.psi.PsiReferenceProvider;
import org.jetbrains.annotations.Nullable;

public abstract class BaseReferenceProvider extends PsiReferenceProvider {
  protected final String myCanonicalName;
  private final Class<? extends DomElement> myDomClass;
  private boolean mySoft = false;

  protected BaseReferenceProvider() {
    myCanonicalName = null;
    myDomClass = null;
  }

  protected BaseReferenceProvider(String canonicalName) {
    myCanonicalName = canonicalName;
    myDomClass = null;
  }

  protected BaseReferenceProvider(Class<? extends DomElement> domClass) {

    myCanonicalName = TypePresentationService.getService().getTypePresentableName(domClass);
    myDomClass = domClass;
  }

  public String getCanonicalName() {
    return myCanonicalName;
  }

  @Nullable
  public Class<? extends DomElement> getDomClass() {
    return myDomClass;
  }

  public void setSoft(boolean softFlag) {
    mySoft = softFlag;
  }

  public boolean isSoft() {
    return mySoft;
  }
}
