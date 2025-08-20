package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:mailType interface.
 */
public interface Mail extends TransitionOwner, JpdlNamedActivity, Graphical, OnOwner {

  /**
   * Returns the value of the template child.
   *
   * @return the value of the template child.
   */
  @NotNull
  @Attribute("template")
  GenericAttributeValue<String> getTemplateAttr();


  /**
   * Returns the list of description children.
   *
   * @return the list of description children.
   */
  @NotNull
  List<GenericDomValue<String>> getDescriptions();

  /**
   * Adds new child to the list of description children.
   *
   * @return created child
   */
  GenericDomValue<String> addDescription();


  /**
   * Returns the value of the from child.
   *
   * @return the value of the from child.
   */
  @NotNull
  MailRecipient getFrom();


  /**
   * Returns the value of the to child.
   *
   * @return the value of the to child.
   */
  @NotNull
  MailRecipient getTo();


  /**
   * Returns the value of the cc child.
   *
   * @return the value of the cc child.
   */
  @NotNull
  MailRecipient getCc();


  /**
   * Returns the value of the bcc child.
   *
   * @return the value of the bcc child.
   */
  @NotNull
  MailRecipient getBcc();


  /**
   * Returns the value of the subject child.
   *
   * @return the value of the subject child.
   */
  @NotNull
  GenericDomValue<String> getSubject();


  /**
   * Returns the value of the text child.
   *
   * @return the value of the text child.
   */
  @NotNull
  GenericDomValue<String> getText();


  /**
   * Returns the value of the html child.
   *
   * @return the value of the html child.
   */
  @NotNull
  GenericDomValue<String> getHtml();


  /**
   * Returns the value of the attachments child.
   *
   * @return the value of the attachments child.
   */
  @NotNull
  Attachments getAttachments();


  /**
   * Returns the value of the continue child.
   * <pre>
   * <h3>Attribute null:continue documentation</h3>
   * To specify async continuations.
   *       sync is the default.
   * </pre>
   *
   * @return the value of the continue child.
   */
  @NotNull
  GenericAttributeValue<Continue> getContinue();


  /**
   * Returns the value of the template child.
   *
   * @return the value of the template child.
   */
  @NotNull
  GenericAttributeValue<String> getTemplate();
}
