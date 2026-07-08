package com.intellij.lang.puppet.psi.references.providers;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.PRODUCES_CONSUMES_TOKENSET;
import static com.intellij.lang.puppet.psi.PuppetDataTypes.ALL_LOWERCASED_DATA_TYPES;

public class PuppetCapitalizedNameReferenceProvider extends PuppetFullQualifiedNamedElementsReferencesProvider {

  @Override
  protected Delegation getLastFqnPartDelegation(@NotNull PsiElement element, @NonNls String fullQualifiedName) {

    IElementType parentElementType = PsiUtilCore.getElementType(element.getParent());

    // fixme this need to be done with just a code
    if (PARENT_CLASS_REFERENCE_PATTERN.accepts(element)             // INHERITS element
      ) {
      return Delegation.CLASS;
    }
    else if (TYPE_REFERENCE_IN_DATA_TYPE_PATTERN.accepts(element)   // element[...
      ) {
      return ALL_LOWERCASED_DATA_TYPES.contains(StringUtil.toLowerCase(fullQualifiedName)) ? Delegation.DATA_TYPE : Delegation.RESOURCE_TYPE;
    }
    else if (
      COLLECTION_TYPE_PATTERN.accepts(element) ||              // element <|....
      RESOURCE_TYPE_REFERENCE_PATTERN.accepts(element) ||
      RESOURCE_DEFAULTS_TYPE_PATTERN.accepts(element)          // element{ ...
      ) {
      return Delegation.RESOURCE_TYPE;
    }
    else if (ARGUMENT_TYPE_PATTERN.accepts(element)                 // element $var
      ) {
      return Delegation.DATA_TYPE;
    }
    else if (PRODUCES_CONSUMES_TOKENSET.contains(parentElementType)) {
      return Delegation.RESOURCE_TYPE;
    }
    else if (parentElementType == SELECTOR_VALUE || parentElementType == CASE_VALUES) {
      return Delegation.DATA_TYPE;
    }

    // fallback for bare CapitalizeName; basically it's a data type without params, see RUBY-18633
    return Delegation.RESOURCE_OR_DATA_TYPE;
  }
}
