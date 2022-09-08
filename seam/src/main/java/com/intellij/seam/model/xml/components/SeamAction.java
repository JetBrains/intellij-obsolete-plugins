package com.intellij.seam.model.xml.components;

import com.intellij.seam.model.xml.SeamDomElement;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Namespace;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/components:actionElemType interface.
 */
@Namespace(SeamNamespaceConstants.COMPONENTS_NAMESPACE_KEY)
public interface SeamAction extends SeamDomElement {

  @NotNull
  @Required
  GenericAttributeValue<String> getExecute();
}
