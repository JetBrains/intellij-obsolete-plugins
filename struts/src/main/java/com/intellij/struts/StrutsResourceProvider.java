/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
