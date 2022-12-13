package com.intellij.seam.structure;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.javaee.module.view.nodes.JavaeeNodeDescriptor;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.HtmlBuilder;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.psi.PsiType;
import com.intellij.seam.model.SeamComponentScope;
import com.intellij.seam.model.xml.components.SeamDomComponent;
import com.intellij.seam.resources.SeamBundle;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.seam.SeamIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SeamDomComponentNodeDescriptor extends JavaeeNodeDescriptor<SeamDomComponent> {
  public SeamDomComponentNodeDescriptor(final Project project,
                                        final NodeDescriptor parentDescriptor,
                                        final Object parameters,
                                        final SeamDomComponent element) {
    super(project, parentDescriptor, parameters, element);
  }

  @Override
  protected String getNewNodeText() {
    SeamDomComponent seamDomComponent = getJamElement();
    if (!seamDomComponent.isValid()) return "";

    return seamDomComponent.getComponentName();
  }

  @Override
  protected Icon getNewIcon() {
    return SeamIcons.Seam;
  }

  @Override
  public boolean isValid() {
    return getJamElement().isValid();
  }

  @Override
  protected void doUpdate() {
    super.doUpdate();
    final String textExt = getNewNodeTextExt();
    if (textExt != null) {
      addColoredFragment(" (" + getNewNodeTextExt() + ")", SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES);
    }
  }

  @Nullable
  protected String getNewNodeTextExt() {
    if (!isValid()) return null;

    SeamDomComponent seamDomComponent = getJamElement();
    PsiType psiType = seamDomComponent.getComponentType();

    return psiType == null ? null : psiType.getPresentableText();
  }

  @Override
  public Object getData(@NotNull final String dataId) {
    if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
      return getJamElement().getIdentifyingPsiElement();
    }

    return super.getData(dataId);
  }

  @Override
  public String getNewTooltip() {
    if (!isValid()) return null;

    SeamDomComponent seamDomComponent = getJamElement();

    HtmlBuilder htmlBuilder = new HtmlBuilder();

    String name = seamDomComponent.getComponentName();
    PsiType psiType = seamDomComponent.getComponentType();
    SeamComponentScope scope = seamDomComponent.getComponentScope();

    String title = SeamBundle.message("seam.dom.tooltip.name");
    htmlBuilder.append(row(title, name));
    if (psiType != null) {
      htmlBuilder.append(row(SeamBundle.message("seam.dom.tooltip.class"), psiType.getCanonicalText()));
    }

    if (scope != null) {
      htmlBuilder.append(row(SeamBundle.message("seam.dom.tooltip.scope"), scope.getValue()));
    }

    if (!htmlBuilder.isEmpty()) {
      return htmlBuilder.wrapWith("table").wrapWith("html").toString();
    }

    return "";
  }

  @NotNull
  private static HtmlChunk.Element row(@Nls String title, @Nls String value) {
    return HtmlChunk.tag("tr").children(
      HtmlChunk.text(title).wrapWith("strong").wrapWith("td"),
      HtmlChunk.text(value).wrapWith("td"));
  }
}
