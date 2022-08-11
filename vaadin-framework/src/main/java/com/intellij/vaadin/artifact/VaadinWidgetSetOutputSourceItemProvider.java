package com.intellij.vaadin.artifact;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.gwt.packaging.GwtCompilerOutputElement;
import com.intellij.gwt.packaging.GwtCompilerOutputElementType;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.roots.ui.configuration.artifacts.sourceItems.FacetBasedPackagingSourceItemsProvider;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementWeights;
import com.intellij.packaging.ui.TreeNodePresentation;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.vaadin.framework.VaadinConstants;
import com.intellij.vaadin.VaadinIcons;
import org.jetbrains.annotations.NotNull;

public class VaadinWidgetSetOutputSourceItemProvider extends FacetBasedPackagingSourceItemsProvider<GwtFacet, GwtCompilerOutputElement> {
  public VaadinWidgetSetOutputSourceItemProvider() {
    super(GwtFacetType.ID, GwtCompilerOutputElementType.getInstance());
  }

  @Override
  protected TreeNodePresentation createPresentation(GwtFacet facet) {
    return new VaadinSourceItemPresentation(facet);
  }

  @Override
  protected PackagingElement<?> createElement(ArtifactEditorContext context, GwtFacet facet) {
    GwtCompilerOutputElement compilerOutputElement = new GwtCompilerOutputElement(context.getProject(), facet);
    return PackagingElementFactory.getInstance().createParentDirectories(VaadinConstants.VAADIN_WIDGET_SETS_PATH, compilerOutputElement);
  }

  @Override
  protected GwtFacet getFacet(GwtCompilerOutputElement element) {
    return element.getFacet();
  }

  private static class VaadinSourceItemPresentation extends TreeNodePresentation {
    private final GwtFacet myFacet;

    VaadinSourceItemPresentation(GwtFacet facet) {
      myFacet = facet;
    }

    @Override
    public String getPresentableName() {
      return "'" + myFacet.getModule().getName() + "' Vaadin WidgetSet compile output";
    }

    @Override
    public void render(@NotNull PresentationData presentationData,
                       SimpleTextAttributes mainAttributes,
                       SimpleTextAttributes commentAttributes) {
      presentationData.setIcon(VaadinIcons.VaadinIcon);
      presentationData.addText(getPresentableName(), mainAttributes);
    }

    @Override
    public int getWeight() {
      return PackagingElementWeights.FACET;
    }
  }
}
