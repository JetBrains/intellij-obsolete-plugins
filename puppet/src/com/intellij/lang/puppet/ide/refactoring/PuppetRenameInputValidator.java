package com.intellij.lang.puppet.ide.refactoring;

import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.refactoring.rename.RenameInputValidator;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class PuppetRenameInputValidator implements RenameInputValidator {
  private static final Pattern KEYWORDS =
    Pattern.compile("and|case|class|default|define|else|elsif|if|in|import|inherits|node|or|undef|unless");
  private static final Pattern VALID_NAME = Pattern.compile("(::)?[a-z][\\-a-zA-Z0-9_]*(::[a-z][\\-a-zA-Z0-9_]*)*");
  private static final Pattern VALID_REF_NAME = Pattern.compile("(::)?[A-Z][\\-a-zA-Z0-9_]*(::[A-Z][\\-a-zA-Z0-9_]*)*");
  private static final Pattern VALID_VAR_NAME = Pattern.compile("(::)?[a-zA-Z0-9_]+(::[a-zA-Z0-9_]+)*");

  @Override
  public @NotNull ElementPattern<? extends PsiElement> getPattern() {
    // check language instead of two instanceof
    return psiElement().with(new PatternCondition<>("puppet.named.element") {
      @Override
      public boolean accepts(@NotNull PsiElement element, ProcessingContext context) {
        return element.getLanguage() == PuppetLanguage.INSTANCE && element instanceof PsiNameIdentifierOwner;
      }
    });
  }

  @Override
  public boolean isInputValid(@NotNull String newName, @NotNull PsiElement element, @NotNull ProcessingContext context) {
    if (StringUtil.isEmpty(newName)) {
      return false;
    }

    // fixme we should introduce an interface for elements to self-validate their names
    if (element instanceof PuppetVariable) {
      return !StringUtil.isCapitalized(newName) && !isKeyword(newName) && VALID_VAR_NAME.matcher(newName).matches();
    }
    /*
    else if (element instanceof PuppetNameMixin) {
      return !StringUtil.isCapitalized(newName) && !isKeyword(newName) && VALID_NAME.matcher(newName).matches();
    }
    else if (element instanceof PuppetRefNameMixin) {
      return StringUtil.isCapitalized(newName) && !isKeyword(newName) && VALID_REF_NAME.matcher(newName).matches();
    }
    */

    return true;
  }

  private static boolean isKeyword(@NotNull String name) {
    return KEYWORDS.matcher(name).matches();
  }
}
