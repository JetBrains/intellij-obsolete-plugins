package com.intellij.seam.pages.xml.pages;

import com.intellij.openapi.paths.PathReference;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Namespace;
import com.intellij.util.xml.SubTag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Namespace(SeamNamespaceConstants.PAGES_NAMESPACE_KEY)
public interface Pages extends SeamPagesDomElement {

  @NotNull
  GenericAttributeValue<PathReference> getNoConversationViewId();

  @NotNull
  GenericAttributeValue<PathReference> getLoginViewId();

  @NotNull
  @SubTag("exception")
  List<PagesException> getExceptions();

  PagesException addException();

  @NotNull
  List<Conversation> getConversations();

  Conversation addConversation();

  @NotNull
  List<Page> getPages();

  Page addPage();
}
