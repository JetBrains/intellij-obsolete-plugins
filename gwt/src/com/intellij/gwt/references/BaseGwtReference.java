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

package com.intellij.gwt.references;

import com.intellij.gwt.codeInsight.GwtReferenceUtil;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.sdk.GwtVersion;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class BaseGwtReference<T extends PsiElement> extends PsiReferenceBase<T> {
  protected final GwtModulesManager myGwtModulesManager;

  protected BaseGwtReference(T element, boolean soft) {
    super(element, soft);
    myGwtModulesManager = GwtModulesManager.getInstance(myElement.getProject());
  }

  protected BaseGwtReference(T element) {
    this(element, false);
  }

  protected XmlFile @NotNull [] getHtmlFilesForModule() {
    final GwtModule module = findGwtModule();
    if (module == null) return XmlFile.EMPTY_ARRAY;

    return myGwtModulesManager.findHtmlFilesByModule(module);

  }

  protected static @Nullable GwtVersion getGwtVersion(@NotNull GwtModule gwtModule) {
    final GwtFacet facet = GwtFacet.getInstance(gwtModule);
    return facet != null ? facet.getSdkVersion() : null;
  }

  public @Nullable GwtModule findGwtModule() {
    return GwtReferenceUtil.findGwtModule(myElement, myGwtModulesManager);
  }

}
