package com.intellij.seam.model.xml.persistence;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.model.xml.components.MultiValuedProperty;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Namespace;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/persistence:managed-hibernate-sessionElemType interface.
 */
@Namespace(SeamNamespaceConstants.PERSISTENCE_NAMESPACE_KEY)
public interface ManagedHibernateSession extends BasicSeamComponent {

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
   * Returns the value of the session-factory child.
   *
   * @return the value of the session-factory child.
   */
  @NotNull
  GenericAttributeValue<String> getSessionFactory();

  /**
   * Returns the value of the session-factory-jndi-name child.
   *
   * @return the value of the session-factory-jndi-name child.
   */
  @NotNull
  GenericAttributeValue<String> getSessionFactoryJndiName();

  @NotNull
  MultiValuedProperty getFilters();
}
