package com.intellij.seam.pageflow.model.xml.converters;

import com.intellij.seam.pageflow.model.xml.PageflowDomModelManager;
import com.intellij.seam.pageflow.model.xml.PageflowModel;
import com.intellij.seam.pageflow.model.xml.pageflow.PageElements;
import com.intellij.seam.pageflow.model.xml.pageflow.PageflowDefinition;
import com.intellij.util.xml.ConvertContext;
import com.intellij.util.xml.ResolvingConverter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PageflowPageElementsConverter extends ResolvingConverter<PageElements> {

  @Override
  @NotNull
  public Collection<? extends PageElements> getVariants(final ConvertContext context) {
    return getAllPageflowNamedElements(getPageflowDefinition(context));
  }

  @Override
  public PageElements fromString(@Nullable @NonNls final String s, final ConvertContext context) {
    if (s == null) return null;

    final PageflowDefinition pageflowDefinition = getPageflowDefinition(context);

    for (PageElements namedElement : getAllPageflowNamedElements(pageflowDefinition)) {
      if (s.equals(namedElement.getName().getStringValue())) {
        return namedElement;
      }
    }

    return null;
  }

  @Nullable
  private static PageflowDefinition getPageflowDefinition(final ConvertContext context) {
    final PageflowModel model = PageflowDomModelManager.getInstance(context.getFile().getProject()).getPageflowModel(context.getFile());

    if (model == null || model.getRoots().size() != 1) return null;

    return model.getRoots().get(0).getRootElement();
  }

  private static List<PageElements> getAllPageflowNamedElements(@Nullable final PageflowDefinition pageflowDefinition) {
    List<PageElements> elements = new ArrayList<>();

    if (pageflowDefinition != null) {
      elements.addAll(pageflowDefinition.getPages());
      elements.add(pageflowDefinition.getStartPage());
    }

    return elements;
  }

  @Override
  public String toString(@Nullable final PageElements pageElements, final ConvertContext context) {
    return pageElements == null ? null : pageElements.getName().getStringValue();
  }
}
