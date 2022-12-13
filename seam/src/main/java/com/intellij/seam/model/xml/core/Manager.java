package com.intellij.seam.model.xml.core;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Namespace;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/core:managerElemType interface.
 */
@Namespace(SeamNamespaceConstants.CORE_NAMESPACE_KEY)
public interface Manager extends BasicSeamComponent {

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
   * Returns the value of the concurrent-request-timeout child.
   *
   * @return the value of the concurrent-request-timeout child.
   */
  @NotNull
  GenericAttributeValue<Integer> getConcurrentRequestTimeout();

  /**
   * Returns the value of the conversation-timeout child.
   *
   * @return the value of the conversation-timeout child.
   */
  @NotNull
  GenericAttributeValue<Integer> getConversationTimeout();

  /**
   * Returns the value of the conversation-id-parameter child.
   *
   * @return the value of the conversation-id-parameter child.
   */
  @NotNull
  GenericAttributeValue<String> getConversationIdParameter();

  /**
   * Returns the value of the parent-conversation-id-parameter child.
   *
   * @return the value of the parent-conversation-id-parameter child.
   */
  @NotNull
  GenericAttributeValue<String> getParentConversationIdParameter();
}
