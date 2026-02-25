// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl;

import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspExpressionTag;

public class GspExprTagImpl extends GspXmlTagBaseImpl implements GspExpressionTag {
  public GspExprTagImpl() {
    super(GspElementTypes.GSP_EXPR_TAG);
  }

  @Override
  public String toString() {
    return "GspExprTagImpl";
  }

}