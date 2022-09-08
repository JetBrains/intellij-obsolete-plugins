// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/framework

package com.intellij.seam.model.xml.framework;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.model.xml.components.MultiValuedProperty;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jboss.com/products/seam/framework:hibernate-entity-queryElemType interface.
 */
public interface HibernateEntityQuery extends SeamFrameworkDomElement, BasicSeamComponent {

	/**
	 * Returns the value of the simple content.
	 * @return the value of the simple content.
	 */
	@NotNull
	@Required
	String getValue();
	/**
	 * Sets the value of the simple content.
	 * @param value the new value to set
	 */
	void setValue(@NotNull String value);


	/**
	 * Returns the value of the ejbql child.
	 * @return the value of the ejbql child.
	 */
	@NotNull
	GenericAttributeValue<String> getEjbql();


	/**
	 * Returns the value of the max-results child.
	 * @return the value of the max-results child.
	 */
	@NotNull
	GenericAttributeValue<Integer> getMaxResults();


	/**
	 * Returns the value of the order child.
	 * @return the value of the order child.
	 */
	@NotNull
	GenericAttributeValue<String> getOrder();


	/**
	 * Returns the value of the cacheable child.
	 * @return the value of the cacheable child.
	 */
	@NotNull
	GenericAttributeValue<Cacheable> getCacheable();


	/**
	 * Returns the value of the cache-region child.
	 * @return the value of the cache-region child.
	 */
	@NotNull
	GenericAttributeValue<String> getCacheRegion();


	/**
	 * Returns the value of the fetch-size child.
	 * @return the value of the fetch-size child.
	 */
	@NotNull
	GenericAttributeValue<Integer> getFetchSize();


	/**
	 * Returns the value of the session child.
	 * @return the value of the session child.
	 */
	@NotNull
	GenericAttributeValue<String> getSession();


	/**
	 * Returns the list of ejbql children.
	 * @return the list of ejbql children.
	 */
	@NotNull
	List<GenericDomValue<String>> getEjbqls();
	/**
	 * Adds new child to the list of ejbql children.
	 * @return created child
	 */
	GenericDomValue<String> addEjbql();


	/**
	 * Returns the list of order children.
	 * @return the list of order children.
	 */
	@NotNull
	List<GenericDomValue<String>> getOrders();
	/**
	 * Adds new child to the list of order children.
	 * @return created child
	 */
	GenericDomValue<String> addOrder();


	/**
	 * Returns the list of restrictions children.
	 * @return the list of restrictions children.
	 */
	@NotNull
	List<MultiValuedProperty> getRestrictionses();
	/**
	 * Adds new child to the list of restrictions children.
	 * @return created child
	 */
	MultiValuedProperty addRestrictions();
}
