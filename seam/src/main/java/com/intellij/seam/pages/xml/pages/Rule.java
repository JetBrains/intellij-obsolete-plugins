package com.intellij.seam.pages.xml.pages;


import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Rule extends TaskOwner, RenderAndRedirectOwner, SeamPagesDomElement {

  @NotNull
  GenericAttributeValue<String> getIfOutcome();


  @NotNull
  GenericAttributeValue<String> getIf();


  @NotNull
  List<Out> getOuts();

  Out addOut();


  @NotNull
  RaiseEvent getRaiseEvent();


  @NotNull
  BeginConversation getBeginConversation();

  @NotNull
  EndConversation getEndConversation();

  @NotNull
  CreateProcess getCreateProcess();

  @NotNull
  ResumeProcess getResumeProcess();
}
