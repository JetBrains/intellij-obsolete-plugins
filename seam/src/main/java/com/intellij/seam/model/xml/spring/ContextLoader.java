// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/spring

package com.intellij.seam.model.xml.spring;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.seam.model.xml.components.MultiValuedProperty;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jboss.com/products/seam/spring:context-loaderElemType interface.
 */
public interface ContextLoader extends SeamSpringDomElement, BasicSeamComponent {

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
	 * Returns the value of the config-locations child.
	 * <pre>
	 * <h3>Attribute null:config-locations documentation</h3>
	 *                                  A single application context config location.
	 *                         
	 * </pre>
	 * @return the value of the config-locations child.
	 */
	@NotNull
	GenericAttributeValue<String> getConfigLocations();


	/**
	 * Returns the list of config-locations children.
	 * <pre>
	 * <h3>Element http://jboss.com/products/seam/spring:config-locations documentation</h3>
	 * 	                 Allows you to specify many config-locations in nested value elements.
	 * 	        
	 * </pre>
	 * @return the list of config-locations children.
	 */
	@NotNull
	List<MultiValuedProperty> getConfigLocationses();
	/**
	 * Adds new child to the list of config-locations children.
	 * @return created child
	 */
	MultiValuedProperty addConfigLocations();


}
