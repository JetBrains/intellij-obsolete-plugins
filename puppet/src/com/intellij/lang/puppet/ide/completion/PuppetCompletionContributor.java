package com.intellij.lang.puppet.ide.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.lang.puppet.ide.completion.providers.PuppetCapitalizedNameCompletionProvider;
import com.intellij.lang.puppet.ide.completion.providers.PuppetRegularNameCompletionProvider;
import com.intellij.lang.puppet.ide.completion.providers.PuppetTextCompletionProvider;
import com.intellij.lang.puppet.ide.completion.providers.PuppetVariableCompletionProvider;
import com.intellij.lang.puppet.psi.PuppetElementPatterns;
import com.intellij.lang.puppet.psi.PuppetPsiFileImpl;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

public class PuppetCompletionContributor extends CompletionContributor implements PuppetElementPatterns {
  public static final String PUPPET_DUMMY_IDENTIFIER = StringUtil.decapitalize(CompletionUtil.DUMMY_IDENTIFIER_TRIMMED);

  public PuppetCompletionContributor() {
    extend(CompletionType.BASIC,
           REGULAR_NAME_PATTERN,
           new PuppetRegularNameCompletionProvider()
    );
    extend(CompletionType.BASIC,
           CAPITALIZED_NAME_PATTERN,
           new PuppetCapitalizedNameCompletionProvider()
    );
    extend(CompletionType.BASIC,
           VARIABLE_NAME_PATTERN,
           new PuppetVariableCompletionProvider()
    );
    extend(CompletionType.BASIC,
           STRING_PATTERN,
           new PuppetTextCompletionProvider()
    );
  }

  @Override
  public void beforeCompletion(@NotNull CompletionInitializationContext context) {
    if (context.getFile() instanceof PuppetPsiFileImpl) {
      context.setDummyIdentifier(PUPPET_DUMMY_IDENTIFIER);
    }
  }
}


