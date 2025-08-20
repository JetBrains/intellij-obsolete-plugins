package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class OptionalAndWhileExecutingOutputRefConvertor extends ResolvingConverter<TDataOutput> {
  @NotNull
  @Override
  public Collection<TDataOutput> getVariants(ConvertContext context) {
    final List<TDataOutput> impl = getVariantsImpl(context);
    return impl == null ? Collections.emptyList() : impl;
  }

  private List<TDataOutput> getVariantsImpl(ConvertContext context) {
    final DomElement element = context.getInvocationElement();
    final DomElement outputSet = element.getParent();
    if (outputSet == null || !TOutputSet.class.equals(outputSet.getDomElementType())) return null;
    final DomElement ioSpecDomElement = outputSet.getParent();
    if (ioSpecDomElement == null || !TInputOutputSpecification.class.equals(ioSpecDomElement.getDomElementType())) return null;

    //final List<TDataInput> inputs = ((TInputOutputSpecification) ioSpecDomElement).getDataInputs();

    final List<TDataOutput> result = new ArrayList<>();
    final List<GenericDomValue<TDataOutput>> inputRefses = ((TOutputSet)outputSet).getDataOutputRefses();
    for (GenericDomValue<TDataOutput> refse : inputRefses) {
      if (refse.isValid()) {
        final TDataOutput value = refse.getValue();
        if (value != null && value.isValid()) {
          result.add(value);
        }
      }
    }
    return result;
  }

  @Override
  public TDataOutput fromString(@Nullable @NonNls String s, ConvertContext context) {
    final List<TDataOutput> impl = getVariantsImpl(context);
    if (impl == null) return null;
    for (TDataOutput output : impl) {
      if (output.getId().isValid() && Objects.equals(output.getId().getStringValue(), s)) {
        return output;
      }
    }
    return null;
  }

  @Override
  public String toString(@Nullable TDataOutput output, ConvertContext context) {
    return output == null ? null : output.getId().getStringValue();
  }
}
