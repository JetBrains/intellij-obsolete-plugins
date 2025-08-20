package com.intellij.plugins.jboss.arquillian.configuration.container;

public enum ArquillianContainerKind {
  Embedded(ArquillianContainer.Scope.Embedded),
  Managed(ArquillianContainer.Scope.Managed),
  Remote(ArquillianContainer.Scope.Remote);

  private final ArquillianContainer.Scope myScope;

  ArquillianContainerKind(ArquillianContainer.Scope scope) {
    myScope = scope;
  }

  public ArquillianContainer.Scope getScope() {
    return myScope;
  }
}
