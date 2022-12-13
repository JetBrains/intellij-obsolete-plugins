package com.intellij.dmserver.artifacts.plan;

import com.intellij.util.xml.GenericAttributeValue;

import java.util.List;

public interface PlanRootElement extends PlanElementBase {

  List<PlanArtifactElement> getArtifacts();

  PlanArtifactElement addArtifact();

  GenericAttributeValue<String> getName();

  GenericAttributeValue<Boolean> getScoped();

  GenericAttributeValue<Boolean> getAtomic();

  GenericAttributeValue<String> getVersion();
}
