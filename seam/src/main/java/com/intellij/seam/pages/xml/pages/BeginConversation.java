package com.intellij.seam.pages.xml.pages;


import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/pages:begin-conversationElemType interface.
 */
public interface BeginConversation extends SeamPagesDomElement {

	/**
	 * Returns the value of the join child.
	 * @return the value of the join child.
	 */
	@NotNull
	GenericAttributeValue<Join> getJoin();


	/**
	 * Returns the value of the nested child.
	 * @return the value of the nested child.
	 */
	@NotNull
	GenericAttributeValue<Nested> getNested();


	/**
	 * Returns the value of the pageflow child.
	 * @return the value of the pageflow child.
	 */
	@NotNull
	GenericAttributeValue<String> getPageflow();


	/**
	 * Returns the value of the flush-mode child.
	 * @return the value of the flush-mode child.
	 */
	@NotNull
	GenericAttributeValue<FlushMode> getFlushMode();


	/**
	 * Returns the value of the if child.
	 * @return the value of the if child.
	 */
	@NotNull
	GenericAttributeValue<String> getIf();


}
