package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tSubConversation interface.
 */
public interface TSubConversation extends Bpmn20DomElement, TConversationNode {

  @NotNull
  @SubTagList("conversationNode")
  List<TConversationNode> getConversationNodes();
}
