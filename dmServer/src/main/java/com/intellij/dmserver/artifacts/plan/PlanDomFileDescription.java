package com.intellij.dmserver.artifacts.plan;

import com.intellij.util.xml.DomFileDescription;

public class PlanDomFileDescription extends DomFileDescription<PlanRootElement> {

  public PlanDomFileDescription() {
    super(PlanRootElement.class, PlanFileManager.PLAN_EXTENSION);
  }
}
