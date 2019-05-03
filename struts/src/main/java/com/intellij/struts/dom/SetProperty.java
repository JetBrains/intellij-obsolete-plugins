/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Generated on Wed Apr 05 15:29:47 MSD 2006
// DTD/Schema  :    tiles-config_1_3.dtd

package com.intellij.struts.dom;

import com.intellij.ide.presentation.Presentation;
import com.intellij.struts.dom.converters.StrutsElementNamer;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Stubbed;
import org.jetbrains.annotations.NotNull;

/**
 * tiles-config_1_3.dtd:set-property interface.
 * Type set-property documentation
 * <pre>
 *  The "set-property" element specifies the method name and initial value of
 *      a bean property. When the object representing
 *      the surrounding element is instantiated, the accessor for the indicated
 *      property is called and passed the indicated value.
 *
 *  property       Name of the JavaBeans property whose setter method
 *                will be called. Exactly one of
 *                "property" or "key" must be specified.
 *
 *  key            Where supported, the key which will be used to store
 *                the specified value in the given config object.  Exactly one of
 *                "property" or "key" must be specified.
 *
 *  value          String representation of the value to which this
 *                property will be set, after suitable type conversion
 * </pre>
 */
@Presentation(provider = StrutsElementNamer.class, icon = "StrutsApiIcons.SetProperty")
public interface SetProperty extends StrutsRootElement {

  /**
   * Returns the value of the value child.
   * Attribute value
   *
   * @return the value of the value child.
   */
  @Stubbed
  @Required
  @NotNull
  GenericAttributeValue<String> getValue();


  /**
   * Returns the value of the property child.
   * Attribute property
   *
   * @return the value of the property child.
   */
  @Stubbed
  @NotNull
  GenericAttributeValue<String> getProperty();


  @Stubbed
  @NotNull
  GenericAttributeValue<String> getKey();

}
