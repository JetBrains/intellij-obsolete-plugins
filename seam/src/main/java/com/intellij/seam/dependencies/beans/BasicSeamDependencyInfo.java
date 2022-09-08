package com.intellij.seam.dependencies.beans;

import com.intellij.jam.model.common.CommonModelElement;
import org.jetbrains.annotations.NotNull;

public class  BasicSeamDependencyInfo implements SeamDependencyInfo<CommonModelElement> {
  private final SeamComponentNodeInfo mySource;
  private final SeamComponentNodeInfo myTarget;
  private final String myName;
  private final CommonModelElement myIdentifyingElement;

  public BasicSeamDependencyInfo(final SeamComponentNodeInfo source,
                                    final SeamComponentNodeInfo target,
                                    final String name,
                                    final CommonModelElement identifyingElement) {
    mySource = source;
    myTarget = target;
    myName = name;
    myIdentifyingElement = identifyingElement;
  }

  @Override
  public SeamComponentNodeInfo getSource() {
    return mySource;
  }

  @Override
  public SeamComponentNodeInfo getTarget() {
    return myTarget;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  @NotNull
  public CommonModelElement getIdentifyingElement() {
    return myIdentifyingElement;
  }
}
