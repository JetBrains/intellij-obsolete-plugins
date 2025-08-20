package com.intellij.jboss.bpmn.jpdl.model.xml.converters;

import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

public class EventNameConverter extends ResolvingConverter<String> {

  @Override
  @NotNull
  public Collection<String> getVariants(final ConvertContext context) {
    //todo collect "custom events"
    return Arrays.asList("start", "end", "take", "timeout");
  }

  @Override
  public String fromString(@Nullable @NonNls final String s, final ConvertContext context) {
    return s;
  }

  @Override
  public String toString(@Nullable String s, ConvertContext convertContext) {
    return s;
  }
}