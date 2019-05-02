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
