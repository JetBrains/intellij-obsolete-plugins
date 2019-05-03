/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts;

import com.intellij.javaee.web.WebUtil;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.ScopeProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class SecurityRoleScopeProvider extends ScopeProvider {

  /**
   * Returns the {@link com.intellij.javaee.model.xml.web.WebApp} for the given DomElement.
   *
   * @param element DomElement.
   * @return null if DomElement does not belong to WebModule.
   */
  @Override
  @Nullable
  public DomElement getScope(@NotNull DomElement element) {
    WebFacet webFacet = WebUtil.getWebFacet(element.getXmlTag());
    if (webFacet != null) {
      return webFacet.getRoot();
    }
    return null;
  }

}