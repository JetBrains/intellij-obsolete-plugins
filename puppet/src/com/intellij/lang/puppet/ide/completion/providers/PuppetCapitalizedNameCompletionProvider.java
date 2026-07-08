package com.intellij.lang.puppet.ide.completion.providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.PARAMETERS_HOLDERS_TOKENSET;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.PRODUCES_CONSUMES_TOKENSET;

public class PuppetCapitalizedNameCompletionProvider extends PuppetCompletionProviderBase {
  public static final PuppetCapitalizedNameCompletionProvider INSTANCE = new PuppetCapitalizedNameCompletionProvider();

  @Override
  protected void addCompletions(@NotNull CompletionParameters parameters,
                                @NotNull ProcessingContext context,
                                @NotNull CompletionResultSet result) {

    PsiElement nameElement = parameters.getPosition();
    PsiElement nameWrapper = nameElement.getParent();
    IElementType parentType = PsiUtilCore.getElementType(nameWrapper.getParent());

    result = adjustPrefixMatcher(parameters, context, result);

    // fixme rework with code
    if (PRODUCES_CONSUMES_TOKENSET.contains(parentType)) {
      fillWithResourceTypes(parameters, context, result, true);
    }
    else if (DATA_TYPE_PARAMETER_PATTERN.accepts(nameWrapper)) {                  // Some::Arbitrary::Type[element]
      computeDataTypeCompletion(parameters, context, result, true);
    }
    else if (RESOURCE_TYPE_REFERENCE_PATTERN.accepts(nameWrapper) ||
             RESOURCE_DEFAULTS_TYPE_PATTERN.accepts(nameWrapper)          // element{ ...
      ) {
      fillWithResourceTypes(parameters, context, result, true);
    }
    else if (PARENT_CLASS_REFERENCE_PATTERN.accepts(nameWrapper)             // INHERITS element
      ) {
      fillWithClasses(parameters, result, true);
    }
    else if (FUNCTION_STATEMENT_PATTERN.accepts(nameWrapper)) // element, might be a type
    {
      fillWithResourceTypes(parameters, context, result, true);
    }
    else {
      PsiElement grandParent = nameWrapper.getParent();
      IElementType grandParentElementType = PsiUtilCore.getElementType(grandParent);
      if (grandParentElementType == PARAMETER && nameWrapper.getPrevSibling() == null ||                           // Type $var
          (nameWrapper instanceof PsiErrorElement &&
           PARAMETERS_HOLDERS_TOKENSET
             .contains(grandParentElementType)) // this is a hacky branch; dummyidentifier is lowercased and parser can't eat this
        )
      {
        fillWithDataTypes(result);
      }
      else if (grandParentElementType == SELECTOR_VALUE || grandParentElementType == CASE_VALUES) // selector: Some::Type => ...
      {
        fillWithDataTypes(result);
      }
      else if (PsiUtilCore.getElementType(nameWrapper) ==
               CAPITALIZED_NAME_WRAPPER) { // fallback for bare CapitalizeName; basically it's a data type without params, see RUBY-18633
        fillWithResourceTypes(parameters, context, result, true);
        fillWithDataTypes(result);
      }
    }
  }
}
