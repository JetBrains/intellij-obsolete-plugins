package com.intellij.struts.highlighting;

import com.intellij.struts.dom.StrutsConfig;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.BasicDomElementsInspection;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class StrutsInspection extends BasicDomElementsInspection<StrutsConfig> {

  public StrutsInspection() {
    super(StrutsConfig.class);
  }

  @NotNull
  @Override
  public String[] getGroupPath() {
    return new String[]{"Struts", getGroupDisplayName()};
  }

  @Override
  protected boolean shouldCheckResolveProblems(final GenericDomValue value) {
    final String stringValue = value.getStringValue();
    if (stringValue != null) {
      return stringValue.indexOf('{') < 0;
    }
    return true;
  }

  @Override
  @NotNull
  @NonNls
  public String getShortName() {
    return "StrutsInspection";
  }
}