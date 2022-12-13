package com.intellij.seam.pages.xml.pages;


import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/pages:end-taskElemType interface.
 */
public interface EndTask extends SeamPagesDomElement {

	/**
	 * Returns the value of the transition child.
	 * @return the value of the transition child.
	 */
	@NotNull
	GenericAttributeValue<String> getTransition();


	/**
	 * Returns the value of the before-redirect child.
	 * @return the value of the before-redirect child.
	 */
	@NotNull
	GenericAttributeValue<BeforeRedirect> getBeforeRedirect();


}
