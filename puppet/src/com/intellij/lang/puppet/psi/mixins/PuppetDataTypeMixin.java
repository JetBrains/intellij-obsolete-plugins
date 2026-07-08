package com.intellij.lang.puppet.psi.mixins;

import com.intellij.lang.ASTNode;
import com.intellij.lang.puppet.psi.PsiPuppetDataTypeParametersBlock;
import com.intellij.lang.puppet.psi.PuppetDataType;
import com.intellij.lang.puppet.psi.PuppetDataTypeParameterInfo;
import com.intellij.lang.puppet.psi.PuppetTypeHolder;
import com.intellij.lang.puppet.psi.impl.PuppetCompositePsiElementBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.intellij.lang.puppet.psi.PuppetDataTypes.ALL_LOWERCASED_DATA_TYPES;
import static com.intellij.lang.puppet.psi.PuppetDataTypes.CLASS_TYPE_NAME;
import static com.intellij.lang.puppet.psi.PuppetDataTypes.RESOURCE_TYPE_NAME;
import static com.intellij.lang.puppet.psi.PuppetDataTypes.TYPE_TYPE_NAME;
import static com.intellij.lang.puppet.psi.PuppetDataTypes.VARIANT_TYPE_NAME;

public abstract class PuppetDataTypeMixin extends PuppetCompositePsiElementBase implements PuppetDataType {
  private static final Set<String> TYPES_DELEGATING_TO_FIRST_PARAMETER =
    Set.of(LOWERCASE_TYPE_TYPE_NAME, LOWERCASE_RESOURCE_TYPE_NAME);

  public PuppetDataTypeMixin(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public @Nullable String getEffectiveTypeName() {
    String qualifyingTypeName = getQualifyingTypeName();
    if (StringUtil.isEmpty(qualifyingTypeName)) {
      return null;
    }

    List<PsiElement> list = getParametersList();
    if (list == null) {
      return qualifyingTypeName;
    }

    if (list.isEmpty()) {
      return null;
    }

    PsiElement firstParameter = list.get(0);

    assert !StringUtil.hasUpperCaseChar(qualifyingTypeName);

    if (TYPES_DELEGATING_TO_FIRST_PARAMETER.contains(qualifyingTypeName) && firstParameter instanceof PuppetTypeHolder) {
      return ((PuppetTypeHolder)firstParameter).getEffectiveTypeName();
    }

    return qualifyingTypeName;
  }

  @Override
  public @Nullable String getQualifyingTypeName() {
    PsiElement child = getFirstChild();
    return child == null ? null : StringUtil.toLowerCase(child.getText());
  }

  @Override
  public @Nullable List<PsiElement> getParametersList() {
    PsiPuppetDataTypeParametersBlock block = getDataTypeParametersBlock();
    return block == null ? null : Arrays.asList(block.getChildren());
  }

  @Override
  public @Nullable String getEffectiveClassName() {
    String typeName = getQualifyingTypeName();
    if (!StringUtil.equals(LOWERCASE_CLASS_TYPE_NAME, typeName)) {
      return null;
    }

    PsiElement firstParameter = getParameterElement(0);

    return firstParameter instanceof PuppetTypeHolder ? ((PuppetTypeHolder)firstParameter).getEffectiveTypeName() : null;
  }

  @Override
  public @Nullable PsiElement getParameterElement(int parameterIndex) {
    List<PsiElement> parametersList = getParametersList();
    if (parametersList == null || parameterIndex < 0 || parametersList.size() - 1 < parameterIndex) {
      return null;
    }

    return parametersList.get(parameterIndex);
  }

  @Override
  public @Nullable PuppetDataTypeParameterInfo getParameterInfo(@NotNull PsiElement parameterElement) {
    List<PsiElement> list = getParametersList();
    if (list == null || list.isEmpty()) {
      return null;
    }

    int parameterIndex = list.lastIndexOf(parameterElement);
    if (parameterIndex == -1) {
      return null;
    }

    String typeName = getQualifyingTypeName();
    if (StringUtil.equalsIgnoreCase(typeName, RESOURCE_TYPE_NAME)) {
      if (parameterIndex == 0) {
        return PuppetDataTypeParameterInfo.forResourceType();
      }
      PsiElement typeSpecifier = getParameterElement(0);
      if (typeSpecifier instanceof PuppetTypeHolder) {
        return PuppetDataTypeParameterInfo.forResourceInstance(((PuppetTypeHolder)typeSpecifier).getEffectiveTypeName());
      }
    }
    else if (StringUtil.equalsIgnoreCase(typeName, CLASS_TYPE_NAME)) {
      return PuppetDataTypeParameterInfo.forClass();
    }
    else if (StringUtil.equalsIgnoreCase(typeName, TYPE_TYPE_NAME)) {
      return PuppetDataTypeParameterInfo.forAnyType();
    }
    else if (StringUtil.equalsIgnoreCase(typeName, VARIANT_TYPE_NAME)) {
      return PuppetDataTypeParameterInfo.forAnyType();
    }
    else if (!ALL_LOWERCASED_DATA_TYPES.contains(typeName)) {
      return PuppetDataTypeParameterInfo.forResourceInstance(getEffectiveTypeName());
    }

    return null;
  }
}
