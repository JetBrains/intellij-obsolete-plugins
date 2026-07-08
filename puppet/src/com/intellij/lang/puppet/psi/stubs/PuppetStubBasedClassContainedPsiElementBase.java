package com.intellij.lang.puppet.psi.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PuppetClassDefinition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil.SEPARATOR;

public abstract class PuppetStubBasedClassContainedPsiElementBase<S extends PuppetStubNamedElement>
  extends PuppetStubBasedNamedWithFqnContainerPsiElementBase<S> {
  public PuppetStubBasedClassContainedPsiElementBase(@NotNull S stub, @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public PuppetStubBasedClassContainedPsiElementBase(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  protected @Nullable String computeNamespaceName() {
    StringBuilder namespaceBuilder = new StringBuilder();
    boolean gotSomething = false;
    PuppetClassDefinition containingClass = getStubOrPsiParentOfType(PuppetClassDefinition.class);

    if (containingClass != null) {
      String containingClassFullQualifiedName = containingClass.getFullQualifiedName();
      if (containingClassFullQualifiedName != null) {
        gotSomething = true;
        namespaceBuilder.append(containingClassFullQualifiedName);
      }
    }

    String fqnContainerNamespaceName = super.computeNamespaceName();
    if (StringUtil.isNotEmpty(fqnContainerNamespaceName)) {
      if (!namespaceBuilder.isEmpty()) {
        namespaceBuilder.append(SEPARATOR);
      }
      gotSomething = true;
      namespaceBuilder.append(fqnContainerNamespaceName);
    }

    return gotSomething ? namespaceBuilder.toString() : null;
  }
}
