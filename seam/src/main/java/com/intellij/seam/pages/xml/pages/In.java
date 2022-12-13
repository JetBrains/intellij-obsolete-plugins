package com.intellij.seam.pages.xml.pages;


import com.intellij.seam.model.SeamComponentScope;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/pages:inElemType interface.
 */
public interface In extends SeamPagesDomElement {

	@NotNull
	@Required
	GenericAttributeValue<String> getName();


	@NotNull
	GenericAttributeValue<SeamComponentScope> getScope();


	@NotNull
	@Required
	GenericAttributeValue<String> getValue();
}
