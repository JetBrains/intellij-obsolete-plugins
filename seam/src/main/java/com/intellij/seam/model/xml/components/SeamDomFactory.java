package com.intellij.seam.model.xml.components;

import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.seam.model.CommonSeamFactoryComponent;
import com.intellij.seam.model.SeamComponentScope;
import com.intellij.seam.model.xml.SeamDomElement;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.NameValue;
import com.intellij.util.xml.Namespace;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/components:factoryElemType interface.
 */
@Namespace(SeamNamespaceConstants.COMPONENTS_NAMESPACE_KEY)
public interface SeamDomFactory extends CommonSeamFactoryComponent, SeamDomElement {

  @NotNull
  @Required
  @NameValue
  GenericAttributeValue<String> getName();

  @NotNull
  GenericAttributeValue<String> getMethod();

  @NotNull
  GenericAttributeValue<String> getValue();

  @NotNull
  GenericAttributeValue<SeamComponentScope> getScope();

  @NotNull
  GenericAttributeValue<Boolean> getAutoCreate();

  @NotNull
  GenericAttributeValue<Boolean> getStartup();
}
