package com.intellij.seam.pageflow.model.xml.pageflow;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ActionsOwner extends SeamPageflowDomElement {
  @NotNull
  List<Action> getActions();

  Action addAction();
}
