package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DataInputRefConvertor extends ResolvingConverter<TDataInput> {
  @Override
  public TDataInput fromString(@Nullable @NonNls String s, ConvertContext context) {
    final List<TDataInput> inputs = getInputs(context);
    if (inputs == null) return null;
    for (TDataInput input : inputs) {
      if (input.getId().isValid() && Objects.equals(input.getId().getValue(), s)) {
        return input;
      }
    }
    return null;
  }

  @Override
  public String toString(@Nullable TDataInput input, ConvertContext context) {
    if (input == null) return null;
    return input.getId().getStringValue();
  }

  @NotNull
  @Override
  public Collection<TDataInput> getVariants(ConvertContext context) {
    final List<TDataInput> inputs = getInputs(context);
    return inputs == null ? Collections.emptyList() : inputs;
  }

  public static List<TDataInput> getInputs(ConvertContext context) {
    final DomElement element = context.getInvocationElement();
    final DomElement inputSet = element.getParent();
    if (inputSet == null ||
        !(TOutputSet.class.equals(inputSet.getDomElementType()) || (TInputSet.class.equals(inputSet.getDomElementType())))) {
      return null;
    }
    final DomElement ioSpecDomElement = inputSet.getParent();
    if (ioSpecDomElement == null || !TInputOutputSpecification.class.equals(ioSpecDomElement.getDomElementType())) return null;

    return ((TInputOutputSpecification)ioSpecDomElement).getDataInputs();
  }
}
