package com.intellij.lang.puppet.psi;

import com.intellij.lang.puppet.PuppetTokenTypes;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;

import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.FUNCTION_STATEMENTS;
import static com.intellij.lang.puppet.psi.PuppetElementPatternConditions.firstChildOfParent;
import static com.intellij.lang.puppet.psi.PuppetElementPatternConditions.firstChildOrAfterTokenInParent;
import static com.intellij.lang.puppet.psi.PuppetElementPatternConditions.nonFirstChildOfParent;
import static com.intellij.patterns.PlatformPatterns.psiElement;

public interface PuppetElementPatterns extends PuppetTokenTypes {
  PsiElementPattern.Capture<PsiElement> REGULAR_NAME_WRAPPER_PATTERN = psiElement(REGULAR_NAME_WRAPPER);
  PsiElementPattern.Capture<PsiElement> REGULAR_NAME_PATTERN = psiElement(NAME);
  PsiElementPattern.Capture<PsiElement> CAPITALIZED_NAME_WRAPPER_PATTERN = psiElement(CAPITALIZED_NAME_WRAPPER);
  PsiElementPattern.Capture<PsiElement> CAPITALIZED_NAME_PATTERN = psiElement(CAPITALIZED_NAME);
  PsiElementPattern.Capture<PsiElement> VARIABLE_PATTERN = psiElement(VAR_WRAPPER);
  PsiElementPattern.Capture<PsiElement> VARIABLE_NAME_PATTERN = psiElement(VARIABLE_NAME);
  PsiElementPattern.Capture<PsiElement> QUOTED_TEXT_PATTERN = psiElement(QUOTED_TEXT);
  PsiElementPattern.Capture<PsiElement> STRING_PATTERN =
    psiElement().andOr(psiElement(SINGLE_QUOTED_STRING), psiElement(DOUBLE_QUOTED_STRING));
  PsiElementPattern.Capture<PsiElement> CLASS_DEFINITION_PATTERN = psiElement(CLASS_DEFINITION);
  PsiElementPattern.Capture<PuppetNameWrapper> NAME_WRAPPER_PATTERN = psiElement(PuppetNameWrapper.class);
  PsiElementPattern.Capture<PsiElement> RESOURCE_DECLARATION_PATTERN = psiElement(RESOURCE_DECLARATION);
  PsiElementPattern.Capture<PsiElement> INCLUDE_CLASS_STATEMENT_PATTERN = psiElement(INCLUDE_CLASS_STATEMENT);
  PsiElementPattern.Capture<PsiElement> DATA_TYPE_PATTERN = psiElement(DATA_TYPE);
  PsiElementPattern.Capture<PsiElement> NAMESPACE_DEFINITION_PATTERN = psiElement(NAMESPACE_DEFINITION);
  PsiElementPattern.Capture<PsiElement> PARAMETER_PATTERN = psiElement(PARAMETER);
  PsiElementPattern.Capture<PsiElement> COLLECTION_PATTERN = psiElement(COLLECTION);

  // class { classname...
  PsiElementPattern.Capture<PsiElement> CLASSNAME_IN_RESOURCE_LIKE_DECLARATION_PATTERN =
    psiElement().andOr(
      psiElement().with(firstChildOfParent(RESOURCE_LIKE_CLASS_DESCRIPTION)),                             // class{ classname:...
      psiElement().withParent(psiElement(ARRAY).with(firstChildOfParent(RESOURCE_LIKE_CLASS_DESCRIPTION)))
      // class{[classname1, classname2...
    );

  PsiElementPattern.Capture<PuppetNameWrapper> PARENT_CLASS_REFERENCE_PATTERN = NAME_WRAPPER_PATTERN.withParent(CLASS_DEFINITION_PATTERN);

  PsiElementPattern.Capture<PuppetNameWrapper>
    DATA_TYPE_FIRST_CHILD_PATTERN = NAME_WRAPPER_PATTERN.with(firstChildOfParent(DATA_TYPE_PATTERN));

  // Type::Name[...
  PsiElementPattern.Capture<PuppetNameWrapper>
    TYPE_REFERENCE_IN_DATA_TYPE_PATTERN = DATA_TYPE_FIRST_CHILD_PATTERN.beforeLeaf(psiElement(LBRACK));

  TokenSet RESOURCE_MODIFIER_TOKENSET = TokenSet.create(AT, ATAT);

  // Some::Type { resource instances... }
  PsiElementPattern.Capture<PuppetNameWrapper> RESOURCE_TYPE_REFERENCE_PATTERN =
    NAME_WRAPPER_PATTERN.with(firstChildOrAfterTokenInParent(RESOURCE_DECLARATION_PATTERN, RESOURCE_MODIFIER_TOKENSET));

  // include/require/etc class::name, class::name
  PsiElementPattern.Capture<PuppetNameWrapper> INCLUDED_CLASS_REFERENCE_PATTERN =
    NAME_WRAPPER_PATTERN.with(nonFirstChildOfParent(INCLUDE_CLASS_STATEMENT_PATTERN));

  // Something::Here[element]
  PsiElementPattern.Capture<PsiElement> DATA_TYPE_PARAMETER_PATTERN = psiElement().withParent(
    psiElement(DATA_TYPE_PARAMETERS_BLOCK).withParent(
      psiElement(DATA_TYPE)
    )
  );

  // Some::Type $var
  PsiElementPattern.Capture<PuppetNameWrapper> ARGUMENT_TYPE_PATTERN = NAME_WRAPPER_PATTERN.with(firstChildOfParent(PARAMETER_PATTERN));

  // Type <|....
  PsiElementPattern.Capture<PuppetNameWrapper> COLLECTION_TYPE_PATTERN = NAME_WRAPPER_PATTERN.with(firstChildOfParent(COLLECTION_PATTERN));

  // some::name(...
  PsiElementPattern.Capture<PuppetNameWrapper> FUNCTION_CALL_PATTERN = NAME_WRAPPER_PATTERN.with(firstChildOfParent(FUNCTION_STATEMENTS));

  // incomplete statement, can be type or function
  // some::name
  PsiElementPattern.Capture<PuppetNameWrapper>
    FUNCTION_STATEMENT_PATTERN = NAME_WRAPPER_PATTERN.with(firstChildOfParent(FUNCTION_CALL_STATEMENT));

  PsiElementPattern.Capture<PuppetNameWrapper> ARGUMENT_PATTERN = NAME_WRAPPER_PATTERN.with(
    firstChildOfParent(psiElement(PsiPuppetArgument.class).withParent(psiElement(RESOURCE_ARGUMENTS_LIST)))
  );

  // argument to resource-like class declaration
  PsiElementPattern.Capture<PuppetNameWrapper> CLASS_ARGUMENT_PATTERN = ARGUMENT_PATTERN
    .withSuperParent(3, psiElement(RESOURCE_LIKE_CLASS_DESCRIPTION));

  // argument to resource instance/defaults reference
  PsiElementPattern.Capture<PuppetNameWrapper> RESOURCE_ARGUMENT_PATTERN = ARGUMENT_PATTERN.andOr(
    psiElement().withSuperParent(3, psiElement(RESOURCE_INSTANCE_DECLARATION)), // resource instance
    psiElement().withSuperParent(4, psiElement(RESOURCE_DEFAULT_STATEMENT))     // type defaults
  );

  // Resource::Type{ ...
  PsiElementPattern.Capture<PuppetNameWrapper> RESOURCE_DEFAULTS_TYPE_PATTERN =
    NAME_WRAPPER_PATTERN.with(firstChildOfParent(RESOURCE_DEFAULT_STATEMENT));

}
