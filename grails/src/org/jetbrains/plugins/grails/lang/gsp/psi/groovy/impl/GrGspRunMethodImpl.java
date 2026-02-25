// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl;

import com.intellij.lang.ASTNode;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrGspRunBlock;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrGspRunMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiElementImpl;

public class GrGspRunMethodImpl extends GroovyPsiElementImpl implements GrGspRunMethod {

  private static final String GSPMETHOD_SYNTHETIC_NAME = "GspRunMethod";

  public GrGspRunMethodImpl(ASTNode node) {
    super(node);
  }

  @Override
  public String toString() {
    return GSPMETHOD_SYNTHETIC_NAME;
  }

  @Override
  public GrGspRunBlock getRunBlock() {
    GrGspRunBlock runBlock = findChildByClass(GrGspRunBlock.class);
    assert runBlock != null;
    return runBlock;
  }
}
