package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:attachmentsElemType interface.
 */
public interface Attachments extends JpdlDomElement {

  /**
   * Returns the list of attachment children.
   *
   * @return the list of attachment children.
   */
  @NotNull
  @Required
  List<Attachment> getAttachments();

  /**
   * Adds new child to the list of attachment children.
   *
   * @return created child
   */
  Attachment addAttachment();
}
