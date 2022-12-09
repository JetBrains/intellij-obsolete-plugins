package com.intellij.dmserver.artifacts.plan;

import com.intellij.util.xml.NamedEnum;
import org.jetbrains.annotations.NonNls;

public enum DMArtifactElementType implements NamedEnum {

  TYPE_BUNDLE("bundle"),
  TYPE_PAR("par"),
  TYPE_PLAN("plan"),
  TYPE_CONFIG("configuration");

  private final String myValue;

  DMArtifactElementType(@NonNls String value) {
    myValue = value;
  }

  @Override
  public String getValue() {
    return myValue;
  }
}
