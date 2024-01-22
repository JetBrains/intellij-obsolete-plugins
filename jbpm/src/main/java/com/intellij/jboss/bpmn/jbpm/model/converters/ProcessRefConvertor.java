package com.intellij.jboss.bpmn.jbpm.model.converters;

import java.util.Collections;
import java.util.Set;

public class ProcessRefConvertor extends TBaseElementConverter {
  @Override
  protected Set<String> possiblyReferencedTypes() {
    return Collections.singletonMap("process", "").keySet();
  }
}
