package com.intellij.lang.puppet.psi.resolve;

import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class PuppetVariableResolveProcessor extends PuppetVariableScopeProcessor {
  private final String myVariableName;
  private final Set<PsiElement> myResult = new HashSet<>();
  private final Set<String> myFactsSet = new HashSet<>();

  public PuppetVariableResolveProcessor(String variableName) {
    myVariableName = variableName;
  }

  @Override
  public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
    if (element instanceof PuppetVariable && ((PuppetVariable)element).isDeclaration()) {
      String variableName = ((PuppetVariable)element).getName();
      if (StringUtil.equals(variableName, myVariableName) &&
          (!((PuppetVariable)element).isCoreFact() || !myFactsSet.contains(variableName))) {
        myResult.add(element);
        return false;
      }
    }
    return true;
  }

  @Override
  public void executeWithName(@NotNull String name, @NotNull PsiElement element) {
    if (StringUtil.equals(name, myVariableName)) {
      myResult.add(element);
      myFactsSet.add(name);
    }
  }

  public Set<PsiElement> getResult() {
    return myResult;
  }
}
