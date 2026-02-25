// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.parsing;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.xml.IXmlElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspTokenTypesEx;
import org.jetbrains.plugins.grails.lang.gsp.parsing.composite.GspCompositeElementType;
import org.jetbrains.plugins.grails.lang.gsp.parsing.html.elements.GspXmlDocument;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.GspDeclarationTagImpl;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.GspExprTagImpl;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.GspScriptletTagImpl;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.directive.GspDirectiveAttributeImpl;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.directive.GspDirectiveAttributeValueImpl;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag.GspAttributeImpl;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag.GspAttributeValueImpl;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag.GspGrailsTagImpl;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag.GspXmlRootTagImpl;

public interface GspElementTypes extends GspTokenTypesEx {

  /*
  "Pure" GSP elements
   */

  IXmlElementType GSP_ROOT_TAG = new GspCompositeElementType("GSP_ROOT_TAG") {
    @Override
    public @NotNull ASTNode createCompositeNode() {
      return new GspXmlRootTagImpl();
    }
  };

  IXmlElementType GSP_SCRIPTLET_TAG = new GspCompositeElementType("GSP_SCRIPTLET_TAG") {
    @Override
    public @NotNull ASTNode createCompositeNode() {
      return new GspScriptletTagImpl();
    }
  };

  IXmlElementType GSP_XML_DOCUMENT = new GspCompositeElementType("GSP_XML_DOCUMENT") {
    @Override
    public @NotNull ASTNode createCompositeNode() {
      return new GspXmlDocument();
    }
  };

  IXmlElementType GSP_EXPR_TAG = new GspCompositeElementType("GSP_EXPR_TAG") {
    @Override
    public @NotNull ASTNode createCompositeNode() {
      return new GspExprTagImpl();
    }
  };

  IXmlElementType GSP_DECLARATION_TAG = new GspCompositeElementType("GSP_DECLARATION_TAG") {
    @Override
    public @NotNull ASTNode createCompositeNode() {
      return new GspDeclarationTagImpl();
    }
  };

  IXmlElementType GRAILS_TAG = new GspCompositeElementType("GRAILS_TAG") {
    @Override
    public @NotNull ASTNode createCompositeNode() {
      return new GspGrailsTagImpl();
    }
  };

  IXmlElementType GRAILS_TAG_ATTRIBUTE = new GspCompositeElementType("GRAILS_TAG_ATTRIBUTE") {
    @Override
    public @NotNull ASTNode createCompositeNode() {
      return new GspAttributeImpl();
    }
  };

  IXmlElementType GSP_ATTRIBUTE_VALUE = new GspCompositeElementType("GSP_ATTRIBUTE_VALUE") {
    @Override
    public @NotNull ASTNode createCompositeNode() {
      return new GspAttributeValueImpl();
    }
  };

  // Composite directive elements
  IXmlElementType GSP_DIRECTIVE_ATTRIBUTE = new GspCompositeElementType("GSP_DIRECTIVE_ATTRIBUTE") {
    @Override
    public @NotNull ASTNode createCompositeNode() {
      return new GspDirectiveAttributeImpl();
    }
  };

  IXmlElementType GSP_DIRECTIVE_ATTRIBUTE_VALUE = new GspCompositeElementType("GSP_DIRECTIVE_ATTRIBUTE_VALUE") {
    @Override
    public @NotNull ASTNode createCompositeNode() {
      return new GspDirectiveAttributeValueImpl();
    }
  };

}
