package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.psi.PsiClass;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.ExtendClass;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:event-listenerElemType interface.
 */
public interface EventListener extends JpdlDomElement, WireObject {
  @NotNull
  @Attribute("class")
  @ExtendClass("org.jbpm.api.listener.EventListener")
  GenericAttributeValue<PsiClass> getClazz();

  @NotNull
  GenericAttributeValue<BooleanValue> getPropagation();
}
