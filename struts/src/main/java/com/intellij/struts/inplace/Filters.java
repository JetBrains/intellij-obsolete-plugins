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

package com.intellij.struts.inplace;

import com.intellij.psi.filters.ElementFilter;
import com.intellij.psi.filters.position.NamespaceFilter;
import com.intellij.struts.StrutsConstants;

/**
 * Contains {@link ElementFilter} related constants and utility methods.
 *
 * @see com.intellij.struts.inplace.reference.StrutsReferenceContributor
 */
public class Filters {

  public final static NamespaceFilter NAMESPACE_STRUTS_CONFIG = new NamespaceFilter(StrutsConstants.STRUTS_DTDS);

  public static final NamespaceFilter NAMESPACE_TILES_CONFIG = new NamespaceFilter(StrutsConstants.TILES_DTDS);

  public static final NamespaceFilter NAMESPACE_VALIDATOR_CONFIG = new NamespaceFilter(StrutsConstants.VALIDATOR_DTDS);

  public static final NamespaceFilter NAMESPACE_TAGLIB_STRUTS_HTML = new NamespaceFilter("http://jakarta.apache.org/struts/tags-html",
                                                                                         "http://jakarta.apache.org/struts/tags-html-el",
                                                                                         "http://struts.apache.org/tags-html",
                                                                                         "http://struts.apache.org/tags-html-el");

  public static final NamespaceFilter NAMESPACE_TAGLIB_STRUTS_LOGIC = new NamespaceFilter("http://jakarta.apache.org/struts/tags-logic",
                                                                                          "http://struts.apache.org/tags-logic",
                                                                                          "http://jakarta.apache.org/struts/tags-logic-el",
                                                                                          "http://struts.apache.org/tags-logic-el");

  public static final NamespaceFilter NAMESPACE_TAGLIB_STRUTS_BEAN = new NamespaceFilter("http://jakarta.apache.org/struts/tags-bean",
                                                                                         "http://struts.apache.org/tags-bean",
                                                                                         "http://jakarta.apache.org/struts/tags-bean-el",
                                                                                         "http://struts.apache.org/tags-bean-el");

  public static final NamespaceFilter NAMESPACE_TAGLIB_STRUTS_TILES = new NamespaceFilter("http://jakarta.apache.org/struts/tags-tiles",
                                                                                          "http://struts.apache.org/tags-tiles",
                                                                                          "http://jakarta.apache.org/struts/tags-tiles-el",
                                                                                          "http://struts.apache.org/tags-tiles-el");

  private Filters() {
  }

}