// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.parsing;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.ICompositeElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspTokenTypesEx;
import org.jetbrains.plugins.grails.lang.gsp.lexer.IGspElementType;
import org.jetbrains.plugins.grails.lang.gsp.parsing.groovy.chameleons.GroovyDeclarationsInGspFileRoot;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl.GrGspClassImpl;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl.GrGspRunBlockImpl;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl.GrGspRunMethodImpl;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyElementType;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;

public interface GspGroovyElementTypes extends GspTokenTypesEx {
  IFileElementType GSP_GROOVY_DECLARATIONS_ROOT = new GroovyDeclarationsInGspFileRoot("GROOVY_DECLARATIONS_IN_GSP_FILE");

  IElementType GSP_CLASS = new GroovyElementType.PsiCreator("GSP_CLASS") {
    @Override
    public @NotNull GroovyPsiElement createPsi(@NotNull ASTNode node) {
      return new GrGspClassImpl(node);
    }
  };
  IElementType GSP_RUN_METHOD = new GroovyElementType.PsiCreator("GSP_RUN_METHOD") {
    @Override
    public @NotNull GroovyPsiElement createPsi(@NotNull ASTNode node) {
      return new GrGspRunMethodImpl(node);
    }
  };
  IElementType GSP_RUN_BLOCK = new GspRunBlockElementType();
  
  IElementType GSP_TEMPLATE_STATEMENT = new IGspElementType("GSP_TEMPLATE_STATEMENT");

  class GspRunBlockElementType extends GroovyElementType implements ICompositeElementType {
    public GspRunBlockElementType() {
      super("GSP_RUN_BLOCK");
    }

    @Override
    public @NotNull ASTNode createCompositeNode() {
      return new GrGspRunBlockImpl(this, null);
    }
  }
}
