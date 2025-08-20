package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:mailRecipientType interface.
 */
public interface MailRecipient extends JpdlDomElement {

  /**
   * Returns the value of the addresses child.
   * <pre>
   * <h3>Attribute null:addresses documentation</h3>
   * list of email address separated by ',' (comma) ';' (semicolon) '|' or whitespace
   * </pre>
   *
   * @return the value of the addresses child.
   */
  @NotNull
  GenericAttributeValue<String> getAddresses();


  /**
   * Returns the value of the users child.
   * <pre>
   * <h3>Attribute null:users documentation</h3>
   * list of user ids that are resolved to the email address against configured identity component.
   *       user ids should be separated by ',' (comma) ';' (semicolon) '|' or whitespace
   * </pre>
   *
   * @return the value of the users child.
   */
  @NotNull
  GenericAttributeValue<String> getUsers();


  /**
   * Returns the value of the groups child.
   * <pre>
   * <h3>Attribute null:groups documentation</h3>
   * list of group ids that are resolved to the email address against configured identity component.
   *       group ids should be separated by ',' (comma) ';' (semicolon) '|' or whitespace
   * </pre>
   *
   * @return the value of the groups child.
   */
  @NotNull
  GenericAttributeValue<String> getGroups();
}
