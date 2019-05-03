/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

// Generated on Wed Apr 05 15:29:47 MSD 2006
// DTD/Schema  :    tiles-config_1_3.dtd

package com.intellij.struts.dom;

import com.intellij.util.xml.GenericDomValue;

/**
 * tiles-config_1_3.dtd:icon interface.
 * Type icon documentation
 * <pre>
 *  The "icon" element contains a small-icon and large-icon element which
 *      specify the location, relative to the Struts configuration file, for small
 *      and large images used to represent the surrounding element in GUI tools.
 * </pre>
 */
public interface Icon extends StrutsRootElement {

  /**
   * Returns the value of the small-icon child.
   * Type small-icon documentation
   * <pre>
   *  The "small-icon" element specifies the location, relative to the Struts
   *      configuration file, of a resource containing a small (16x16 pixel)
   *      icon image.
   * </pre>
   *
   * @return the value of the small-icon child.
   */
  GenericDomValue<String> getSmallIcon();


  /**
   * Returns the value of the large-icon child.
   * Type large-icon documentation
   * <pre>
   *  The "large-icon" element specifies the location, relative to the Struts
   *      configuration file, of a resource containing a large (32x32 pixel)
   *      icon image.
   * </pre>
   *
   * @return the value of the large-icon child.
   */
  GenericDomValue<String> getLargeIcon();


}
