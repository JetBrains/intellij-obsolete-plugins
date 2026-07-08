package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PsiPuppetIncludeClassExpression;
import com.intellij.lang.puppet.psi.PsiPuppetResourceLikeClassDeclarationBlock;
import com.intellij.lang.puppet.psi.PsiPuppetResourceLikeClassDescription;
import com.intellij.lang.puppet.psi.PuppetIncludeClassStatement;
import com.intellij.lang.puppet.psi.stubs.PuppetStubBasedPsiElementBase;
import com.intellij.lang.puppet.psi.stubs.impl.PuppetIncludeClassStatementStub;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class PuppetIncludeClassExpressionMixin extends PuppetStubBasedPsiElementBase<PuppetIncludeClassStatementStub>
  implements PuppetIncludeClassStatement, PsiPuppetIncludeClassExpression {
  public PuppetIncludeClassExpressionMixin(@NotNull PuppetIncludeClassStatementStub stub,
                                           @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public PuppetIncludeClassExpressionMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @NotNull Collection<String> getClassNames() {
    PsiPuppetResourceLikeClassDeclarationBlock block = getResourceLikeClassDeclarationBlock();
    if (block == null) {
      return Collections.emptyList();
    }

    List<String> result = new ArrayList<>();
    for (PsiPuppetResourceLikeClassDescription description : block.getResourceLikeClassDescriptionList()) {
      result.addAll(description.getNamesList());
    }

    return result;
  }
}
