// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl.template;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.template.GspTemplateStatement;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiElementImpl;

public class GspTemplateStatementImpl extends GroovyPsiElementImpl implements GspTemplateStatement {

  public GspTemplateStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public String toString() {
    return "GspTemplateStatement";
  }

}
