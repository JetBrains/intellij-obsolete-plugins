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

public class DataOutputRefConvertor extends ResolvingConverter<TDataOutput> {
  @NotNull
  @Override
  public Collection<TDataOutput> getVariants(ConvertContext context) {
    final Collection<TDataOutput> variants = getOutputs(context);
    return variants == null ? Collections.emptyList() : variants;
  }

  @Override
  public TDataOutput fromString(@Nullable @NonNls String s, ConvertContext context) {
    final List<TDataOutput> outputs = getOutputs(context);
    if (outputs == null) return null;
    for (TDataOutput output : outputs) {
      if (output.getId().isValid() && Objects.equals(output.getId().getValue(), s)) {
        return output;
      }
    }
    return null;
  }

  @Override
  public String toString(@Nullable TDataOutput output, ConvertContext context) {
    return output == null ? null : output.getId().getStringValue();
  }

  public static List<TDataOutput> getOutputs(ConvertContext context) {
    final DomElement element = context.getInvocationElement();
    final DomElement inputSet = element.getParent();
    if (inputSet == null ||
        !(TOutputSet.class.equals(inputSet.getDomElementType()) || (TInputSet.class.equals(inputSet.getDomElementType())))) {
      return null;
    }
    final DomElement ioSpecDomElement = inputSet.getParent();
    if (ioSpecDomElement == null || !TInputOutputSpecification.class.equals(ioSpecDomElement.getDomElementType())) return null;

    return ((TInputOutputSpecification)ioSpecDomElement).getDataOutputs();
  }
}
