package com.intellij.seam.model.xml.components;

import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.seam.model.references.SeamObserverEventTypeReferenceConverter;
import com.intellij.seam.model.xml.SeamDomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Namespace;
import com.intellij.util.xml.Referencing;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Namespace(SeamNamespaceConstants.COMPONENTS_NAMESPACE_KEY)
public interface SeamEvent extends SeamDomElement {

  @NotNull
  @Required
  @Referencing(SeamObserverEventTypeReferenceConverter.class)
  GenericAttributeValue<String> getType();

  @NotNull
  List<SeamAction> getActions();

  SeamAction addAction();
}
