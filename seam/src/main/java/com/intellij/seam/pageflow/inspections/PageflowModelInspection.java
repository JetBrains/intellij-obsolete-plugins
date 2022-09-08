package com.intellij.seam.pageflow.inspections;

import com.intellij.seam.pageflow.model.xml.pageflow.PageflowDefinition;
import com.intellij.seam.resources.SeamInspectionBundle;
import com.intellij.util.xml.highlighting.BasicDomElementsInspection;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class PageflowModelInspection extends BasicDomElementsInspection<PageflowDefinition> {

  public PageflowModelInspection() {
    super(PageflowDefinition.class);
  }

  @Override
  @NotNull
  public String getGroupDisplayName() {
    return SeamInspectionBundle.message("model.inspection.group.name");
  }

  @Override
  @NotNull
  @NonNls
  public String getShortName() {
    return "PageflowModelInspection";
  }
}
