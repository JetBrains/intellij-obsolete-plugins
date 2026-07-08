package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.psi.resolve.PuppetResolveUtil;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.search.PsiElementProcessor;

import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.VARIABLE_TOKENSET;

public interface PuppetParametrizedDeclaration extends PsiNamedElement {

  default boolean processParametersDeclarations(PsiElementProcessor<? super PuppetVariable> processor) {
    return processVariablesDeclarations(element -> {
      if (element.isParameter()) {
        return processor.execute(element);
      }
      return true;
    });
  }

  default boolean processVariablesDeclarations(PsiElementProcessor<? super PuppetVariable> processor) {
    return PuppetResolveUtil.processStubBasedChildrenWithSmartStop(this, VARIABLE_TOKENSET, element -> {
      assert element instanceof PuppetVariable;
      return !((PuppetVariable)element).isDeclaration() || processor.execute((PuppetVariable)element);
    });
  }
}
