package com.intellij.seam.model.xml.core;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Namespace;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/core:pojo-cacheElemType interface.
 */
@Namespace(SeamNamespaceConstants.CORE_NAMESPACE_KEY)
public interface PojoCache extends BasicSeamComponent {

  /**
   * Returns the value of the simple content.
   *
   * @return the value of the simple content.
   */
  @NotNull
  @Required
  String getValue();

  /**
   * Sets the value of the simple content.
   *
   * @param value the new value to set
   */
  void setValue(@NotNull String value);

  /**
   * Returns the value of the cfg-resource-name child.
   *
   * @return the value of the cfg-resource-name child.
   */
  @NotNull
  GenericAttributeValue<String> getCfgResourceName();
}
