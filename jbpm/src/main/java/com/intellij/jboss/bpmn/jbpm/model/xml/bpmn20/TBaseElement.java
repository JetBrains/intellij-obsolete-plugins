package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.ide.presentation.Presentation;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.NameValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Presentation(provider = BPMNPresentationProvider.class)
public interface TBaseElement extends Bpmn20DomElement {

  @NotNull
  @NameValue
  GenericAttributeValue<String> getId();

  @NotNull
  List<TDocumentation> getDocumentations();

  @NotNull
  TExtensionElements getExtensionElements();
}
