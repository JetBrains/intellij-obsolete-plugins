// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/jms

package com.intellij.seam.model.xml.jms;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/jms:topic-connectionElemType interface.
 */
public interface TopicConnection extends SeamJmsDomElement, BasicSeamComponent {

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
	 * Returns the value of the topic-connection-factory-jndi-name child.
	 * @return the value of the topic-connection-factory-jndi-name child.
	 */
	@NotNull
	GenericAttributeValue<String> getTopicConnectionFactoryJndiName();


}
