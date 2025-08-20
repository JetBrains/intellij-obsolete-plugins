package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class OptionalAndWhileExecutingInputRefConvertor extends ResolvingConverter<TDataInput> {
  @NotNull
  @Override
  public Collection<TDataInput> getVariants(ConvertContext context) {
    final List<TDataInput> variantsImpl = getVariantsImpl(context);
    return variantsImpl == null ? Collections.emptyList() : variantsImpl;
  }

  private List<TDataInput> getVariantsImpl(ConvertContext context) {
    final DomElement element = context.getInvocationElement();
    final DomElement inputSet = element.getParent();
    if (inputSet == null || !TInputSet.class.equals(inputSet.getDomElementType())) return null;
    final DomElement ioSpecDomElement = inputSet.getParent();
    if (ioSpecDomElement == null || !TInputOutputSpecification.class.equals(ioSpecDomElement.getDomElementType())) return null;

    //final List<TDataInput> inputs = ((TInputOutputSpecification) ioSpecDomElement).getDataInputs();

    final List<TDataInput> result = new ArrayList<>();
    final List<GenericDomValue<TDataInput>> inputRefses = ((TInputSet)inputSet).getDataInputRefses();
    for (GenericDomValue<TDataInput> refse : inputRefses) {
      if (refse.isValid()) {
        final TDataInput value = refse.getValue();
        if (value != null && value.isValid()) {
          result.add(value);
        }
      }
    }
    return result;
  }

  @Override
  public TDataInput fromString(@Nullable @NonNls String s, ConvertContext context) {
    final List<TDataInput> variantsImpl = getVariantsImpl(context);
    if (variantsImpl == null) return null;
    for (TDataInput input : variantsImpl) {
      if (input.getId().isValid() && Objects.equals(input.getId().getValue(), s)) {
        return input;
      }
    }
    return null;
  }

  @Override
  public String toString(@Nullable TDataInput input, ConvertContext context) {
    return input == null ? null : input.getId().getStringValue();
  }
}
