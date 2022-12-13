package com.intellij.seam.model.xml.persistence;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.model.xml.components.MapProperty;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Namespace;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/persistence:filterElemType interface.
 */
@Namespace(SeamNamespaceConstants.PERSISTENCE_NAMESPACE_KEY)
public interface Filter extends BasicSeamComponent {

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

  @NotNull
  GenericAttributeValue<Boolean> getEnabled();

  @NotNull
  @Tag("name")
  GenericDomValue<String> getFilterName();

  /**
   * Returns the value of the parameters child.
   *
   * @return the value of the parameters child.
   */
  @NotNull
  MapProperty getParameters();
}
