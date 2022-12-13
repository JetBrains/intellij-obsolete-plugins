package com.intellij.seam.model.xml.persistence;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.model.xml.components.MapProperty;
import com.intellij.seam.model.xml.components.MultiValuedProperty;
import com.intellij.seam.constants.SeamNamespaceConstants;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.Namespace;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jboss.com/products/seam/persistence:hibernate-session-factoryElemType interface.
 */
@Namespace(SeamNamespaceConstants.PERSISTENCE_NAMESPACE_KEY)
public interface HibernateSessionFactory extends BasicSeamComponent {

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

  /**
   * Returns the list of mapping-classes children.
   *
   * @return the list of mapping-classes children.
   */
  @NotNull
  List<MultiValuedProperty> getMappingClasseses();

  /**
   * Adds new child to the list of mapping-classes children.
   *
   * @return created child
   */
  MultiValuedProperty addMappingClasses();

  /**
   * Returns the list of mapping-files children.
   *
   * @return the list of mapping-files children.
   */
  @NotNull
  List<MultiValuedProperty> getMappingFileses();

  /**
   * Adds new child to the list of mapping-files children.
   *
   * @return created child
   */
  MultiValuedProperty addMappingFiles();

  /**
   * Returns the list of mapping-jars children.
   *
   * @return the list of mapping-jars children.
   */
  @NotNull
  List<MultiValuedProperty> getMappingJarses();

  /**
   * Adds new child to the list of mapping-jars children.
   *
   * @return created child
   */
  MultiValuedProperty addMappingJars();

  /**
   * Returns the list of mapping-packages children.
   *
   * @return the list of mapping-packages children.
   */
  @NotNull
  List<MultiValuedProperty> getMappingPackageses();

  /**
   * Adds new child to the list of mapping-packages children.
   *
   * @return created child
   */
  MultiValuedProperty addMappingPackages();

  /**
   * Returns the list of mapping-resources children.
   *
   * @return the list of mapping-resources children.
   */
  @NotNull
  List<MultiValuedProperty> getMappingResourceses();

  /**
   * Adds new child to the list of mapping-resources children.
   *
   * @return created child
   */
  MultiValuedProperty addMappingResources();

  /**
   * Returns the list of cfg-properties children.
   *
   * @return the list of cfg-properties children.
   */
  @NotNull
  List<MapProperty> getCfgPropertieses();

  /**
   * Adds new child to the list of cfg-properties children.
   *
   * @return created child
   */
  MapProperty addCfgProperties();
}
