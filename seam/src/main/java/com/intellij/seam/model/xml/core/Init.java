package com.intellij.seam.model.xml.core;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Namespace;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/core:initElemType interface.
 */
@Namespace(SeamNamespaceConstants.CORE_NAMESPACE_KEY)
public interface Init extends BasicSeamComponent {

  /**
   * Returns the value of the simple content.
   *
   * @return the value of the simple content.
   */
  @NotNull
  @Required
  String getValue();

  /**
   * Sets the value of the simple content.
   *
   * @param value the new value to set
   */
  void setValue(@NotNull String value);

  /**
   * Returns the value of the debug child.
   *
   * @return the value of the debug child.
   */
  @NotNull
  GenericAttributeValue<Debug> getDebug();

  /**
   * Returns the value of the jndi-pattern child.
   *
   * @return the value of the jndi-pattern child.
   */
  @NotNull
  GenericAttributeValue<String> getJndiPattern();

  /**
   * Returns the value of the transaction-management-enabled child.
   *
   * @return the value of the transaction-management-enabled child.
   */
  @NotNull
  GenericAttributeValue<TransactionManagementEnabled> getTransactionManagementEnabled();

  /**
   * Returns the value of the user-transaction-name child.
   *
   * @return the value of the user-transaction-name child.
   */
  @NotNull
  GenericAttributeValue<String> getUserTransactionName();
}
