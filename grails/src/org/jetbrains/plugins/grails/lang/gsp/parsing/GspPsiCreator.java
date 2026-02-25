// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.parsing;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl.template.GspTemplateStatementImpl;

public final class GspPsiCreator implements GspGroovyElementTypes, GspElementTypes {
  private GspPsiCreator() {}

  public static PsiElement createElement(ASTNode node) {

    IElementType type = node.getElementType();

    if (GSP_TEMPLATE_STATEMENT.equals(type)) return new GspTemplateStatementImpl(node);

    return new ASTWrapperPsiElement(node);
  }
}
