package com.intellij.seam.dependencies.beans;

import com.intellij.seam.SeamIcons;
import com.intellij.seam.model.jam.bijection.SeamJamBijection;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class  UnknownBijectionNodeInfo implements SeamComponentNodeInfo<SeamJamBijection>{
  private final SeamJamBijection myBijection;

  public UnknownBijectionNodeInfo(final SeamJamBijection bijection) {
    myBijection = bijection;
  }

  @Override
  public String getName() {
    return myBijection.getName();
  }

  @Override
  public Icon getIcon() {
    return SeamIcons.Seam;
  }

  @Override
  @NotNull
  public SeamJamBijection getIdentifyingElement() {
    return myBijection;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final UnknownBijectionNodeInfo that = (UnknownBijectionNodeInfo)o;

    if (myBijection != null && myBijection.isValid() && that.myBijection.isValid() ? !myBijection.equals(that.myBijection) : that.myBijection != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return (myBijection != null && myBijection.isValid() ? myBijection.hashCode() : 0);
  }
}
