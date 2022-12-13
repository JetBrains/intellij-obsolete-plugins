// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/mail

package com.intellij.seam.model.xml.mail;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/mail:mail-sessionElemType interface.
 */
public interface MailSession extends SeamMailDomElement, BasicSeamComponent {
	/**
	 * Returns the value of the username child.
	 * @return the value of the username child.
	 */
	@NotNull
	GenericAttributeValue<String> getUsername();


	/**
	 * Returns the value of the password child.
	 * @return the value of the password child.
	 */
	@NotNull
	GenericAttributeValue<String> getPassword();


	/**
	 * Returns the value of the host child.
	 * @return the value of the host child.
	 */
	@NotNull
	GenericAttributeValue<String> getHost();


	/**
	 * Returns the value of the port child.
	 * @return the value of the port child.
	 */
	@NotNull
	GenericAttributeValue<String> getPort();


	/**
	 * Returns the value of the debug child.
	 * @return the value of the debug child.
	 */
	@NotNull
	GenericAttributeValue<String> getDebug();


	/**
	 * Returns the value of the ssl child.
	 * @return the value of the ssl child.
	 */
	@NotNull
	GenericAttributeValue<Boolean> getSsl();


	/**
	 * Returns the value of the tls child.
	 * @return the value of the tls child.
	 */
	@NotNull
	GenericAttributeValue<Boolean> getTls();


	/**
	 * Returns the value of the session-jndi-name child.
	 * @return the value of the session-jndi-name child.
	 */
	@NotNull
	GenericAttributeValue<String> getSessionJndiName();


}
