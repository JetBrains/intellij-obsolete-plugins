// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.references.constraints;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;

import java.util.Map;

import static org.jetbrains.plugins.grails.references.constraints.GrailsConstraintNamedArgumentProvider.DESCRIPTORS;
import static org.jetbrains.plugins.grails.references.constraints.GrailsConstraintNamedArgumentProvider.Descriptor;
import static org.jetbrains.plugins.grails.references.constraints.GrailsConstraintNamedArgumentProvider.MinMaxArgumentDescriptor;
import static org.jetbrains.plugins.grails.references.constraints.GrailsConstraintNamedArgumentProvider.MyArgumentDescriptor;
import static org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor.SIMPLE_ON_TOP;

public class GrailsConstraintGroupNamedArgumentProvider extends GroovyNamedArgumentProvider {

  @Override
  public void getNamedArguments(@NotNull GrCall call,
                                @NotNull GroovyResolveResult resolveResult,
                                @Nullable String argumentName,
                                boolean forCompletion,
                                @NotNull Map<String, NamedArgumentDescriptor> result) {
    PsiElement resolved = resolveResult.getElement();
    if (resolved == null) return;

    for (final Map.Entry<String, Descriptor> entry : DESCRIPTORS.entrySet()) {
      if (argumentName != null && !argumentName.equals(entry.getKey())) continue;

      String name = entry.getKey();

      if ("unique".equals(name)) continue;

      Object argumentDescriptorMarker = entry.getValue().marker();

      NamedArgumentDescriptor argumentDescriptor;

      if (argumentDescriptorMarker == MinMaxArgumentDescriptor.class) {
        argumentDescriptor = SIMPLE_ON_TOP;
      }
      else {
        argumentDescriptor = (NamedArgumentDescriptor)argumentDescriptorMarker;
      }

      result.put(name, new MyArgumentDescriptor(entry.getValue().constraintFn().apply(call), argumentDescriptor, resolved));
    }
  }
}
