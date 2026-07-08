package com.intellij.lang.puppet.ide.completion.providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.lang.puppet.psi.PsiPuppetIncludeClassStatement;
import com.intellij.lang.puppet.psi.PuppetQuotedString;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public final class PuppetTextCompletionProvider extends PuppetCompletionProviderBase {
  @Override
  protected void addCompletions(@NotNull CompletionParameters parameters,
                                @NotNull ProcessingContext context,
                                @NotNull CompletionResultSet result) {

    PsiElement position = parameters.getPosition();
    PsiElement originalPosition = parameters.getOriginalPosition();

    PsiElement stringWrapper = originalPosition == null ? position.getParent() : originalPosition.getParent();

    if (stringWrapper instanceof PuppetQuotedString) {
      result = result.withPrefixMatcher(ElementManipulators.getValueText(stringWrapper));
    }
    //}

    // fixme duplicates CompletionProvider, should be refactored and merged
    if (CLASSNAME_IN_RESOURCE_LIKE_DECLARATION_PATTERN.accepts(stringWrapper) ||  // class { element:
        stringWrapper.getParent() instanceof PsiPuppetIncludeClassStatement
      ) {
      fillWithClasses(parameters, result, false);
    }
    else if (DATA_TYPE_PARAMETER_PATTERN.accepts(stringWrapper)) {  // Arbitrary::Type[element]
      computeDataTypeCompletion(parameters, context, result, false);
    }
  }
}
