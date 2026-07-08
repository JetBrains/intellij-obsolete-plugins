package com.intellij.lang.puppet.psi;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import static com.intellij.lang.puppet.psi.PuppetStubElementTypes.CLASS_DEFINITION;
import static com.intellij.lang.puppet.psi.PuppetStubElementTypes.FUNCTION_DEFINITION;
import static com.intellij.lang.puppet.psi.PuppetStubElementTypes.INCLUDE_CLASS_EXPRESSION;
import static com.intellij.lang.puppet.psi.PuppetStubElementTypes.INCLUDE_CLASS_STATEMENT;
import static com.intellij.lang.puppet.psi.PuppetStubElementTypes.NAMESPACE_DEFINITION;
import static com.intellij.lang.puppet.psi.PuppetStubElementTypes.NODE_DEFINITION;
import static com.intellij.lang.puppet.psi.PuppetStubElementTypes.RESOURCE_INSTANCE_DECLARATION;
import static com.intellij.lang.puppet.psi.PuppetStubElementTypes.TYPE_DEFINITION;
import static com.intellij.lang.puppet.psi.PuppetStubElementTypes.VAR_WRAPPER;

public final class PuppetElementTypeFactory {
  private PuppetElementTypeFactory() {
  }

  public static IElementType getElementType(@NotNull String name) {
    return switch (name) {
      case "CLASS_DEFINITION" -> CLASS_DEFINITION;
      case "FUNCTION_DEFINITION" -> FUNCTION_DEFINITION;
      case "INCLUDE_CLASS_EXPRESSION" -> INCLUDE_CLASS_EXPRESSION;
      case "INCLUDE_CLASS_STATEMENT" -> INCLUDE_CLASS_STATEMENT;
      case "NAMESPACE_DEFINITION" -> NAMESPACE_DEFINITION;
      case "NODE_DEFINITION" -> NODE_DEFINITION;
      case "RESOURCE_INSTANCE_DECLARATION" -> RESOURCE_INSTANCE_DECLARATION;
      case "TYPE_DEFINITION" -> TYPE_DEFINITION;
      case "VAR_WRAPPER" -> VAR_WRAPPER;
      default -> new PuppetElementType(name);
    };
  }

  public static IElementType getTokenType(@NotNull String name) {
    if (name.equals("HEREDOC_BODY_QQ")) {
      return new PuppetQQHeredocElementType(name);
    }
    return new PuppetElementType(name);
  }
}
