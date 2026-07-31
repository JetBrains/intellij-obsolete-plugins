package com.intellij.gwt.uiBinder;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.ArrayUtilRt;
import com.intellij.xml.impl.BasicXmlAttributeDescriptor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class GwtXmlAttributeDescriptorBase extends BasicXmlAttributeDescriptor {
  private static final String[] BOOLEAN_VALUES = {Boolean.TRUE.toString(), Boolean.FALSE.toString()};
  private final String[] myValues;
  private final boolean myRequired;

  public GwtXmlAttributeDescriptorBase(@Nullable PsiType type, boolean required) {
    if (TypeConversionUtil.isBooleanType(type)) {
      myValues = BOOLEAN_VALUES;
    }
    else if (type instanceof PsiClassType) {
      final PsiClass psiClass = ((PsiClassType)type).resolve();
      if (psiClass != null && psiClass.isEnum()) {
        List<String> values = new ArrayList<>();
        for (PsiField field : psiClass.getFields()) {
          if (field instanceof PsiEnumConstant) {
            values.add(field.getName());
          }
        }
        myValues = ArrayUtilRt.toStringArray(values);
      }
      else {
        myValues = ArrayUtilRt.EMPTY_STRING_ARRAY;
      }
    }
    else {
      myValues = ArrayUtilRt.EMPTY_STRING_ARRAY;
    }
    myRequired = required;
  }

  @Override
  public boolean isRequired() {
    return myRequired;
  }

  @Override
  public boolean isFixed() {
    return false;
  }

  @Override
  public boolean hasIdType() {
    return false;
  }

  @Override
  public boolean hasIdRefType() {
    return false;
  }

  @Override
  public String getDefaultValue() {
    return null;
  }

  @Override
  public String[] getEnumeratedValues() {
    return myValues;
  }

  @Override
  public void init(PsiElement element) {
  }

  @Override
  public boolean isEnumerated() {
    return myValues.length > 0;
  }
}
