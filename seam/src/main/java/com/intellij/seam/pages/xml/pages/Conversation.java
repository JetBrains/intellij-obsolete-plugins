package com.intellij.seam.pages.xml.pages;


import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/pages:conversationElemType interface.
 */
public interface Conversation extends SeamPagesDomElement {

	/**
	 * Returns the value of the name child.
	 * @return the value of the name child.
	 */
	@NotNull
	@Required
	GenericAttributeValue<String> getName();


	/**
	 * Returns the value of the parameter-name child.
	 * @return the value of the parameter-name child.
	 */
	@NotNull
	@Required
	GenericAttributeValue<String> getParameterName();


	/**
	 * Returns the value of the parameter-value child.
	 * @return the value of the parameter-value child.
	 */
	@NotNull
	GenericAttributeValue<String> getParameterValue();


}
