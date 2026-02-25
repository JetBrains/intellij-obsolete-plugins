// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl;

import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspScriptletTag;

public class GspScriptletTagImpl extends GspXmlTagBaseImpl implements GspScriptletTag {

  public GspScriptletTagImpl() {
    super(GspElementTypes.GSP_SCRIPTLET_TAG);
  }

  @Override
  public String toString() {
    return "GSP scriptlet gtag";
  }

}
