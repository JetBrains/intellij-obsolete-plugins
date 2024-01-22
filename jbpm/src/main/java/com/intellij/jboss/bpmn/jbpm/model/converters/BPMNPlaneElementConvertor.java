package com.intellij.jboss.bpmn.jbpm.model.converters;

import java.util.HashSet;
import java.util.Set;

public class BPMNPlaneElementConvertor extends TBaseElementConverter {
  /*a reference to either a Process, SubProcess, AdHocSubProcess, Transaction, Collaboration, Choreography or
  SubChoreography in a BPMN model.*/
  private final static Set<String> ourClasses = new HashSet<>();

  static {
    ourClasses.add("process");

    ourClasses.add("subProcess");
    ourClasses.add("adHocSubProcess");
    ourClasses.add("transaction");

    ourClasses.add("collaboration");
    ourClasses.add("choreography");
    ourClasses.add("subChoreography");
  }

  @Override
  protected Set<String> possiblyReferencedTypes() {
    return ourClasses;
  }
}
