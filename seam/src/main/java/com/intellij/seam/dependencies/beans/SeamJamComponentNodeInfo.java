package com.intellij.seam.dependencies.beans;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.seam.SeamIcons;
import com.intellij.seam.model.jam.SeamJamComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class  SeamJamComponentNodeInfo implements SeamComponentNodeInfo<SeamJamComponent> {
  private final SeamJamComponent myJamComponent;

  public SeamJamComponentNodeInfo(final SeamJamComponent jamComponent) {

    myJamComponent = jamComponent;
  }

  @Override
  public String getName() {
    final String name = myJamComponent.getComponentName();
    return StringUtil.isEmptyOrSpaces(name) ? "noname" : name;
  }

  @Override
  public Icon getIcon() {
    return SeamIcons.Seam;
  }

  @Override
  @NotNull
  public SeamJamComponent getIdentifyingElement() {
    return myJamComponent;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final SeamJamComponentNodeInfo that = (SeamJamComponentNodeInfo)o;

    if (myJamComponent != null && myJamComponent.isValid() && that.myJamComponent.isValid()? !myJamComponent.equals(that.myJamComponent) : that.myJamComponent != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return (myJamComponent != null && myJamComponent.isValid() ? myJamComponent.hashCode() : 0);
  }
}
