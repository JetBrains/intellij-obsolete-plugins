package com.intellij.seam.model.xml.persistence;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.model.xml.components.MultiValuedProperty;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Namespace;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jboss.com/products/seam/persistence:managed-persistence-contextElemType interface.
 */
@Namespace(SeamNamespaceConstants.PERSISTENCE_NAMESPACE_KEY)
public interface ManagedPersistenceContext extends BasicSeamComponent {

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
   * Returns the value of the entity-manager-factory child.
   *
   * @return the value of the entity-manager-factory child.
   */
  @NotNull
  GenericAttributeValue<String> getEntityManagerFactory();

  /**
   * Returns the value of the persistence-unit-jndi-name child.
   *
   * @return the value of the persistence-unit-jndi-name child.
   */
  @NotNull
  GenericAttributeValue<String> getPersistenceUnitJndiName();

  /**
   * Returns the list of filters children.
   *
   * @return the list of filters children.
   */
  @NotNull
  List<MultiValuedProperty> getFilterses();

  /**
   * Adds new child to the list of filters children.
   *
   * @return created child
   */
  MultiValuedProperty addFilters();

  /**
   * Returns the list of persistence-unit-jndi-name children.
   *
   * @return the list of persistence-unit-jndi-name children.
   */
  @NotNull
  List<GenericDomValue<String>> getPersistenceUnitJndiNames();

  /**
   * Adds new child to the list of persistence-unit-jndi-name children.
   *
   * @return created child
   */
  GenericDomValue<String> addPersistenceUnitJndiName();
}
