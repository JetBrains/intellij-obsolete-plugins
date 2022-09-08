package com.intellij.seam.model.xml.persistence;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.model.xml.components.MapProperty;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Namespace;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/persistence:entity-manager-factoryElemType interface.
 */
@Namespace(SeamNamespaceConstants.PERSISTENCE_NAMESPACE_KEY)
public interface EntityManagerFactory extends BasicSeamComponent {

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
   * Returns the value of the persistence-unit-name child.
   *
   * @return the value of the persistence-unit-name child.
   */
  @NotNull
  GenericAttributeValue<String> getPersistenceUnitName();

  /**
   * Returns the value of the persistence-unit-properties child.
   *
   * @return the value of the persistence-unit-properties child.
   */
  @NotNull
  @Required
  MapProperty getPersistenceUnitProperties();
}
