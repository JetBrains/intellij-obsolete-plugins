package com.intellij.seam.model.xml.components;

import com.intellij.psi.PsiClass;
import com.intellij.seam.model.SeamComponentScope;
import com.intellij.seam.model.converters.SeamAnnoReferenceConverter;
import com.intellij.seam.model.xml.SeamDomElement;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/components:componentElemType interface.
 */
public interface BasicSeamComponent extends SeamDomElement {

  /**
   * Returns the value of the name child.
   *
   * @return the value of the name child.
   */
  @NotNull
  @Required(value = false, nonEmpty = true)
  @NameValue(unique = true, referencable = false)
  @Referencing(value = SeamAnnoReferenceConverter.class, soft = true)
  GenericAttributeValue<String> getName();

  /**
   * Returns the value of the class child.
   *
   * @return the value of the class child.
   */
  @NotNull
  @Attribute("class")
  @Required(value = false, nonEmpty = true)
  @ExtendClass(allowInterface = false, instantiatable = false)
  GenericAttributeValue<PsiClass> getClazz();

  /**
   * Returns the value of the scope child.
   *
   * @return the value of the scope child.
   */
  @NotNull
  GenericAttributeValue<SeamComponentScope> getScope();

  /**
   * Returns the value of the precedence child.
   *
   * @return the value of the precedence child.
   */
  @NotNull
  GenericAttributeValue<Integer> getPrecedence();

  /**
   * Returns the value of the installed child.
   *
   * @return the value of the installed child.
   */
  @NotNull
  GenericAttributeValue<Boolean> getInstalled();

  /**
   * Returns the value of the auto-create child.
   *
   * @return the value of the auto-create child.
   */
  @NotNull
  GenericAttributeValue<Boolean> getAutoCreate();
}
