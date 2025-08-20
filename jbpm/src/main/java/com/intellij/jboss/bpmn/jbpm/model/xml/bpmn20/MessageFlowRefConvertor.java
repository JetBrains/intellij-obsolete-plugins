package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.converters.TBaseElementConverter;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Irina.Chernushina
 * <p>
 * todo: completion: suggest according to rule "source abd target - from different pools"
 */
public class MessageFlowRefConvertor extends TBaseElementConverter {
  /*Of the types of InteractionNode, only Pools/Participants, Activities, and
  Events can be the source/target of a Message Flow.*/
  private final static Set<String> ourClasses = new HashSet<>();

  static {
    ourClasses.add("participant");

    ourClasses.add("callActivity");
    ourClasses.add("subProcess");
    ourClasses.add("adHocSubProcess");
    ourClasses.add("transaction");

    ourClasses.add("task");
    ourClasses.add("businessRuleTask");
    ourClasses.add("manualTask");
    ourClasses.add("receiveTask");
    ourClasses.add("scriptTask");
    ourClasses.add("sendTask");
    ourClasses.add("serviceTask");
    ourClasses.add("userTask");

    ourClasses.add("boundaryEvent");
    ourClasses.add("intermediateCatchEvent");
    ourClasses.add("startEvent");
    ourClasses.add("endEvent");
    ourClasses.add("implicitThrowEvent");
    ourClasses.add("intermediateThrowEvent");
  }

  @Override
  protected Set<String> possiblyReferencedTypes() {
    return ourClasses;
  }
}
