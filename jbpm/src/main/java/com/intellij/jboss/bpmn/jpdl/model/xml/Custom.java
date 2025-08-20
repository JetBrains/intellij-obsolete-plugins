package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.psi.PsiClass;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:customElemType interface.
 */
public interface Custom extends TransitionOwner, JpdlNamedActivity, Graphical, OnOwner, WireObject {

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

  @NotNull
  @Attribute("class")
  GenericAttributeValue<PsiClass> getClazz();
}
