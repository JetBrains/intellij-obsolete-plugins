package com.intellij.seam.model.xml.components;

import com.intellij.seam.model.xml.SeamDomElement;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Namespace;
import org.jetbrains.annotations.NotNull;

@Namespace(SeamNamespaceConstants.COMPONENTS_NAMESPACE_KEY)
public interface SeamEjbComponent extends SeamDomElement {
  @NotNull
  GenericAttributeValue<String> getJndiName();
}
