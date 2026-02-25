// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.GspLazyElement;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrMapAttributeValue;
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;

public class GrMapAttrValueImpl extends GspLazyElement implements GrMapAttributeValue {

  public GrMapAttrValueImpl(@NotNull IElementType type, @Nullable CharSequence buffer) {
    super(type, buffer);
  }

  @Override
  public String toString() {
    return "GrMapAttrValueImpl";
  }

  @Override
  public void accept(@NotNull GroovyElementVisitor visitor) {
    visitor.visitStatement(this);
  }

  @Override
  public @Nullable GrExpression getExpression() {
    return findChildByClass(GrExpression.class);
  }

  @Override
  public @Nullable <T extends GrStatement> T replaceWithStatement(T statement) {
    return null;
  }

  @Override
  public void removeStatement() {}
}
