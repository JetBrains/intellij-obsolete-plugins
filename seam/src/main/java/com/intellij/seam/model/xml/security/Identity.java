package com.intellij.seam.model.xml.security;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Namespace;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/security:identityElemType interface.
 */
@Namespace(SeamNamespaceConstants.SECURITY_NAMESPACE_KEY)
public interface Identity extends BasicSeamComponent {

  @NotNull
  @Required
  String getValue();

  void setValue(@NotNull String value);

  @NotNull
  GenericAttributeValue<String> getAuthenticateMethod();

  @NotNull
  GenericAttributeValue<Boolean> getRememberMe();

  @NotNull
  GenericAttributeValue<Boolean> getAuthenticateEveryRequest();

  @NotNull
  GenericAttributeValue<String> getJaasConfigName();

  @NotNull
  GenericAttributeValue<String> getSecurityRules();

  @NotNull
  GenericAttributeValue<String> getCookieMaxAge();
}
