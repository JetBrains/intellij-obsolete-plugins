package com.intellij.gwt.packaging;

import com.intellij.facet.pointers.FacetPointer;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.icons.GwtIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.packaging.ui.PackagingElementWeights;
import com.intellij.packaging.ui.TreeNodePresentation;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

public final class GwtCompilerOutputNodePresentation extends TreeNodePresentation {
  private final FacetPointer<GwtFacet> myFacetPointer;
  private final String myOutputName;

  private GwtCompilerOutputNodePresentation(FacetPointer<GwtFacet> facetPointer, final String outputName) {
    myFacetPointer = facetPointer;
    myOutputName = outputName;
  }

  @Override
  public String getPresentableName() {
    final String moduleName = myFacetPointer != null ? myFacetPointer.getModuleName() : "<unknown>";
    return "'" + moduleName + "' " + myOutputName;
  }

  @Override
  public void render(@NotNull PresentationData presentationData, SimpleTextAttributes mainAttributes, SimpleTextAttributes commentAttributes) {
    presentationData.setIcon(GwtIcons.GoogleSmall);
    presentationData.addText(getPresentableName(), mainAttributes);
  }

  @Override
  public int getWeight() {
    return PackagingElementWeights.FACET;
  }

  public static GwtCompilerOutputNodePresentation createDeployPresentation(FacetPointer<GwtFacet> facetPointer) {
    return new GwtCompilerOutputNodePresentation(facetPointer, GwtBundle.message("artifact.node.gwt.deploy.output"));
  }

  public static GwtCompilerOutputNodePresentation createOutputPresentation(FacetPointer<GwtFacet> facetPointer) {
    return new GwtCompilerOutputNodePresentation(facetPointer, GwtBundle.message("artifact.node.gwt.compiler.output"));
  }
}
