package com.intellij.dmserver.artifacts.plan;

import com.intellij.dmserver.artifacts.DMPlanArtifactType;
import com.intellij.dmserver.facet.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ArtifactNameConverter extends ResolvingConverter.StringConverter {

  @NotNull
  @Override
  public Collection<String> getVariants(ConvertContext context) {
    PlanArtifactElement artifactElement = (PlanArtifactElement)context.getInvocationElement().getParent();
    if (artifactElement == null) {
      return Collections.emptyList();
    }
    DMArtifactElementType artifactElementType = artifactElement.getType().getValue();

    Module module = context.getModule();

    DMCompositeFacet compositeFacet = DMCompositeFacet.getInstance(module);
    if (compositeFacet == null) {
      return Collections.emptyList();
    }
    DMCompositeFacetConfiguration compositeConfiguration = compositeFacet.getConfigurationImpl();
    if (compositeConfiguration.getCompositeType() != DMCompositeType.PLAN) {
      return Collections.emptyList();
    }

    NestedUnitProvider unitProvider = new NestedUnitProvider(module, new DefaultModulesProvider(context.getProject()));

    List<String> result = new ArrayList<>();
    for (Module possibleNestedModule : unitProvider.getPossibleNestedModules(compositeConfiguration)) {
      DMUnitDescriptor unitDescriptor = DMUnitDescriptorProvider.getInstance().processModule(possibleNestedModule);
      if (unitDescriptor == null) {
        continue;
      }
      if (artifactElementType == null
          || artifactElementType.equals(DMPlanArtifactType.getElementType4UnitType(unitDescriptor.getType()))) {
        result.add(unitDescriptor.getSymbolicName());
      }
    }

    return result;
  }
}
