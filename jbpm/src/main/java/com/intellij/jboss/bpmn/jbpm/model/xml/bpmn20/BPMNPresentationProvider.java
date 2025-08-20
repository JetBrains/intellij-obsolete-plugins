package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.ide.presentation.PresentationProvider;
import com.intellij.openapi.util.text.StringUtil;

public class BPMNPresentationProvider extends PresentationProvider<TBaseElement> {
  @Override
  public String getName(TBaseElement element) {
    if (element instanceof TFlowElement) {
      final String value = ((TFlowElement)element).getName().getStringValue();
      if (!StringUtil.isEmptyOrSpaces(value)) {
        return value;
      }
    }
    return element.getId().getStringValue();
  }

  @Override
  public String getTypeName(TBaseElement element) {
    return element.getXmlElementName();
  }
}
