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

package com.intellij.struts.inplace.reference.config;

import com.intellij.javaee.model.xml.SecurityRole;
import com.intellij.javaee.model.xml.web.WebApp;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.struts.inplace.reference.ListAttrReferenceProvider;
import com.intellij.struts.inplace.reference.XmlValueReference;
import com.intellij.util.xml.DomUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reference to security roles defined in web.xml
 */
public class RolesReferenceProvider extends ListAttrReferenceProvider {

  /**
   * CTOR.
   *
   * @param single true if only one security role is allowed
   */
  public RolesReferenceProvider(boolean single) {
    super(SecurityRole.class, single);
  }

  @Override
  @NotNull
  protected XmlValueReference create(final XmlAttributeValue attribute, final TextRange range) {

    return new XmlValueReference(attribute, this, range) {

      @Override
      protected PsiElement doResolve() {
        final String roleName = getValue();

        // role "*" cannot be mapped in web.xml
        if (roleName.equals("*")) {
          return getElement();
        }

        final WebApp webApp = getWebApp(getWebFacet());

        if (webApp == null) {
          return null;
        }

        final SecurityRole securityRole = DomUtil.findByName(webApp.getSecurityRoles(), roleName);
        return securityRole == null ? null : securityRole.getXmlTag();
      }

      @Override
      protected Object[] doGetVariants() {
        final WebApp webApp = getWebApp(getWebFacet());
        return webApp == null ? null : getItems(webApp.getSecurityRoles());
      }

    };
  }

  @Nullable
  private static WebApp getWebApp(@Nullable final WebFacet webFacet) {
    return webFacet == null ? null : webFacet.getRoot();
  }

}
