package com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup;

import com.intellij.jboss.bpmn.jpdl.model.xml.*;
import com.intellij.jboss.bpmn.jpdl.model.xml.converters.JavaActivityMethodConverter;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:javaType interface.
 */
public interface JavaActivity extends TransitionOwner, JpdlNamedActivity, Graphical, OnOwner {

  /**
   * The name of the method to invoke.
   *
   * @return the value of the method child.
   */
  @NotNull
  @Required
  @Convert(value = JavaActivityMethodConverter.class, soft = true)
  GenericAttributeValue<PsiMethod> getMethod();

  /**
   * The jndi name of the ejb that needs to be invoked.
   *
   * @return the value of the ejb-jndi-name child.
   */
  @NotNull
  GenericAttributeValue<String> getEjbJndiName();

  @NotNull
  @Attribute("class")
  GenericAttributeValue<PsiClass> getClazz();

  @NotNull
  List<GenericDomValue<String>> getDescriptions();

  @NotNull
  List<GenericDomValue<String>> getFields();

  @NotNull
  List<GenericDomValue<String>> getArgs();

  @NotNull
  GenericAttributeValue<Continue> getContinue();


  @NotNull
  GenericAttributeValue<String> getExpr();

  @NotNull
  GenericAttributeValue<String> getLang();

  @NotNull
  GenericAttributeValue<String> getVar();
}
