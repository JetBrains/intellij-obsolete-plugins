package com.intellij.seam.pageflow.model.xml.pageflow;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface PageflowTransitionHolder extends SeamPageflowDomElement {
    @NotNull
    List<Transition> getTransitions();

    Transition addTransition();
}
