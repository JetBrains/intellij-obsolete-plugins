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
