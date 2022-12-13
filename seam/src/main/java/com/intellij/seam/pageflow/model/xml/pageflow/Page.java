package com.intellij.seam.pageflow.model.xml.pageflow;


import com.intellij.ide.presentation.Presentation;
import com.intellij.openapi.paths.PathReference;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

@Presentation(icon = "SeamPageflowIcons.Page", typeName = Page.PAGE)
public interface Page extends SeamPageflowDomElement, PageElements {

  String PAGE = "Page";

  @NotNull
  @Attribute("redirect")
  GenericAttributeValue<Boolean> getRedirectAttr();

  @NotNull
  GenericAttributeValue<Enabled> getSwitch();

  @NotNull
  GenericAttributeValue<PathReference> getNoConversationViewId();

  @NotNull
  GenericAttributeValue<Integer> getTimeout();

  @NotNull
  GenericAttributeValue<Enabled> getBack();

  @NotNull
  EndConversation getEndConversation();

  @NotNull
  EndTask getEndTask();
}
