package com.intellij.seam.pageflow;

import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.seam.pageflow.model.xml.pageflow.PageflowDefinition;
import com.intellij.util.xml.DomFileDescription;

public class PageflowDomFileDescription extends DomFileDescription<PageflowDefinition> {

  public PageflowDomFileDescription() {
    super(PageflowDefinition.class, "pageflow-definition");
  }

  @Override
  protected void initializeFileDescription() {
    registerNamespacePolicy(SeamNamespaceConstants.PAGEFLOW_NAMESPACE_KEY, SeamNamespaceConstants.PAGEFLOW_NAMESPACE);
  }


}
