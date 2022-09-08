package com.intellij.seam.pages.xml.pages;


import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/pages:http-errorElemType interface.
 */
public interface HttpError extends SeamPagesDomElement {

	/**
	 * Returns the value of the error-code child.
	 * @return the value of the error-code child.
	 */
	@NotNull
	GenericAttributeValue<String> getErrorCode();


	/**
	 * Returns the value of the message child.
	 * @return the value of the message child.
	 */
	@NotNull
	Message getMessage();


}
