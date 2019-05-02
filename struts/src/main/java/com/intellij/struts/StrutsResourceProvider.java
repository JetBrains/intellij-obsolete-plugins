/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.intellij.struts;

import com.intellij.codeInsight.template.impl.DefaultLiveTemplatesProvider;
import com.intellij.javaee.ResourceRegistrar;
import com.intellij.javaee.StandardResourceProvider;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;

/**
 * @author Dmitry Avdeev
 */
public class StrutsResourceProvider implements StandardResourceProvider,
                                               DefaultLiveTemplatesProvider {

  @NonNls private static final String HTTP_PREFIX = "http://";

  @Override
  public void registerResources(ResourceRegistrar registrar) {
    // register all DTD mappings for user convenience
    registerDTDs(StrutsConstants.STRUTS_DTDS, registrar);
    registerDTDs(StrutsConstants.TILES_DTDS, registrar);
    registerDTDs(StrutsConstants.VALIDATOR_DTDS, registrar);
  }

  /**
   * Registers the given DTD URIs with files provided in our classpath (/com/intellij/struts/dtds/[filename]).
   *
   * @param dtds URIs.
   * @param registrar
   */
  private static void registerDTDs(final String[] dtds, ResourceRegistrar registrar) {
    for (String url : dtds) {
      if (url.startsWith(HTTP_PREFIX)) {
        int pos = url.lastIndexOf('/');
        @NonNls String file = "/dtds" + url.substring(pos);
        registrar.addStdResource(url, file, StrutsResourceProvider.class);
      }
    }
  }

  @Override
  public String[] getDefaultLiveTemplateFiles() {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @Override
  public String[] getHiddenLiveTemplateFiles() {
    return new String[] { "/liveTemplates/struts" };
  }
}
