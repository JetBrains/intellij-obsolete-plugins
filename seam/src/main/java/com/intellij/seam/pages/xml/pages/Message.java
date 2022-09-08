package com.intellij.seam.pages.xml.pages;


import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/pages:messageElemType interface.
 */
public interface Message extends SeamPagesDomElement {

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
	 * Returns the value of the for child.
	 * @return the value of the for child.
	 */
	@NotNull
	GenericAttributeValue<String> getFor();


	/**
	 * Returns the value of the severity child.
	 * @return the value of the severity child.
	 */
	@NotNull
	GenericAttributeValue<Severity> getSeverity();


}
