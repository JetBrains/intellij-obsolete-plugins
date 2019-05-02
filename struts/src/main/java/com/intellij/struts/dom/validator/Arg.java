/*
 * Copyright 2000-2006 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Generated on Wed Apr 05 15:23:26 MSD 2006
// DTD/Schema  :    validator_1_2_0.dtd

package com.intellij.struts.dom.validator;

import com.intellij.ide.presentation.Presentation;
import com.intellij.struts.dom.StrutsRootElement;
import com.intellij.struts.dom.converters.StrutsElementNamer;
import com.intellij.struts.dom.validator.converters.ValidatorNameConverter;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NotNull;

/**
 * validator_1_2_0.dtd:arg interface.
 * Type arg documentation
 * <pre>
 *      The "arg" element defines a replacement value to use with the
 *      message template for this validator or this field.
 *      The arg element accepts these attributes.
 *       name        The name of the validator corresponding to this msg.
 *       			  If not supplied, this argument will be used in the given
 *       			  position for every validator.
 * <p/>
 *       bundle     The resource bundle name that the key should be resolved in.
 *       key         The key that will return the message template from a
 *                   resource bundle.
 *       resource    If set to "false", the key is taken to be a literal
 *                   value rather than a bundle key.
 *                   [true]
 *       position    The position of this replacement parameter in the message.
 *       			  For example, position="0" will set the first argument.
 *       			  [0]
 * </pre>
 */
@Presentation(icon = "StrutsApiIcons.Validator.Arg",
              provider = StrutsElementNamer.class)
public interface Arg extends StrutsRootElement {

  /**
   * Returns the value of the position child.
   * Attribute position
   *
   * @return the value of the position child.
   */
  @NotNull
  GenericAttributeValue<Integer> getPosition();


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
