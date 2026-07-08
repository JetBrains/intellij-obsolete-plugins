package com.intellij.lang.puppet.ide.completion.providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public final class PuppetVariableCompletionProvider extends PuppetCompletionProviderBase {
  @Override
  protected void addCompletions(@NotNull CompletionParameters parameters,
                                @NotNull ProcessingContext context,
                                @NotNull CompletionResultSet result) {

    PsiElement position = parameters.getPosition();
    PsiElement variable = position.getParent();
    if (!(variable instanceof PuppetVariable)) // looks like variable in wrong place
    {
      return;
    }

    if (((PuppetVariable)variable).isDeclaration() || ((PuppetVariable)variable).isParameter()) {
      return;
    }

    result = adjustPrefixMatcher(parameters, context, result);

    fillWithVariables(parameters, ((PuppetVariable)variable).isFullQualified(), result);
  }

}
