// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/pdf

package com.intellij.seam.model.xml.pdf;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/pdf:key-store-configElemType interface.
 */
public interface KeyStoreConfig extends SeamPdfDomElement, BasicSeamComponent {

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
	 * Returns the value of the key-store child.
	 * @return the value of the key-store child.
	 */
	@NotNull
	GenericAttributeValue<String> getKeyStore();


	/**
	 * Returns the value of the key-store-password child.
	 * @return the value of the key-store-password child.
	 */
	@NotNull
	GenericAttributeValue<String> getKeyStorePassword();


	/**
	 * Returns the value of the key-password child.
	 * @return the value of the key-password child.
	 */
	@NotNull
	GenericAttributeValue<String> getKeyPassword();


	/**
	 * Returns the value of the key-alias child.
	 * @return the value of the key-alias child.
	 */
	@NotNull
	GenericAttributeValue<String> getKeyAlias();


}
