package com.intellij.seam.pages.xml.pages;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jsf.converters.model.AsteriskPathReferenceConverter;
import com.intellij.openapi.paths.PathReference;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Namespace(SeamNamespaceConstants.PAGES_NAMESPACE_KEY)
@Presentation(icon = "AllIcons.FileTypes.Any_type")
public interface Page extends SeamPagesDomElement {

  @NotNull
  GenericAttributeValue<String> getAction();

  @NotNull
  @NameValue(unique = false)
  @Convert(value = AsteriskPathReferenceConverter.class, soft = true)
  GenericAttributeValue<PathReference> getViewId();

  @NotNull
  GenericAttributeValue<Switch> getSwitch();

  @NotNull
  GenericAttributeValue<PathReference> getNoConversationViewId();

  @NotNull
  GenericAttributeValue<ConversationRequired> getConversationRequired();

  @NotNull
  GenericAttributeValue<LoginRequired> getLoginRequired();

  @NotNull
  GenericAttributeValue<String> getScheme();

  @NotNull
  GenericAttributeValue<Integer> getTimeout();

  @NotNull
  GenericAttributeValue<String> getBundle();

  @NotNull
  GenericAttributeValue<String> getConversation();

  @NotNull
  List<GenericDomValue<String>> getRestricts();

  GenericDomValue<String> addRestrict();

  @NotNull
  List<GenericDomValue<String>> getDescriptions();

  GenericDomValue<String> addDescription();

  @NotNull
  List<Param> getParams();

  Param addParam();

  @NotNull
  List<BeginConversation> getBeginConversations();

  BeginConversation addBeginConversation();


  @NotNull
  List<EndConversation> getEndConversations();

  EndConversation addEndConversation();


  @NotNull
  List<StartTask> getStartTasks();

  StartTask addStartTask();

  @NotNull
  List<BeginTask> getBeginTasks();

  BeginTask addBeginTask();

  @NotNull
  List<EndTask> getEndTasks();

  EndTask addEndTask();

  @NotNull
  List<CreateProcess> getCreateProcesses();

  CreateProcess addCreateProcess();

  @NotNull
  List<ResumeProcess> getResumeProcesses();

  ResumeProcess addResumeProcess();

  @NotNull
  List<In> getIns();

  In addIn();

  @NotNull
  List<RaiseEvent> getRaiseEvents();

  RaiseEvent addRaiseEvent();

  @NotNull
  List<Action> getActions();

  Action addAction();

  @NotNull
  List<Navigation> getNavigations();

  Navigation addNavigation();
}
