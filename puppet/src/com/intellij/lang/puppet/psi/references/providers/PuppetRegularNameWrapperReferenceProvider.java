package com.intellij.lang.puppet.psi.references.providers;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

import static com.intellij.lang.puppet.lexer.PuppetTokenTypeSets.PRODUCES_CONSUMES_TOKENSET;

public class PuppetRegularNameWrapperReferenceProvider extends PuppetFullQualifiedNamedElementsReferencesProvider {


  @Override
  protected Delegation getLastFqnPartDelegation(@NotNull PsiElement element, String fullQualifiedName) {

    IElementType parentElementType = PsiUtilCore.getElementType(element.getParent());

    // fixme this need to be done with just a code
    if (PARENT_CLASS_REFERENCE_PATTERN.accepts(element)) {          // INHERITS element
      return Delegation.CLASS;
    }
    else if (RESOURCE_TYPE_REFERENCE_PATTERN.accepts(element)) {    // element { resource instance..}
      return Delegation.RESOURCE_TYPE;
    }
    else if (INCLUDED_CLASS_REFERENCE_PATTERN.accepts(element)) {   // include/require/etc element
      return Delegation.CLASS;
    }
    else if (FUNCTION_CALL_PATTERN.accepts(element)) // element(
    {
      return Delegation.FUNCTION;
    }
    else if (CLASSNAME_IN_RESOURCE_LIKE_DECLARATION_PATTERN.accepts(element)) // class{ element: ...}
    {
      return Delegation.CLASS;
    }
    else if (CLASS_ARGUMENT_PATTERN.accepts(element)) { // element => ... inside class declaration
      return Delegation.CLASS_PARAMETER;
    }
    else if (RESOURCE_ARGUMENT_PATTERN.accepts(element)) {  // element => inside resource instantiation
      return Delegation.TYPE_PARAMETER;
    }
    else if (PRODUCES_CONSUMES_TOKENSET.contains(parentElementType)) {
      return Delegation.RESOURCE_TYPE;
    }
    return Delegation.NONE;
  }
}
