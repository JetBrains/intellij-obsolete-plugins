// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/drools

package com.intellij.seam.model.xml.drools;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/drools:rule-agentElemType interface.
 */
public interface RuleAgent extends SeamDroolsDomElement, BasicSeamComponent  {

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
	 * Returns the value of the configuration-file child.
	 * @return the value of the configuration-file child.
	 */
	@NotNull
	GenericAttributeValue<String> getConfigurationFile();


	/**
	 * Returns the value of the config-name child.
	 * @return the value of the config-name child.
	 */
	@NotNull
	GenericAttributeValue<String> getConfigName();


	/**
	 * Returns the value of the new-instance child.
	 * @return the value of the new-instance child.
	 */
	@NotNull
	GenericAttributeValue<String> getNewInstance();


	/**
	 * Returns the value of the files child.
	 * @return the value of the files child.
	 */
	@NotNull
	GenericAttributeValue<String> getFiles();


	/**
	 * Returns the value of the url child.
	 * @return the value of the url child.
	 */
	@NotNull
	GenericAttributeValue<String> getUrl();


	/**
	 * Returns the value of the local-cache-dir child.
	 * @return the value of the local-cache-dir child.
	 */
	@NotNull
	GenericAttributeValue<String> getLocalCacheDir();


	/**
	 * Returns the value of the poll child.
	 * @return the value of the poll child.
	 */
	@NotNull
	GenericAttributeValue<String> getPoll();


}
