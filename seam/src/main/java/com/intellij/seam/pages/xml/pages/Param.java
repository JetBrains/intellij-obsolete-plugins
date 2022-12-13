package com.intellij.seam.pages.xml.pages;


import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/pages:paramElemType interface.
 */
public interface Param extends SeamPagesDomElement {

	/**
	 * Returns the value of the name child.
	 * @return the value of the name child.
	 */
	@NotNull
	GenericAttributeValue<String> getName();


	/**
	 * Returns the value of the value child.
	 * @return the value of the value child.
	 */
	@NotNull
	GenericAttributeValue<String> getValue();


	/**
	 * Returns the value of the converter child.
	 * @return the value of the converter child.
	 */
	@NotNull
	GenericAttributeValue<String> getConverter();


	/**
	 * Returns the value of the converterId child.
	 * @return the value of the converterId child.
	 */
	@NotNull
	GenericAttributeValue<String> getConverterId();


	/**
	 * Returns the value of the validator child.
	 * @return the value of the validator child.
	 */
	@NotNull
	GenericAttributeValue<String> getValidator();


	/**
	 * Returns the value of the validatorId child.
	 * @return the value of the validatorId child.
	 */
	@NotNull
	GenericAttributeValue<String> getValidatorId();


	/**
	 * Returns the value of the required child.
	 * @return the value of the required child.
	 */
	@NotNull
	GenericAttributeValue<Boolean> getRequired();


}
