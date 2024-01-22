package com.intellij.jboss.bpmn.jbpm.model.converters;

import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.Converter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

public class DoubleConverter extends Converter<Double> {
  @Override
  public Double fromString(@Nullable @NonNls String s, ConvertContext context) {
    try {
      return Double.parseDouble(s);
    }
    catch (NumberFormatException e) {
      return null;
    }
  }

  @Override
  public String toString(@Nullable Double aDouble, ConvertContext context) {
    return aDouble == null ? null : Double.toString(aDouble);
  }
}
