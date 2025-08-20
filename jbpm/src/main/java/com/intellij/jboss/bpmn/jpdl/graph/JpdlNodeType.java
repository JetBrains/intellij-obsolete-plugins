package com.intellij.jboss.bpmn.jpdl.graph;

public enum JpdlNodeType {
  START,
  DECISIION,
  TASK,
  STATE,
  END,
  END_CANCEL,
  END_ERROR,
  SUBPROCESS,
  FORK,
  JOIN,
  JAVA,
  CUSTOM,
  SQL,
  HQL,
  SCRIPT,
  RULES,
  RULES_DECISION,
  MAIL,
  GROUP, UNKNOWN
}
