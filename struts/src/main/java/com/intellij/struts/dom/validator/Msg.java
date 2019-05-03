/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Generated on Wed Apr 05 15:23:26 MSD 2006
// DTD/Schema  :    validator_1_2_0.dtd

package com.intellij.struts.dom.validator;

import com.intellij.ide.presentation.Presentation;
import com.intellij.struts.dom.StrutsRootElement;
import com.intellij.struts.dom.validator.converters.ValidatorNameConverter;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NotNull;

/**
 * validator_1_2_0.dtd:msg interface.
 * Type msg documentation
 * <pre>
 *      The "msg" element defines a custom message key to use when one of the
 *      validators for this field fails. Each validator has a default message
 *      property that is used when a corresponding field msg is not specified.
 *      Each validator applied to a field may have its own msg element.
 *      The msg element accepts these attributes.
 *       name        The name of the validator corresponding to this msg.
 * <p/>
 *       bundle     The resource bundle name that the key should be resolved in.
 *       key         The key that will return the message template from a
 *                   resource bundle.
 *       resource    If set to "false", the key is taken to be a literal
 *                   value rather than a bundle key.
 *                   [true]
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.Validator.Msg")
public interface Msg extends StrutsRootElement {

  /**
   * Returns the value of the bundle child.
   * Attribute bundle
   *
   * @return the value of the bundle child.
   */
  @NotNull
  GenericAttributeValue<String> getBundle();


  /**
   * Returns the value of the name child.
   * Attribute name
   *
   * @return the value of the name child.
   */
  @Convert(ValidatorNameConverter.class)
  @NameValue(referencable = false)
  @NotNull
  @Required
  GenericAttributeValue<Validator> getName();


  /**
   * Returns the value of the key child.
   * Attribute key
   *
   * @return the value of the key child.
   */
  @NotNull
  @Required
  GenericAttributeValue<String> getKey();


  /**
   * Returns the value of the resource child.
   * Attribute resource
   *
   * @return the value of the resource child.
   */
  @NotNull
  GenericAttributeValue<Boolean> getResource();


}
