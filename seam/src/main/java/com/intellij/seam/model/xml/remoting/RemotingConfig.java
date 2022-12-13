// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/remoting

package com.intellij.seam.model.xml.remoting;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/remoting:remoting-configElemType interface.
 */
public interface RemotingConfig extends SeamRemotingDomElement, BasicSeamComponent {

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
	 * Returns the value of the poll-interval child.
	 * @return the value of the poll-interval child.
	 */
	@NotNull
	GenericAttributeValue<String> getPollInterval();


	/**
	 * Returns the value of the poll-timeout child.
	 * @return the value of the poll-timeout child.
	 */
	@NotNull
	GenericAttributeValue<String> getPollTimeout();


	/**
	 * Returns the value of the debug child.
	 * @return the value of the debug child.
	 */
	@NotNull
	GenericAttributeValue<Debug> getDebug();


}
