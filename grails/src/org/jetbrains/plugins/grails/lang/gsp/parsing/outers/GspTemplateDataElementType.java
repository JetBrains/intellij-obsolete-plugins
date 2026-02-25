// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.parsing.outers;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.ILeafElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.lexer.IGspElementType;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl.GspOuterHtmlElementImpl;

public class GspTemplateDataElementType extends IGspElementType implements ILeafElementType {

  public GspTemplateDataElementType() {
    super("GSP TEMPLATE STATEMENTS");
  }

  @Override
  public @NotNull ASTNode createLeafNode(@NotNull CharSequence leafText) {
    return new GspOuterHtmlElementImpl(this, leafText);
  }
}
