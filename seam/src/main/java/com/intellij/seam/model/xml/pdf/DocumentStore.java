// Generated on Tue Dec 11 18:29:45 MSK 2007
// DTD/Schema  :    http://jboss.com/products/seam/pdf

package com.intellij.seam.model.xml.pdf;

import com.intellij.seam.model.xml.components.BasicSeamComponent;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/pdf:document-storeElemType interface.
 */
public interface DocumentStore extends SeamPdfDomElement, BasicSeamComponent {

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
	 * Returns the value of the error-page child.
	 * @return the value of the error-page child.
	 */
	@NotNull
	GenericAttributeValue<String> getErrorPage();


	/**
	 * Returns the value of the use-extensions child.
	 * @return the value of the use-extensions child.
	 */
	@NotNull
	GenericAttributeValue<String> getUseExtensions();


}
