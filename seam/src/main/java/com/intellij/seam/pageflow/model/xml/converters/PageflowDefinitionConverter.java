package com.intellij.seam.pageflow.model.xml.converters;

import com.intellij.openapi.module.Module;
import com.intellij.seam.pageflow.model.xml.PageflowDomModelManager;
import com.intellij.seam.pageflow.model.xml.PageflowModel;
import com.intellij.seam.pageflow.model.xml.pageflow.PageflowDefinition;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class PageflowDefinitionConverter extends ResolvingConverter<PageflowDefinition> {

  @Override
  @NotNull
  public Collection<PageflowDefinition> getVariants(final ConvertContext context) {
    return getPageflowDefinitions(context);
  }

  @Override
  public PageflowDefinition fromString(@Nullable @NonNls final String s, final ConvertContext context) {
    if (s == null) return null;

    final List<PageflowDefinition> pageflowDefinitions = getPageflowDefinitions(context);
    for (PageflowDefinition definition : pageflowDefinitions) {
      if (s.equals(definition.getName().getStringValue())) {
        return definition;
      }
    }

    return null;
  }

  @NotNull
  private static List<PageflowDefinition> getPageflowDefinitions(final ConvertContext context) {
    final Module module = context.getModule();
    final List<PageflowModel> models = PageflowDomModelManager.getInstance(module.getProject()).getAllModels(module);
    return ContainerUtil.map2List(models, pageflowModel -> pageflowModel.getRoots().get(0).getRootElement());
  }

  @Override
  public String toString(@Nullable final PageflowDefinition pageflowDefinition, final ConvertContext context) {
    return pageflowDefinition == null ? null : pageflowDefinition.getName().getStringValue();
  }
}
