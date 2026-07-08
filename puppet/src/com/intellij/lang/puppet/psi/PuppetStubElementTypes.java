package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.psi.stubs.elementTypes.PuppetClassDefinitionElementType;
import com.intellij.lang.puppet.psi.stubs.elementTypes.PuppetFunctionDefinitionElementType;
import com.intellij.lang.puppet.psi.stubs.elementTypes.PuppetIncludeClassExpressionElementType;
import com.intellij.lang.puppet.psi.stubs.elementTypes.PuppetIncludeClassStatementElementType;
import com.intellij.lang.puppet.psi.stubs.elementTypes.PuppetNamespaceDefinitionElementType;
import com.intellij.lang.puppet.psi.stubs.elementTypes.PuppetNodeDefinitionElementType;
import com.intellij.lang.puppet.psi.stubs.elementTypes.PuppetResourceInstanceElementType;
import com.intellij.lang.puppet.psi.stubs.elementTypes.PuppetTypeDefinitionElementType;
import com.intellij.lang.puppet.psi.stubs.elementTypes.PuppetVariableElementType;
import com.intellij.psi.tree.IElementType;

public interface PuppetStubElementTypes {
  IElementType CLASS_DEFINITION = new PuppetClassDefinitionElementType("CLASS_DEFINITION");
  IElementType FUNCTION_DEFINITION = new PuppetFunctionDefinitionElementType("FUNCTION_DEFINITION");
  IElementType INCLUDE_CLASS_EXPRESSION = new PuppetIncludeClassExpressionElementType("INCLUDE_CLASS_EXPRESSION");
  IElementType INCLUDE_CLASS_STATEMENT = new PuppetIncludeClassStatementElementType("INCLUDE_CLASS_STATEMENT");
  IElementType NAMESPACE_DEFINITION = new PuppetNamespaceDefinitionElementType("NAMESPACE_DEFINITION");
  IElementType NODE_DEFINITION = new PuppetNodeDefinitionElementType("NODE_DEFINITION");
  IElementType RESOURCE_INSTANCE_DECLARATION = new PuppetResourceInstanceElementType("RESOURCE_INSTANCE_DECLARATION");
  IElementType TYPE_DEFINITION = new PuppetTypeDefinitionElementType("TYPE_DEFINITION");
  IElementType VAR_WRAPPER = new PuppetVariableElementType("VAR_WRAPPER");
}
