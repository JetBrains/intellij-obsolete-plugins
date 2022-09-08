package com.intellij.seam.pages.graph.beans;

import com.intellij.psi.xml.XmlElement;
import com.intellij.seam.pages.xml.pages.PagesViewIdOwner;
import com.intellij.seam.pages.xml.pages.Rule;
import org.jetbrains.annotations.NotNull;

public class RuleEdge extends BasicPagesEdge<Rule> {
  public RuleEdge(@NotNull final BasicPagesNode source,
                  @NotNull final BasicPagesNode target,
                  @NotNull final PagesViewIdOwner viewID,
                  @NotNull final Rule parentElement) {
    super(source, target, viewID, parentElement);
  }

  @Override
  public String getName() {
    final XmlElement ifElement = getParentElement().getIf().getXmlElement();
    if (ifElement != null) return ifElement.getText();

    final XmlElement ifOutcomeElement = getParentElement().getIfOutcome().getXmlElement();
    if (ifOutcomeElement != null) return ifOutcomeElement.getText();

    return super.getName();
  }
}

