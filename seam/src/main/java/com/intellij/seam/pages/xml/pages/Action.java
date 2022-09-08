package com.intellij.seam.pages.xml.pages;


import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/pages:actionElemType interface.
 */
public interface Action extends SeamPagesDomElement {

	/**
	 * Returns the value of the if child.
	 * @return the value of the if child.
	 */
	@NotNull
	GenericAttributeValue<String> getIf();


	/**
	 * Returns the value of the execute child.
	 * @return the value of the execute child.
	 */
	@NotNull
	@Required
	GenericAttributeValue<String> getExecute();


}
