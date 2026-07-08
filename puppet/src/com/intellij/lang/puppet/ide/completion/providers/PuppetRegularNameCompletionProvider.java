package com.intellij.lang.puppet.ide.completion.providers;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.lang.puppet.psi.PuppetPsiUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.lang.puppet.ide.completion.PuppetSyntaxLookupElements.getCaseCompoundElement;
import static com.intellij.lang.puppet.ide.completion.PuppetSyntaxLookupElements.getClassDefnitionElement;
import static com.intellij.lang.puppet.ide.completion.PuppetSyntaxLookupElements.getElseCompoundElement;
import static com.intellij.lang.puppet.ide.completion.PuppetSyntaxLookupElements.getElsifCompoundElement;
import static com.intellij.lang.puppet.ide.completion.PuppetSyntaxLookupElements.getFunctionDefinitionElement;
import static com.intellij.lang.puppet.ide.completion.PuppetSyntaxLookupElements.getIfCompoundElement;
import static com.intellij.lang.puppet.ide.completion.PuppetSyntaxLookupElements.getNodeDefinitionElement;
import static com.intellij.lang.puppet.ide.completion.PuppetSyntaxLookupElements.getResourceTypeDefinitionElement;
import static com.intellij.lang.puppet.ide.completion.PuppetSyntaxLookupElements.getUnlessCompoundElement;
import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.PRODUCES_CONSUMES_TOKENSET;

public class PuppetRegularNameCompletionProvider extends PuppetCompletionProviderBase {
  @Override
  protected void addCompletions(@NotNull CompletionParameters parameters,
                                @NotNull ProcessingContext context,
                                @NotNull CompletionResultSet result) {

    PsiElement nameElement = parameters.getPosition();
    PsiElement nameWrapper = nameElement.getParent();
    IElementType parentType = PsiUtilCore.getElementType(nameWrapper.getParent());

    result = adjustPrefixMatcher(parameters, context, result);

    // fixme rework with code
    if (result.getPrefixMatcher().getPrefix().isEmpty()) {
      PuppetCapitalizedNameCompletionProvider.INSTANCE.addCompletions(parameters, context, result);
    }

    if (PRODUCES_CONSUMES_TOKENSET.contains(parentType)) {
      fillWithResourceTypes(parameters, context, result, false);
    }
    else if (DATA_TYPE_PARAMETER_PATTERN.accepts(nameWrapper)) { // Some::Arbitrary::Type[element]
      computeDataTypeCompletion(parameters, context, result, false);
    }
    else if (PARENT_CLASS_REFERENCE_PATTERN.accepts(nameWrapper)) {          // INHERITS element
      fillWithClasses(parameters, result, false);
    }
    else if (RESOURCE_TYPE_REFERENCE_PATTERN.accepts(nameWrapper)) {    // element { resource instance..}
      fillWithResourceTypes(parameters, context, result, false);
    }
    else if (INCLUDED_CLASS_REFERENCE_PATTERN.accepts(nameWrapper)) {   // include/require/etc element
      fillWithClasses(parameters, result, false);
    }
    else if (FUNCTION_STATEMENT_PATTERN.accepts(nameWrapper)) // element, might be type or function
    {
      fillWithFunctions(parameters, context, result);
      fillWithResourceTypes(parameters, context, result, false);
      result.addElement(getClassDefnitionElement());
      result.addElement(getFunctionDefinitionElement());
      result.addElement(getResourceTypeDefinitionElement());
      result.addElement(getNodeDefinitionElement());
      result.addElement(getIfCompoundElement());
      result.addElement(getUnlessCompoundElement());
      result.addElement(getCaseCompoundElement());

      PsiElement statementElement = nameWrapper.getParent();
      PsiElement previousStatement = PuppetPsiUtil.getPrevNonSpaceSibling(statementElement);
      IElementType previousStatementElementType = PsiUtilCore.getElementType(previousStatement);
      if (previousStatementElementType == IF_STATEMENT) {
        PsiElement ifStatementLastBranch = previousStatement.getLastChild();

        if (PsiUtilCore.getElementType(ifStatementLastBranch) == CONDITIONAL_BRANCH) {
          result.addElement(getElsifCompoundElement());
          result.addElement(getElseCompoundElement());
        }
      }
      else if (previousStatementElementType == UNLESS_STATEMENT) {
        PsiElement unlessStatementLastBranch = previousStatement.getLastChild();
        if (PsiUtilCore.getElementType(unlessStatementLastBranch) == CONDITIONAL_BRANCH) {
          result.addElement(getElseCompoundElement());
        }
      }
    }
    else if (FUNCTION_CALL_PATTERN.accepts(nameWrapper)) // element( or element ...
    {
      fillWithFunctions(parameters, context, result);
    }
    else if (CLASSNAME_IN_RESOURCE_LIKE_DECLARATION_PATTERN.accepts(nameWrapper)) // class{ element: ...}
    {
      fillWithClasses(parameters, result, false);
    }
    else if (CLASS_ARGUMENT_PATTERN.accepts(nameWrapper)) { // element => ... inside class declaration
      fillWithClassParameters(parameters, context, result);
    }
    else if (RESOURCE_ARGUMENT_PATTERN.accepts(nameWrapper)) {  // element => inside resource instantiation
      fillWithTypeParameters(parameters, context, result);
    }
    if (parentType == RESOURCE_INSTANCE_DECLARATION &&
        nameWrapper.getNextSibling() instanceof PsiErrorElement) // resource instance fallback, may be an attribute
    {
      fillWithTypeParameters(parameters, context, result);
    }
  }
}
