// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/web

package com.intellij.seam.model.xml.web;


import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import com.intellij.seam.model.xml.components.BasicSeamComponent;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/web:ajax4jsf-filterElemType interface.
 */
public interface Ajax4jsfFilter extends SeamWebDomElement, BasicSeamComponent {

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
	 * Returns the value of the url-pattern child.
	 * @return the value of the url-pattern child.
	 */
	@NotNull
	GenericAttributeValue<String> getUrlPattern();


	/**
	 * Returns the value of the disabled child.
	 * @return the value of the disabled child.
	 */
	@NotNull
	GenericAttributeValue<String> getDisabled();


	/**
	 * Returns the value of the enable-cache child.
	 * @return the value of the enable-cache child.
	 */
	@NotNull
	GenericAttributeValue<EnableCache> getEnableCache();


	/**
	 * Returns the value of the force-parser child.
	 * @return the value of the force-parser child.
	 */
	@NotNull
	GenericAttributeValue<ForceParser> getForceParser();


	/**
	 * Returns the value of the log4j-init-file child.
	 * @return the value of the log4j-init-file child.
	 */
	@NotNull
	GenericAttributeValue<String> getLog4jInitFile();


}
