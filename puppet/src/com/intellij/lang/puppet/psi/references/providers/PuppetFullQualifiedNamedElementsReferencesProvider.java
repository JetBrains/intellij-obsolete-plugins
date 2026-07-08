package com.intellij.lang.puppet.psi.references.providers;

import com.intellij.lang.puppet.psi.PuppetDataType;
import com.intellij.lang.puppet.psi.PuppetDataTypeParameterInfo;
import com.intellij.lang.puppet.psi.PuppetElementPatterns;
import com.intellij.lang.puppet.psi.PuppetVariable;
import com.intellij.lang.puppet.psi.references.PuppetClassDefinitionReference;
import com.intellij.lang.puppet.psi.references.PuppetClassParameterReference;
import com.intellij.lang.puppet.psi.references.PuppetDataTypeReference;
import com.intellij.lang.puppet.psi.references.PuppetFunctionReference;
import com.intellij.lang.puppet.psi.references.PuppetNamespaceReference;
import com.intellij.lang.puppet.psi.references.PuppetResourceInstanceReference;
import com.intellij.lang.puppet.psi.references.PuppetTypeDefinitionReference;
import com.intellij.lang.puppet.psi.references.PuppetTypeParameterReference;
import com.intellij.lang.puppet.psi.references.PuppetVariableReference;
import com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.ElementManipulator;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.lang.puppet.util.PuppetQualifiedNamesUtil.SEPARATOR;

public abstract class PuppetFullQualifiedNamedElementsReferencesProvider extends PsiReferenceProvider implements PuppetElementPatterns {
  enum Delegation {
    NONE,
    CLASS,
    RESOURCE_TYPE,
    RESOURCE_OR_DATA_TYPE,
    CLASS_PARAMETER,
    TYPE_PARAMETER,
    NAMESPACE,
    FUNCTION,
    DATA_TYPE,
    VARIABLE
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {

    PuppetDataTypeParameterInfo parameterInfo = getTypeParameterInfo(element);
    if (parameterInfo != null && parameterInfo.isResourceInstance()) {
      String resourceDataType = parameterInfo.getResourceDataType();
      assert resourceDataType != null;
      return new PsiReference[]{new PuppetResourceInstanceReference(element, resourceDataType)};
    }

    return parameterInfo != null || isFullQualifiedElement(element)
           ? getFullQualifiedReferencesForElement(element, parameterInfo)
           : PsiReference.EMPTY_ARRAY;
  }

  public PsiReference[] getFullQualifiedReferencesForElement(@NotNull PsiElement element,
                                                             @Nullable PuppetDataTypeParameterInfo dataTypeParameterInfo) {

    ElementManipulator<PsiElement> manipulator = ElementManipulators.getNotNullManipulator(element);
    TextRange elementRange = manipulator.getRangeInElement(element);
    if (elementRange.isEmpty()) {
      return PsiReference.EMPTY_ARRAY;
    }

    List<TextRange> elementRanges = PuppetQualifiedNamesUtil.getQualifiedPartRanges(element, elementRange);
    String elementText = element.getText();

    String textInRange = elementRange.substring(elementText);

    int startOffset = elementRange.getStartOffset();

    if (StringUtil.startsWith(textInRange, SEPARATOR)) {
      startOffset += SEPARATOR.length();
    }

    final List<PsiReference> references = new ArrayList<>();
    int rangesNumber = elementRanges.size();

    for (int i = 0; i < rangesNumber; i++) {
      final TextRange range = elementRanges.get(i);
      if (range.isEmpty()) {
        continue;
      }
      final TextRange fullQualifiedRange = TextRange.create(startOffset, range.getEndOffset());
      final String fullQualifiedName = fullQualifiedRange.substring(elementText);

      Delegation result;
      if (i == rangesNumber - 1) {

        if (dataTypeParameterInfo != null) {
          if (dataTypeParameterInfo.isClass()) {
            result = Delegation.CLASS;
          }
          else if (dataTypeParameterInfo.isDataType()) {
            result = dataTypeParameterInfo.isResourceType() ? Delegation.RESOURCE_OR_DATA_TYPE : Delegation.DATA_TYPE;
          }
          else if (dataTypeParameterInfo.isResourceType()) {
            result = Delegation.RESOURCE_TYPE;
          }
          else {
            result = Delegation.NONE;
          }
        }
        else {
          result = getLastFqnPartDelegation(element, fullQualifiedName);
        }
      }
      else {
        result = Delegation.NAMESPACE;
      }

      if (result != Delegation.NONE) {
        processDelegation(element, references, range, fullQualifiedName, result);
      }
    }
    return references.toArray(PsiReference.EMPTY_ARRAY);
  }

  private static void processDelegation(@NotNull PsiElement element,
                                        List<PsiReference> references,
                                        TextRange range,
                                        String fullQualifiedName,
                                        Delegation delegation) {
    if (delegation == Delegation.CLASS) {
      references.add(new PuppetClassDefinitionReference(element, range, fullQualifiedName));
    }
    else if (delegation == Delegation.NAMESPACE) {
      references.add(new PuppetNamespaceReference(element, range, fullQualifiedName));
    }
    else if (delegation == Delegation.RESOURCE_TYPE) {
      references.add(new PuppetTypeDefinitionReference(element, range, fullQualifiedName));
    }
    else if (delegation == Delegation.RESOURCE_OR_DATA_TYPE) {
      references.add(new PuppetTypeDefinitionReference(element, range, fullQualifiedName, true));
    }
    else if (delegation == Delegation.FUNCTION) {
      references.add(new PuppetFunctionReference(element, range, fullQualifiedName));
    }
    else if (delegation == Delegation.CLASS_PARAMETER) {
      references.add(new PuppetClassParameterReference(element));
    }
    else if (delegation == Delegation.TYPE_PARAMETER) {
      references.add(new PuppetTypeParameterReference(element));
    }
    else if (delegation == Delegation.DATA_TYPE) {
      references.add(new PuppetDataTypeReference(element, range, fullQualifiedName));
    }
    else if (delegation == Delegation.VARIABLE) {
      references.add(new PuppetVariableReference(((PuppetVariable)element), range));
    }
  }


  /**
   * @return true if element should be treated as full-qualified identifier
   */
  protected boolean isFullQualifiedElement(@NotNull PsiElement element) {
    return true;
  }

  /**
   * Checks if we are in type parameter and returns parameter info from DataType
   *
   * @param element argument element
   * @return PuppetDataTypeParameterInfo or null if unavailable
   */
  protected @Nullable PuppetDataTypeParameterInfo getTypeParameterInfo(@NotNull PsiElement element) {
    if (!DATA_TYPE_PARAMETER_PATTERN.accepts(element)) { // Some::Arbitrary::Type[element]
      return null;
    }

    PuppetDataType containingType = PsiTreeUtil.getParentOfType(element, PuppetDataType.class);
    return containingType == null ? null : containingType.getParameterInfo(element);
  }

  /**
   * Calculates reference delegation for last fqn part of element
   * @param element element in question
   * @param fullQualifiedName fqn (need for types)
   * @return delegation
   */
  protected abstract Delegation getLastFqnPartDelegation(@NotNull PsiElement element, String fullQualifiedName);
}
